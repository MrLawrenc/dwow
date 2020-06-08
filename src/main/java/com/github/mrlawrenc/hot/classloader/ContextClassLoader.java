package com.github.mrlawrenc.hot.classloader;

import com.github.mrlawrenc.hot.boot.Boot;
import com.github.mrlawrenc.hot.boot.FileListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author : MrLawrenc
 * date  2020/6/7 10:40
 * <p>
 * 两种方式打破双亲委派{@link ClassLoader#loadClass(String)}
 * <pre>
 *     1. 手动设置parent属性为null
 *     2. 改变loadclass内容，使 findLoadedClass(name);方法能获取到已经加载的类，详见ClassLoader line=570
 * </pre>
 * <p>
 * 全局应用类加载器,只有触发热部署才会重新实例化该加载器
 * <p>
 * <p>
 * 所有的.class文件对应一个{@link HotSwapClassClassLoader}实例
 * 每个jar包文件对应一个{@link HotSwapJarClassLoader}实例
 */
@Slf4j
public class ContextClassLoader extends ClassLoader implements BootClassLoader {


    @Getter
    private Lock monitor = new ReentrantLock();
    @Getter
    private Boot boot;

    /**
     * 应用的class path路径
     */
    private String classPath;

    /**
     * 热加载的类加载器分为两类，一类为加载.class文件的，一类是加载jar文件的
     */
    private HotSwapClassClassLoader classClassLoader;
    // private LinkedList<HotSwapClassClassLoader> classClassLoaderQueue;
    /**
     * 使用{@link HotSwapClassClassLoader}加载过的类名集合
     */
    private List<String> classClassLoaderLoadClzNames;


    /**
     * 使用{@link HotSwapJarClassLoader}加载的类名作为key，对应的加载器对象为value
     */
    private Map<String, HotSwapJarClassLoader> jarClassLoadersMap;


    public ContextClassLoader(String classPath) {
        this.classPath = new File(classPath).getAbsolutePath();
        this.classClassLoaderLoadClzNames = new ArrayList<>();
        //  this.classClassLoaderQueue = new ArrayDeque<>();

        Class<?> bootClz = loadBoot();

        if (bootClz == null) {
            throw new NullPointerException("boot is null");
        }
        try {
            boot = (Boot) bootClz.getConstructor().newInstance();
        } catch (Exception e) {
            log.error("boot instance fail", e);
        }
    }

    /**
     * 初始化所有字节码
     */
    public void initHotClass() {
        Collection<File> listFiles = FileUtils.listFiles(new File(classPath), new String[]{CLASS_FLAG, JAR_FLAG}, true);

        List<File> classFileList = new ArrayList<>();
        List<File> jarFileList = new ArrayList<>();
        listFiles.forEach(file -> {
            if (file.getName().endsWith(CLASS_FLAG)) {
                classFileList.add(file);
            } else {
                jarFileList.add(file);
            }
        });

        loadClassFiLe(classFileList);
        loadJarFiLe(jarFileList);

        loadFileListenerRun(this, 2000L);
    }

    /**
     * 加载file listener 并且 run
     * <p>
     * 在初始化全局类加载器的时候被反射调用
     *
     * @param interval 轮训间隔 单位ms
     */
    private void loadFileListenerRun(ContextClassLoader loader, Long interval) {
        System.out.println("listening : " + classPath);
        FileAlterationObserver observer = new FileAlterationObserver(classPath);

        observer.addListener(new FileListener(loader));

        FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);

        try {
            monitor.start();
        } catch (Exception e) {
            throw new RuntimeException("start file listener fail! listen dir : " + classPath);
        }
    }

    private void loadJarFiLe(List<File> jarFileList) {
        jarFileList.forEach(file -> {
            HotSwapJarClassLoader jarClassLoader = new HotSwapJarClassLoader();

            File path = file.getAbsoluteFile();
            JarFile jarFile ;
            try {
                jarFile = new JarFile(file);
            } catch (IOException e) {
                log.error("load jar file({}) fail ", path);
                return;
            }
            Iterator<JarEntry> iterator = jarFile.entries().asIterator();
            while (iterator.hasNext()) {
                JarEntry jarEntry = iterator.next();
                try {
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    byte[] bytes = inputStream.readAllBytes();
                    jarClassLoader.defineClass0(jarEntry.getRealName(), bytes, 0, bytes.length);
                    jarClassLoadersMap.put(jarEntry.getRealName(), jarClassLoader);
                } catch (IOException e) {
                    log.error("load class(in jar) fail! jar:{} class:{}", path, jarEntry.getRealName());
                }
            }
        });
    }

    /**
     * 使用一个{@link HotSwapClassClassLoader}加载所有class文件
     *
     * @param classFileList 字节码文件
     */
    private void loadClassFiLe(List<File> classFileList) {
        this.classClassLoader = new HotSwapClassClassLoader();
        classFileList.forEach(file -> {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = inputStream.readAllBytes();
                classClassLoader.defineClass0(filePath2ClzName(file.getAbsolutePath()), bytes, 0, bytes.length);

                classClassLoaderLoadClzNames.add(file.getName());
            } catch (Exception e) {
                log.error("load class({}) fail", file.getAbsoluteFile());
            }
        });
        //classClassLoaderQueue.offer(classClassLoader);
    }


    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        monitor.lock();
        try {
            //判断当前类是否已经被加载,若以加载，从后开始查找
            if (classClassLoaderLoadClzNames.contains(name)) {
                Class<?> loadClass = classClassLoader.loadClass(name, resolve);
                if (loadClass == null) {
                    //说明已经热更新过,重新加载

                    this.classClassLoader.updateClass(name);

                }

            }
            HotSwapJarClassLoader jarClassLoader = jarClassLoadersMap.get(name);
            if (jarClassLoader != null) {
                return jarClassLoader.loadClass(name, resolve);
            }

            //当前类交由AppClassLoader
            return getSystemClassLoader().loadClass(name);
        } finally {
            monitor.unlock();
        }
    }


    public void reloadClass(String name, byte[] b, int off, int len, boolean jarType) {
        monitor.lock();
        try {
            if (jarType) {
                //todo
            } else {
                HotSwapClassClassLoader newLoader = new HotSwapClassClassLoader();
                newLoader.setByteMap(this.classClassLoader.getByteMap());

                this.classClassLoader = newLoader;

                classClassLoader.defineClass0(name, b, off, len);
            }

        } finally {
            monitor.unlock();
        }
    }


    /**
     * 加载Boot
     */
    private Class<?> loadBoot() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> aList = bean.getInputArguments();
        //当前代理jar包所在位置
        String keyword = "java-agent-dwow";
        for (String s : aList) {
            if (!s.contains(keyword)) {
                continue;
            }
            //从当前jar包加载Boot
            String filePath = s.split("=")[0].split("javaagent:")[1];
            System.out.println("current agent path : " + filePath);

            File file = new File(filePath);
            try {
                JarFile jarFile = new JarFile(file);
                Iterator<JarEntry> jarEntryIterator = jarFile.entries().asIterator();
                while (jarEntryIterator.hasNext()) {
                    JarEntry jarEntry = jarEntryIterator.next();
                    if (jarEntry.getRealName().replaceAll("/", ".").contains(Boot.class.getName())) {
                        System.out.println("find boot:" + jarEntry);

                        byte[] bytes = jarFile.getInputStream(jarEntry).readAllBytes();
                        Class<?> boot = defineClass(Boot.class.getName(), bytes, 0, bytes.length);
                        System.out.println("load proxy boot:" + boot);
                        return boot;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("load boot fail", e);
            }
        }
        return null;
    }

    /**
     * 根据文件全路径获取该文件对应得class全限定名
     */
    public String filePath2ClzName(String filePath) {
        String str = filePath.replace(classPath, "").replaceAll(SEPARATOR, ".");
        return str.replace(".class", "");
    }
}