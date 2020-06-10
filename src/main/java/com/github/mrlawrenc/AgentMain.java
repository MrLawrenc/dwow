package com.github.mrlawrenc;

import com.github.mrlawrenc.hot.boot.Boot;
import javassist.*;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

/**
 * @author : MrLawrenc
 * date  2020/6/6 19:53
 * <p>
 * 入口函数
 */
public class AgentMain {


    public static String agentArgs = "";

    /**
     * jvm启动时调用,支持jdk version >=1.5
     *
     * @param agentOps agent参数
     * @param inst     {@link Instrumentation}
     */
    public static void premain(String agentOps, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        agentArgs = agentOps;
        inst.addTransformer(new ClzInterceptor());
    }

    /**
     * 基于attach的方式在jvm启动后的任意时间点调用,支持jdk version >=1.6
     *
     * @param agentArgs agent参数
     * @param inst      {@link Instrumentation}
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {

    }
}

class ClzInterceptor implements ClassFileTransformer {
    public static boolean changeMain = false;

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className.contains("Boot")) {
            //注入main方法的方法体
            CtClass boot = ClassPool.getDefault().get("com.github.mrlawrenc.hot.classloader.boot.Boot");

            boot.setName("Main$Proxy");
            CtMethod start = boot.getDeclaredMethod("start");
            start.insertAfter("{\ncom.swust.Main.main$proxy(\"" + "main 方法 参数 " + "\");}");

            return boot.toBytecode();

        }

        try {

            if (!changeMain && className.replaceAll("/", ".").contains("com.swust.Main")) {
                System.out.println("识别到main方法所在的类：" + className);

                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                //CtClass ctClass = classPool.get("com.swust.Main");

                //当当前类加载器class not found时，可以手动加载classPool.get("com.github.mrlawrenc.hot.classloader.boot.Boot").toClass();
                CtMethod ctMethod = ctClass.getDeclaredMethod("main");

                //创建新的方法，复制原来的方法
                CtMethod newMethod = CtNewMethod.copy(ctMethod, "main", ctClass, null);
                newMethod.setName("main$proxy");

                newMethod.addCatch("{\nSystem.out.println(\"结束:\"+System.nanoTime());\nthrow $e;}",classPool.get(Throwable.class.getName()));
                ctClass.addMethod(newMethod);
                createNewClz(newMethod);

              /*   StringBuffer body = new StringBuffer();
                body.append("{\ncom.github.mrlawrenc.hot.classloader.boot.Boot.run(\"").append(path).append("\");\n");
                       body.append("System.out.println(\"center code###############\");\n");
                        body.append("System.out.println(\"main stop.................\"); }");*/

                //String body = "{com.github.mrlawrenc.hot.boot.Boot.run();}";

                ctMethod.setBody("{" + Boot.class.getName() + ".run();}");


                changeMain = true;
                return ctClass.toBytecode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }

    /**
     * 生成新的class文件
     */
    public void createNewClz(CtMethod method) throws Exception {
        ClassPool pool = ClassPool.getDefault();

        CtClass ctClass = pool.makeClass("com.study.javassist.Emp");

        //添加字段
        //首先添加字段private String ename
        CtField enameField = new CtField(pool.getCtClass("java.lang.String"), "ename", ctClass);
        enameField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(enameField);

        //其次添加字段privtae int eno
        CtField enoField = new CtField(pool.getCtClass("int"), "eno", ctClass);
        enoField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(enoField);

        //为字段ename和eno添加getXXX和setXXX方法
        ctClass.addMethod(CtNewMethod.getter("getEname", enameField));
        ctClass.addMethod(CtNewMethod.setter("setEname", enameField));
        ctClass.addMethod(CtNewMethod.getter("getEno", enoField));
        ctClass.addMethod(CtNewMethod.setter("setEno", enoField));

        //添加构造函数
        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
        //为构造函数设置函数体
        StringBuffer buffer = new StringBuffer();
        buffer.append("{\n")
                .append("ename=\"yy\";\n")
                .append("eno=001;\n}");
        ctConstructor.setBody(buffer.toString());
        //把构造函数添加到新的类中
        ctClass.addConstructor(ctConstructor);

        //添加自定义方法
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "printInfo", new CtClass[]{}, ctClass);
        //为自定义方法设置修饰符
        ctMethod.setModifiers(Modifier.PUBLIC);
        //为自定义方法设置函数体
        StringBuffer buffer2 = new StringBuffer();
        buffer2.append("{\nSystem.out.println(\"begin!\");\n")
                .append("System.out.println(ename);\n")
                .append("System.out.println(eno);\n")
                .append("System.out.println(\"over!\");\n")
                .append("}");
        ctMethod.setBody(buffer2.toString());
        ctClass.addMethod(ctMethod);

       /* method.setName("MainProxy");
        ctClass.addMethod(method);*/

        //为了验证效果，下面使用反射执行方法printInfo
        Class<?> clazz = ctClass.toClass();
        Object obj = clazz.newInstance();
        obj.getClass().getMethod("printInfo", new Class[]{}).invoke(obj, new Object[]{});

        //把生成的class文件写入文件
        byte[] byteArr = ctClass.toBytecode();
        FileOutputStream fos = new FileOutputStream(new File("D://Emp.class"));
        fos.write(byteArr);
        fos.close();


    }
}