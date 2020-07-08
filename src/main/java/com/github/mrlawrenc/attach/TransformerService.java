package com.github.mrlawrenc.attach;

import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.monitor.Monitor;
import com.github.mrlawrenc.attach.monitor.impl.JdbcMonitor;
import com.github.mrlawrenc.attach.monitor.impl.ServletMonitor;
import javassist.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:13
 */
@Slf4j
public class TransformerService implements ClassFileTransformer {
    private static final String AGENT_SUFFIX = "$agent";

    private static final String STACK_SRC = "{" +
            "StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();\n" +
            "StackTraceElement stackTraceElement = stackTrace[1];\n" +
            "com.github.mrlawrenc.attach.util.StackBinaryTree stackTree = com.github.mrlawrenc.attach.util.ThreadLocalUtil.globalThreadLocal.get();\n" +
            "if(Objects.nonNull(stackTree)){\n" +
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
/*        int modifiers = classBeingRedefined.getModifiers();
        if (Modifier.isNative(modifiers) || Modifier.isEnum(modifiers) || Modifier.isInterface(modifiers)) {
            System.out.println(className);
            return new byte[0];
        }*/

        boolean flag = false;
        Monitor monitor = null;
        for (Monitor currentMonitor : monitorList) {
            if (Objects.nonNull(className) && currentMonitor.isTarget(className)) {
                monitor = currentMonitor;
                flag = true;
                break;
            }
        }
        try {
            if (flag) {
                ClassPool pool = new ClassPool(true);
                pool.insertClassPath(new LoaderClassPath(loader));
                CtClass targetClz = pool.get(className.replaceAll("/", "."));
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
            } else {
            }
        } catch (Exception e) {
            log.error("error", e);
        }

        return new byte[0];
    }
}