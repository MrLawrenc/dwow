package com.github.mrlawrenc.hot.classloader;

import com.github.mrlawrenc.hot.boot.Boot;
import com.github.mrlawrenc.hot.boot.FileListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
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


    private List<String> loadClzList = new ArrayList<>();


    /**
     * 应用的class path路径
     */
    private String classPath;
    /**
     * 热加载的类加载器分为两类，一类为加载.class文件的，一类是加载jar文件的
     */
    private HotSwapClassClassLoader classClassLoader;
    /**
     * 使用{@link HotSwapClassClassLoader}加载过的类名集合
     */
    private List<String> classClassLoaderLoadClzNames;


    /**
     * 使用{@link HotSwapJarClassLoader}加载的类名作为key，对应的加载器对象为value
     */
    private Map<String, HotSwapJarClassLoader> jarClassLoadersMap;


    public ContextClassLoader(String classPath) {
        loadBoot();

        loadFileListenerRun(classPath, 2000);

        Collection<File> listFiles = FileUtils.listFiles(new File(classPath), new String[]{"class", "jar"}, true);

        List<File> classFileList = new ArrayList<>();
        List<File> jarFileList = new ArrayList<>();
        listFiles.forEach(file -> {
            if (file.getName().endsWith(".class")) {
                classFileList.add(file);
            } else {
                jarFileList.add(file);
            }
        });

        loadClassFiLe(classFileList);
        loadJarFiLe(jarFileList);
    }


    /**
     * 加载file listener 并且 run
     *
     * @param interval 轮训间隔
     * @param path     监听文件夹路径
     */
    private void loadFileListenerRun(String path, long interval) {
        System.out.println("listening : " + path);
        FileAlterationObserver observer = new FileAlterationObserver(path);

        observer.addListener(new FileListener());

        //5s
        FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);

        try {
            monitor.start();
        } catch (Exception e) {
            throw new RuntimeException("start file listener fail! listen dir : " + path);
        }
    }

    private void loadJarFiLe(List<File> jarFileList) {

    }

    /**
     * 使用一个{@link HotSwapClassClassLoader}加载所有class文件
     * @param classFileList 字节码文件
     */
    private void loadClassFiLe(List<File> classFileList) {
      //  HotSwapClassClassLoader classClassLoader = new HotSwapClassClassLoader();

    }


    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //判断当前类是否已经被加载
        if (classClassLoaderLoadClzNames.contains(name)) {
            return classClassLoader.loadClass(name, resolve);
        }
        HotSwapJarClassLoader jarClassLoader = jarClassLoadersMap.get(name);
        if (jarClassLoader != null) {
            return jarClassLoader.loadClass(name, resolve);
        }

        //当前类交由AppClassLoader
        return getSystemClassLoader().loadClass(name);
    }


    /**
     * 加载Boot
     */
    private void loadBoot() {
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
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("load boot fail", e);
            }
        }
    }
}