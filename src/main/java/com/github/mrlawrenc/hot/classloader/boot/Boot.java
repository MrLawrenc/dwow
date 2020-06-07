package com.github.mrlawrenc.hot.classloader.boot;


import com.github.mrlawrenc.AgentMain;
import com.github.mrlawrenc.hot.classloader.HotSwapClassLoader;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author : MrLawrenc
 * date  2020/6/7 11:34
 */
public class Boot {

    public void start() {
        System.out.println("###############################Hot boot init###############################");
        System.out.println("  _  _             _       ___                     _ __    ___      _                              _                         _                               ___                            _  _  \n" +
                " | || |    ___    | |_    / __|  __ __ __ __ _    | '_ \\  / __|    | |    __ _     ___     ___    | |      ___    __ _    __| |    ___      _ _     o O O   | _ \\    _ _    ___    __ __   | || | \n" +
                " | __ |   / _ \\   |  _|   \\__ \\  \\ V  V // _` |   | .__/ | (__     | |   / _` |   (_-<    (_-<    | |__   / _ \\  / _` |  / _` |   / -_)    | '_|   o        |  _/   | '_|  / _ \\   \\ \\ /    \\_, | \n" +
                " |_||_|   \\___/   _\\__|   |___/   \\_/\\_/ \\__,_|   |_|__   \\___|   _|_|_  \\__,_|   /__/_   /__/_   |____|  \\___/  \\__,_|  \\__,_|   \\___|   _|_|_   TS__[O]  _|_|_   _|_|_   \\___/   /_\\_\\   _|__/  \n" +
                "_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"| {======|_| \"\"\" |_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_| \"\"\"\"| \n" +
                "\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'./o--000'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-' \n");
        System.out.println("current thread loader:" + Thread.currentThread().getContextClassLoader());
        System.out.println("current obj loader:" + this.getClass().getClassLoader());
        String copyMain = "D:\\Main$Proxy.class";
        //存储副本
        try (FileOutputStream stream = new FileOutputStream(new File(copyMain))) {
            ClassPool pool = ClassPool.getDefault();
            CtClass main = pool.get("com.swust.Main");
            stream.write(main.toBytecode());
            System.out.println("copy main write to  : " + copyMain);


            //调用main方法
          /*  System.out.println("invoke proxy main ");
            Method proxy = Class.forName("com.swust.Main").getDeclaredMethod("main$proxy", String[].class);
            proxy.invoke(null, (Object) new String[]{"111", "222", "333"});*/

        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("###############################Hot boot  end###############################");
    }

    /**
     * @param loader 类加载器
     * @param first  是否是第一次初始化
     * @param path   监听字节码的class path
     * @throws Exception 代理失败
     */
    public static void start0(HotSwapClassLoader loader, boolean first, String path) throws Exception {

        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> aList = bean.getInputArguments();
        //当前代理jar包所在位置
        String keyword = "java-agent-dwow";
        for (int i = 0; i < aList.size(); i++) {
            if (aList.get(i).contains(keyword)) {
                //从当前jar包加载Boot
                String filePath = aList.get(i).split("=")[0].split("javaagent:")[1];
                System.out.println("current agent path : " + filePath);

                File file = new File(filePath);
                JarFile jarFile = new JarFile(file);
                Iterator<JarEntry> jarEntryIterator = jarFile.entries().asIterator();
                while (jarEntryIterator.hasNext()) {
                    JarEntry jarEntry = jarEntryIterator.next();
                    if (jarEntry.getRealName().replaceAll("/", ".").contains(Boot.class.getName())) {
                        System.out.println("find boot:" + jarEntry);

                        byte[] bytes = jarFile.getInputStream(jarEntry).readAllBytes();
                        Class<?> boot = loader.defineClass0(Boot.class.getName(), bytes, 0, bytes.length);
                        System.out.println("load proxy boot:" + boot);

                        //热加载
                        Object bootObj = boot.getConstructor().newInstance();
                        boot.getMethod("start").invoke(bootObj);
                        Thread.currentThread().setContextClassLoader(loader);
                        if (first) {
                            System.out.println("first invoke ,will init file listener");
                            boot.getMethod("startListenFile",String.class).invoke(bootObj, path);
                        }
                    }
                }

            }
        }



    }

    /**
     * 热部署代理入口
     *
     * @throws Exception 代理异常
     */
    public static void run() throws Exception {
        String path = AgentMain.agentArgs;
        System.out.println("run ..... " + path);
        HotSwapClassLoader hotSwapClassLoader = new HotSwapClassLoader(path);

        start0(hotSwapClassLoader, true, path);
    }

    public void startListenFile(String path) throws Exception {
        System.out.println("listening : " + path);
        FileAlterationObserver observer = new FileAlterationObserver(path);

        observer.addListener(new FileListener());

        //5s
        FileAlterationMonitor monitor = new FileAlterationMonitor(5000, observer);

        monitor.start();
    }

}