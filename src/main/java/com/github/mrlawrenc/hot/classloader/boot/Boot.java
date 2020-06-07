package com.github.mrlawrenc.hot.classloader.boot;


import com.github.mrlawrenc.hot.classloader.HotSwapClassLoader;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

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

        System.out.println("###############################Hot boot  end###############################");
    }

    public static void start0(HotSwapClassLoader loader) throws Exception {
        Class<?> boot = loader.loadClass("com.github.mrlawrenc.hot.classloader.boot.Boot");
        boot.getMethod("start").invoke(boot.getConstructor().newInstance());

    }

    /**
     * 热部署代理入口
     *
     * @param path 当前需要热部署的class path路径
     * @throws Exception 代理异常
     */
    public static void run(String path) throws Exception {
        System.out.println("run ..... "+path);
        HotSwapClassLoader hotSwapClassLoader = new HotSwapClassLoader(path);
        startListenFile(path);
        start0(hotSwapClassLoader);
    }

    public static void startListenFile(String path) throws Exception {
        System.out.println("listening : " + path);
        FileAlterationObserver observer = new FileAlterationObserver(path);

        observer.addListener(new FileListener());

        //5s
        FileAlterationMonitor monitor = new FileAlterationMonitor(5000, observer);

        monitor.start();
    }

}