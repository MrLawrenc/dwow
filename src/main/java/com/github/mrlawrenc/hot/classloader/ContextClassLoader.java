package com.github.mrlawrenc.hot.classloader;

import com.github.mrlawrenc.hot.boot.Boot;
import com.github.mrlawrenc.hot.boot.FileListener;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
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
import java.lang.reflect.Field;
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
    private Object boot;

    /**
     * 应用的class path路径
     */
    private String classPath;

    /**
     * 热加载的类加载器分为两类，一类为加载.class文件的，一类是加载jar文件的
     */
    @Getter
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
        this.jarClassLoadersMap = new HashMap<>();
        //  this.classClassLoaderQueue = new ArrayDeque<>();

        Class<?> bootClz = loadBoot();

        if (bootClz == null) {
            throw new NullPointerException("boot is null");
        }
        try {
            //不能强转，且boot不应该定义为Boot类型，应为Object，当前boot是由ContextClassLoader加载的
            //boot = (Boot) bootClz.getConstructor().newInstance();
            boot = bootClz.getConstructor().newInstance();
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

        //destroy monitor.stop();monitor.getObservers().iterator().next().destroy();
        loadFileListenerRun(this, 1000L);
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
            JarFile jarFile;
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
                Class<?> loadedClz = classClassLoader.defineClass0(filePath2ClzName(file.getAbsolutePath()), bytes, 0, bytes.length);

                classClassLoaderLoadClzNames.add(loadedClz.getName());
            } catch (Exception e) {
                log.error("load class({}) fail", file.getAbsoluteFile());
            }
        });
        //classClassLoaderQueue.offer(classClassLoader);
    }


    public Class<?> defineClass0(String name, byte[] b, int off, int len) {
        return this.defineClass(name, b, off, len);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        monitor.lock();
        try {
            System.out.println("context load " + name);
            //判断当前类是否已经被加载,若以加载，从后开始查找
            if (classClassLoaderLoadClzNames.contains(name)) {
                return classClassLoader.loadClass(name, resolve);
            }

            HotSwapJarClassLoader jarClassLoader = jarClassLoadersMap.get(name);
            if (jarClassLoader != null) {
                return jarClassLoader.loadClass(name, resolve);
            }

            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
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
                HotSwapClassClassLoader old = this.classClassLoader;
                System.out.println(" old class:" + old.loadClass(name).hashCode());
                HotSwapClassClassLoader newLoader = new HotSwapClassClassLoader();
                old.changeClassLoader(newLoader);

                this.classClassLoader = newLoader;
                Class<?> defineClass0 = newLoader.defineClass0(name, b, off, len);
                System.out.println("new class:" + defineClass0.hashCode());
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
                        byte[] bytes = jarFile.getInputStream(jarEntry).readAllBytes();
                        Class<?> boot = defineClass(Boot.class.getName(), bytes, 0, bytes.length);
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
        return str.substring(1, str.length() - 6);
    }

    public static void main(String[] args) throws Exception {
        String absolutePath = new File("D:\\A").getAbsolutePath();


        ClassPool classPool = ClassPool.getDefault();
        CtClass boot = classPool.get("com.github.mrlawrenc.hot.boot.Boot");

        CtMethod tt = boot.getDeclaredMethod("tt");
        CtMethod proxyMethod = CtNewMethod.copy(tt, tt.getName() + "agent", boot, null);
        boot.addMethod(proxyMethod);

        //改变原方法，且调用新的方法
        CtClass t = classPool.get(Throwable.class.getName());
        tt.setBody("{\n"
                +"long start = System.currentTimeMillis();"
                +"Object c=($w)$0.ttagent($$);"
                +" System.out.println(System.currentTimeMillis()-start);"
                +"return ($r)c;"
                + "\n}");

        tt.addCatch("{\nSystem.out.println(\"结束:\"+System.nanoTime());\nthrow $e;}",t);



        //作为finally
        tt.insertAfter(  "{Long end=System.nanoTime();\nSystem.out.println(\"finally end:\");}",true);


        //Class<?> toClass = boot.toClass();
        //toClass.getDeclaredMethod("tt").invoke(toClass.getConstructor());


        boot.writeFile(absolutePath);
    }
private static boolean first=true;
    /**
     * 拷贝main方法所在的类，并保存
     */
    public void saveCopyMain() {
        if (first){
            try {
                //保存源main方法所在的类的副本
                ClassPool pool = ClassPool.getDefault();
                CtClass main = pool.get("com.swust.Main");
                //如果CtClass通过writeFile(),toClass(),toBytecode()转换了类文件，javassist冻结了CtClass对象。以后是不允许修改这个 CtClass对象
                //cc.writeFile();//冻结
                //cc.defrost();//解冻
                //cc.setSuperclass(...);   // OK since the class is not frozen.可以重新操作CtClass，因为被解冻了
                main.defrost();
                main.setName("Hello");

                main.writeFile("D:\\B");
                first=false;
                //System.out.println("newMain:"+newMain+"  loader:"+newMain.getClassLoader());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        //调用main副本方法
        File proxyFile = new File("D:\\B\\Hello.class");
        try (FileInputStream inputStream = new FileInputStream(proxyFile)) {
            byte[] bytes = inputStream.readAllBytes();
            Class<?> proxyMain = this.defineClass("Hello", bytes, 0, bytes.length);

            Field copyMainObj = boot.getClass().getDeclaredField("copyMainObj");
            copyMainObj.setAccessible(true);
            copyMainObj.set(boot, proxyMain.getConstructor().newInstance());
        } catch (Exception e) {
            log.error("save copy main({}) fail", proxyFile.getAbsolutePath());
        }
    }
}