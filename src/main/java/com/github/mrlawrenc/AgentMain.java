package com.github.mrlawrenc;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
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


    private CtMethod newMainMethod = null;

    /**
     * jvm启动时调用,支持jdk version >=1.5
     *
     * @param agentOps agent参数
     * @param inst     {@link Instrumentation}
     */
    public static void premain(String agentOps, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
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

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className.contains("Boot")){
            //注入main方法的方法体
            CtClass boot = ClassPool.getDefault().get("com.github.mrlawrenc.hot.classloader.boot.Boot");
            CtMethod start = boot.getDeclaredMethod("start");
            start.insertAfter("{\ncom.swust.Main.main$proxy(\"" + "main 方法 参数 " + "\");}");

            return boot.toBytecode();

        }

        try {
            String path = "F:\\openSources\\test\\out\\production\\test";
            if (className.replaceAll("/", ".").contains("com.swust.Main")) {
                System.out.println("识别到main方法所在的类：" + className);

                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

                //当当前类加载器class not found时，可以手动加载classPool.get("com.github.mrlawrenc.hot.classloader.boot.Boot").toClass();

                CtMethod ctMethod = ctClass.getDeclaredMethod("main");

                //创建新的方法，复制原来的方法
                CtMethod newMethod = CtNewMethod.copy(ctMethod, "main", ctClass, null);
                newMethod.setName("main$proxy");



                StringBuffer body = new StringBuffer();
                body.append("{\ncom.github.mrlawrenc.hot.classloader.boot.Boot.run(\"").append(path).append("\");\n}");
                      /*  body.append("System.out.println(\"center code###############\");\n");
                        body.append("System.out.println(\"main stop.................\"); }");*/

                ctMethod.setBody(body.toString());


                ctClass.addMethod(newMethod);
                return ctClass.toBytecode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }
}