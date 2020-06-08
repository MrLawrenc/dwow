package com.github.mrlawrenc.hot.boot;


import com.github.mrlawrenc.AgentMain;
import com.github.mrlawrenc.hot.classloader.ContextClassLoader;
import com.github.mrlawrenc.hot.classloader.HotSwapClassLoader;
import javassist.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

/**
 * @author : MrLawrenc
 * date  2020/6/7 11:34
 */
@Slf4j
public class Boot {

    private Object mainObj;

    /**
     * 应用正式启动
     */
    private void start0() {
        System.out.println("###############################Hot boot init###############################");
        System.out.println("  _  _             _       ___                     _ __    ___      _                              _                         _                               ___                            _  _  \n" +
                " | || |    ___    | |_    / __|  __ __ __ __ _    | '_ \\  / __|    | |    __ _     ___     ___    | |      ___    __ _    __| |    ___      _ _     o O O   | _ \\    _ _    ___    __ __   | || | \n" +
                " | __ |   / _ \\   |  _|   \\__ \\  \\ V  V // _` |   | .__/ | (__     | |   / _` |   (_-<    (_-<    | |__   / _ \\  / _` |  / _` |   / -_)    | '_|   o        |  _/   | '_|  / _ \\   \\ \\ /    \\_, | \n" +
                " |_||_|   \\___/   _\\__|   |___/   \\_/\\_/ \\__,_|   |_|__   \\___|   _|_|_  \\__,_|   /__/_   /__/_   |____|  \\___/  \\__,_|  \\__,_|   \\___|   _|_|_   TS__[O]  _|_|_   _|_|_   \\___/   /_\\_\\   _|__/  \n" +
                "_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"| {======|_| \"\"\" |_|\"\"\"\"\"|_|\"\"\"\"\"|_|\"\"\"\"\"|_| \"\"\"\"| \n" +
                "\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'./o--000'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-'\"`-0-0-' \n");
        System.out.println("###############################Hot boot init###############################");

        System.out.println("current thread loader:" + Thread.currentThread().getContextClassLoader());
        System.out.println("current obj loader:" + this.getClass().getClassLoader());


        //调用main副本方法
        HotSwapClassLoader hotSwapClassLoader = (HotSwapClassLoader) this.getClass().getClassLoader();
        File proxyFile = new File("D:\\B\\Hello.class");
        try (FileInputStream inputStream = new FileInputStream(proxyFile)) {
            byte[] bytes = new byte[(int) proxyFile.length()];
            inputStream.read(bytes);
            Class<?> proxyMain = hotSwapClassLoader.defineClass0("Hello", bytes, 0, bytes.length);
            System.out.println("load copy main,will invoke main");
            proxyMain.getMethod("main$proxy", String[].class).invoke(null, new Object[]{new String[]{"aa"}});

        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("###############################Hot boot  end###############################");
    }

    /**
     * @param loader    类加载器
     * @param startBoot 是否需要启动应用
     * @throws Exception 代理失败
     */
    public static void start(ContextClassLoader loader, boolean startBoot) throws Exception {
        if (startBoot) {
            //启动应用
            Method start = loader.getBoot().getClass().getDeclaredMethod("start0");
            start.setAccessible(true);
            start.invoke(loader.getBoot());
        } else {

        }
    }

    /**
     * 重新装载class
     *
     * @param loader     装载该变化的class文件所需要的加载器
     * @param changeFile 变化的文件，为class或者jar
     */
    public void reloadClz(ContextClassLoader loader, File changeFile) {
        loader.getMonitor().lock();
        try {
            if (changeFile.getName().endsWith(ContextClassLoader.CLASS_FLAG)) {

                try (FileInputStream inputStream = new FileInputStream(changeFile)) {
                    byte[] bytes = inputStream.readAllBytes();
                    loader.reloadClass(loader.filePath2ClzName(changeFile.getAbsolutePath()), bytes, 0, bytes.length, false);
                } catch (Exception e) {
                    log.error("reload class({}) fail ", changeFile.getAbsoluteFile());
                }
            } else if (changeFile.getName().endsWith(ContextClassLoader.JAR_FLAG)) {

            } else {

            }
        } finally {
            loader.getMonitor().unlock();
        }

    }

    /**
     * 热部署代理入口
     *
     * @throws Exception 代理异常
     */
    public static void run() throws Exception {
        System.out.println("run ..... " + AgentMain.agentArgs);

        ContextClassLoader contextClassLoader = new ContextClassLoader(AgentMain.agentArgs);
        contextClassLoader.initHotClass();

        Thread.currentThread().setContextClassLoader(contextClassLoader);

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

            //System.out.println("newMain:"+newMain+"  loader:"+newMain.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }

        start(contextClassLoader, true);
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