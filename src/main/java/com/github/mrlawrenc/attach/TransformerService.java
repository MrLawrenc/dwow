package com.github.mrlawrenc.attach;

import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.monitor.Monitor;
import com.github.mrlawrenc.attach.monitor.impl.JdbcMonitor;
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

    /**
     * 所有monitor实现类集合
     */
    private final List<AbstractMonitor> monitorList = new ArrayList<>();


    public TransformerService() {
        monitorList.add(new JdbcMonitor());

        //初始化所有的单例对象
        monitorList.forEach(AbstractMonitor::init);
    }

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] data) {
        try {
            boolean flag = false;
            Monitor monitor = null;
            for (Monitor currentMonitor : monitorList) {
                if (currentMonitor.isTarget(className)) {
                    monitor = currentMonitor;
                    log.info("target class:{}  use monitor:{}", className.replace("/", "."), monitor.getClass().getName());
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return new byte[0];
            }


            ClassPool pool = new ClassPool(true);
            pool.insertClassPath(new LoaderClassPath(loader));
            CtClass targetClz = pool.get(className.replaceAll("/", "."));

            CtMethod method = monitor.targetMethod(targetClz);
            if (Objects.isNull(method)){
                return new byte[0];
            }

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
        } catch (Exception e) {
            log.error("error", e);
        }
        return new byte[0];
    }
}