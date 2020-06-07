package com.github.mrlawrenc.hot.classloader.boot;


import com.github.mrlawrenc.AgentMain;
import com.github.mrlawrenc.hot.classloader.HotSwapClassLoader;
import javassist.*;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
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


        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass main = pool.get("com.swust.Main");
            //如果CtClass通过writeFile(),toClass(),toBytecode()转换了类文件，javassist冻结了CtClass对象。以后是不允许修改这个 CtClass对象
            //cc.writeFile();//冻结
            //cc.defrost();//解冻
            //cc.setSuperclass(...);   // OK since the class is not frozen.可以重新操作CtClass，因为被解冻了
            main.defrost();
            main.setName("Hello");
            main.writeFile("D:\\B");

            //System.out.println("newMain:"+newMain+"  loader:"+newMain.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //调用main方法
        HotSwapClassLoader hotSwapClassLoader = (HotSwapClassLoader) this.getClass().getClassLoader();
        File proxyFile = new File("D:\\B\\Hello.class");
        try (FileInputStream inputStream = new FileInputStream(proxyFile)) {
            byte[] bytes = new byte[(int) proxyFile.length()];
            inputStream.read(bytes);
            Class<?> proxyMain = hotSwapClassLoader.defineClass0("Hello", bytes, 0, bytes.length);
            System.out.println("load copy main,will invoke main");
            proxyMain.getMethod("main", String[].class).invoke(null, new Object[]{new String[]{"aa"}});

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
                            boot.getMethod("startListenFile", String.class).invoke(bootObj, path);
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

    /**
     * 创建一个Person 对象
     *
     * @throws Exception
     */
    public static void createPseson() throws Exception {
        ClassPool pool = ClassPool.getDefault();

        // 1. 创建一个空类
        CtClass cc = pool.makeClass("com.rickiyang.learn.javassist.Person");

        // 2. 新增一个字段 private String name;
        // 字段名为name
        CtField param = new CtField(pool.get("java.lang.String"), "name", cc);
        // 访问级别是 private
        param.setModifiers(Modifier.PRIVATE);
        // 初始值是 "xiaoming"
        cc.addField(param, CtField.Initializer.constant("xiaoming"));

        // 3. 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setName", param));
        cc.addMethod(CtNewMethod.getter("getName", param));

        // 4. 添加无参的构造函数
        CtConstructor cons = new CtConstructor(new CtClass[]{}, cc);
        cons.setBody("{name = \"xiaohong\";}");
        cc.addConstructor(cons);

        // 5. 添加有参的构造函数
        cons = new CtConstructor(new CtClass[]{pool.get("java.lang.String")}, cc);
        // $0=this / $1,$2,$3... 代表方法参数
        cons.setBody("{$0.name = $1;}");
        cc.addConstructor(cons);

        // 6. 创建一个名为printName方法，无参数，无返回值，输出name值
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "printName", new CtClass[]{}, cc);
        ctMethod.setModifiers(Modifier.PUBLIC);
        ctMethod.setBody("{System.out.println(name);}");
        cc.addMethod(ctMethod);

        //这里会将这个创建的类对象编译为.class文件
        cc.writeFile("D:\\A");

        CtClass ctClass = ClassPool.getDefault().get("com.github.mrlawrenc.hot.classloader.boot.Boot");

        ctClass.writeFile("D:\\B");
    }

    public static boolean second = false;

    public static void main(String[] args) throws Exception {
        if (second) {
            System.out.println("第二次调用main。。。。。。");
            return;
        }
        second = true;
        try {
            createPseson();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileInputStream stream = new FileInputStream(new File("D:\\B\\com\\github\\mrlawrenc\\hot\\classloader\\boot\\Boot.class"));

        byte[] bytes = stream.readAllBytes();
        Class<?> class0 = new HotSwapClassLoader("").defineClass0("com.github.mrlawrenc.hot.classloader.boot.Boot", bytes, 0, bytes.length);

        System.out.println("new loader : " + class0.getClassLoader());

        Method main = class0.getMethod("main", String[].class);
        System.out.println("static test method : " + main);
        main.invoke(null, new Object[]{new String[]{"a", "b"}});
    }

    public static void test(String[] args) {
        System.out.println("测试的静态方法");
    }
}