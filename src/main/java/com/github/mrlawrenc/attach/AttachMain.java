package com.github.mrlawrenc.attach;

import javassist.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hz20035009-逍遥
 * date   2020/6/10 14:58
 * <p>
 * attach注入的入口
 */
@Slf4j
public class AttachMain {
    public static void premain(String agentOps, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        System.out.println("######################################################################");
        System.out.println("######################################################################");
        System.out.println("#######                     Agent  Success                     #######");
        System.out.println("######################################################################");
        System.out.println("######################################################################");
        inst.addTransformer(new TransformerService(), true);
        // p(agentOps,inst);
        //p1(agentOps, inst);
    }

    /**
     * @param agentArgs 格式 {basePkg:com.swust}
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        inst.retransformClasses(p(agentArgs, inst).toArray(Class[]::new));
    }

    public static List<Class<?>> p(String agentArgs, Instrumentation inst) {
        System.out.println("######################################################################");
        System.out.println("######################################################################");
        System.out.println("#######                     Attach Success                     #######");
        System.out.println("######################################################################");
        System.out.println("######################################################################");

        String basePkg = "org";
        List<Class<?>> targetClz = new ArrayList<>();
        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(loadedClass)) {
                if (Driver.class.isAssignableFrom(loadedClass)) {
                    targetClz.add(loadedClass);
                }

            }
        }
        /**
         * <pre>
         *         String source="{" +
         *             "long begin = System.currentTimeMillis();" +
         *             "Object result;" +
         *             "try{" +
         *             "        result = ($w)%s$agent($$);" +
         *             "}finally{" +
         *             "        long end = System.currentTimeMillis();" +
         *             "        System.out.println(end - begin);" +
         *             "}" +
         *             "return ($r)result;"+
         *             "}";
         * </pre>
         */

        inst.addTransformer(new ClassFileTransformer() {
            @SneakyThrows
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if ("com.mysql.cj.jdbc.NonRegisteringDriver".equals(className.replace("/", "."))) {
                    CtClass driver = null;
                    try {
                 /*       ClassPool pool = ClassPool.getDefault();
                       pool.appendClassPath(new ClassClassPath(classBeingRedefined));
                        pool.appendClassPath(new LoaderClassPath(loader));*/
                        ClassPool pool = new ClassPool();
                        pool.insertClassPath(new LoaderClassPath(loader));
                        log.info("poll loader:{}", pool);

                        //在premain阶段还没有字节码 需要自身编译，所以使用make方法。若是agentmain阶段，则使用已有字节码，使用get
                        driver = pool.get("com.mysql.cj.jdbc.NonRegisteringDriver");
                        log.info("ct class:{}", driver);
                        CtMethod connect = driver.getMethod("connect", "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");


                        //============================================================
                        System.out.println("开始复制方法:" + connect.getName() + "agent");
                        //添加复制之后的方法
                        CtMethod proxyMethod = CtNewMethod.copy(connect, connect.getName() + "agent", driver, null);
                        driver.addMethod(proxyMethod);
                        System.out.println("复制方法:" + connect.getName() + "agent 成功");
                        //载入异常类，改变原方法，在原方法里面调用新拷贝的方法
                        CtClass throwable = pool.get("java.lang.Throwable");
                        connect.setBody("{\n"
                                + "long start = System.currentTimeMillis();"
                                + "com.github.mrlawrenc.attach.service.JdbcCollector collector=com.github.mrlawrenc.attach.service.JdbcCollector.INSTANCE;"
                                + "Object c=($w)$0.connectagent($$);"
                                + "java.sql.Connection result=collector.proxy((java.sql.Connection)c);"
                                + " System.out.println(System.currentTimeMillis()-start);"
                                + "return ($r)result;"
                                + "\n}");
                        connect.addCatch("{\nSystem.out.println(\"一场:\"+System.nanoTime());\nthrow $e;}", throwable);


                        //在try catch之后添加，作为finally
                        connect.insertAfter("{Long end=System.nanoTime();\nSystem.out.println(\"finally end:\");}", true);
                        //============================================================

                        //connect.insertAfter("{\nSystem.out.println(\"结束:\"+System.nanoTime());}");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return driver.toBytecode();
                }

                return classfileBuffer;
            }
        }, true);
        return targetClz;
    }

    public static List<Class<?>> p1(String agentArgs, Instrumentation inst) {
        String basePkg = "org";
        List<Class<?>> targetClz = new ArrayList<>();
        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(loadedClass)) {
                if (Driver.class.isAssignableFrom(loadedClass)) {
                    targetClz.add(loadedClass);
                }

            }
        }
        inst.addTransformer(new ClassFileTransformer() {
            @SneakyThrows
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if ("com.mysql.cj.jdbc.NonRegisteringDriver".equals(className.replace("/", "."))) {
                    CtClass driver = null;
                    try {
                        ClassPool pool = new ClassPool(true);
                        pool.insertClassPath(new LoaderClassPath(loader));
                        log.info("poll loader:{}", pool);

                        driver = pool.get("com.mysql.cj.jdbc.NonRegisteringDriver");
                        log.info("ct class:{}", driver);
                        CtMethod connect = driver.getMethod("connect", "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");

                        CtMethod proxyMethod = CtNewMethod.copy(connect, connect.getName() + "agent", driver, null);
                        driver.addMethod(proxyMethod);
                        CtClass throwable = pool.get("java.lang.Throwable");

                        connect.setBody("{" +
                                "long begin = System.currentTimeMillis();" +
                                "Object result;" +
                                "try{" +
                                //测试catch 模拟异常 不能直接写1/0 javassist的编译器可以直接检查出来0.0，在编译时就会报错，就测试不了异常代码块了
                                "int a=1;" +
                                "int b=0;" +
                                "int c=a/b;" +


                                "result=($w)$0.connectagent($$);" +
                                "}catch(java.lang.Throwable t){" +
                                "System.out.println(\"出现异常:\"+t.getMessage());" +
                                " t.printStackTrace();" +
                                "throw t;" +
                                "}finally{" +
                                "long end = System.currentTimeMillis();" +
                                "System.out.println(\"耗时:\"+(end-begin));" +
                                "}" +
                                "return ($r) result;" +
                                "}");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return driver.toBytecode();
                }

                return classfileBuffer;
            }
        }, true);
        return targetClz;
    }
}