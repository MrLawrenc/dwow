package com.github.mrlawrenc.attach;

import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.monitor.Monitor;
import com.github.mrlawrenc.attach.monitor.impl.JdbcMonitor;
import com.github.mrlawrenc.attach.monitor.impl.ServletMonitor;
import com.github.mrlawrenc.attach.util.StackBinaryTree;
import com.github.mrlawrenc.attach.util.ThreadLocalUtil;
import javassist.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:13
 */
@Slf4j
public class TransformerService implements ClassFileTransformer {
    private static final String AGENT_SUFFIX = "$agent";
    /**
     * 堆栈插桩需要排除的方法
     */
    private static final List<String> EXCLUDE_METHOD = Arrays.asList("toString", "equals", "hashCode");

    private static final String STACK_SRC = "{" +
            //"StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[1];\n" +
            "StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();\n" +
            StackBinaryTree.class.getName() + " stackTree = " + ThreadLocalUtil.class.getName() + ".globalThreadLocal.get();\n" +
            "if(java.util.Objects.nonNull(stackTree)){\n" +
            "   stackTree.addNode(stackTraceElement);\n" +
            "}\n" +
            "}";

    /**
     * 所有monitor实现类集合
     */
    private final List<AbstractMonitor> monitorList = new ArrayList<>();


    public TransformerService() {
        monitorList.add(new JdbcMonitor());
        monitorList.add(new ServletMonitor());

        //初始化所有的单例对象 fix
        monitorList.forEach(AbstractMonitor::init);
    }

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] data) {
        if (Objects.isNull(className) || className.replaceAll("/", ".").equals(StackBinaryTree.class.getName())) {
            return new byte[0];
        }
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(new LoaderClassPath(loader));
        //若需要在系统类里面植入代码（即当前loader为app loader的双亲），需要在class pool中加入app loader ，否则无法找到相关植入的类
        pool.appendClassPath(new ClassClassPath(StackBinaryTree.class));
        CtClass targetClz = pool.get(className.replaceAll("/", "."));

        int modifiers = targetClz.getModifiers();
        if (Modifier.isNative(modifiers) || Modifier.isEnum(modifiers) || Modifier.isInterface(modifiers)) {
            return new byte[0];
        }

        boolean flag = false;
        Monitor monitor = null;
        for (Monitor currentMonitor : monitorList) {
            if (currentMonitor.isTarget(className)) {
                monitor = currentMonitor;
                flag = true;
                break;
            }
        }
        String clzName = className.replaceAll("/", ".");
        try {
            if (flag) {
                log.info("target class:{}  use monitor:{}", className.replace("/", "."), monitor.getClass().getName());
                CtMethod method = monitor.targetMethod(pool, targetClz);
                if (Objects.nonNull(method)) {
                    String newMethodName = method.getName() + AGENT_SUFFIX;
                    log.info("start copy new method : {}", newMethodName);
                    CtMethod newMethod = CtNewMethod.copy(method, newMethodName, targetClz, null);
                    targetClz.addMethod(newMethod);

                    CtClass throwable = pool.get("java.lang.Throwable");

                    MethodInfo methodInfo = monitor.getMethodInfo(newMethodName);
                    if (methodInfo.isNewInfo()) {
                        method.setBody(methodInfo.getNewBody());
                    } else {
                        method.setBody(methodInfo.getTryBody());
                        method.addCatch(methodInfo.getCatchBody(), throwable);
                        method.insertAfter(methodInfo.getFinallyBody(), true);
                    }
                    log.info("copy method end");
                    return targetClz.toBytecode();
                }
            } else if (clzName.startsWith("com.huize") && !clzName.contains("sun") && !className.contains("java")) {
                CtMethod[] methods = targetClz.getDeclaredMethods();
                List<String> fieldNameList = Stream.of(targetClz.getDeclaredFields())
                        .map(field -> field.getName().toLowerCase())
                        .collect(Collectors.toList());

                for (CtMethod method : methods) {
                    if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                        continue;
                    }
                    if (EXCLUDE_METHOD.contains(method.getName())) {
                        continue;
                    }
                    String name = method.getName();
                    if ((name.startsWith("set") || name.startsWith("get")) && fieldNameList.contains(name.substring(3).toLowerCase())) {
                        //排除getter setter
                        continue;
                    }

                    //插入堆栈统计
                    log.info("植入堆栈的类:" + className + "#" + name);
                    method.insertBefore(STACK_SRC);

                    //也可以使用如下方法插入堆栈
/*                    method.addLocalVariable("stackTree",pool.get(StackBinaryTree.class.getName()));
                    method.addLocalVariable("stackTraceElement",pool.get(StackTraceElement.class.getName()));
                    String src="{" +
                            "stackTraceElement = Thread.currentThread().getStackTrace()[1];\n" +
                            "stackTree = " + ThreadLocalUtil.class.getName() + ".globalThreadLocal.get();\n" +
                            "System.out.println(\"#########我是植入代码#########\");" +
                            "if(java.util.Objects.nonNull(stackTree)){\n" +
                            "   stackTree.addNode(stackTraceElement);\n" +
                            "}\n" +
                            "}";
                    method.insertBefore(src);*/
                }
                return targetClz.toBytecode();
            }
        } catch (Exception e) {
            log.error("error", e);
        }

        return new byte[0];
    }
}