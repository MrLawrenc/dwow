package com.github.mrlawrenc.attach.monitor;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:00
 * <p>
 * 监控接口
 */
public interface Monitor {


    /**
     * 是否是监控目标
     *
     * @param className 格式com/github/A.class
     * @return true 是
     */
    boolean isTarget(String className);

    /**
     * 获取目标方法
     *
     * @param clz 目标方法所在类
     * @return 目标方法
     */
    CtMethod targetMethod(CtClass clz) throws NotFoundException;

    /**
     * 返回植入方法之前的代码
     *
     * @param oldMethodName 复制之前的方法名
     * @return 返回植入方法之前的代码
     */
    MethodInfo getMethodInfo(String oldMethodName);

    DefaultStatistics begin(Object obj, Object... args);

    void exception(DefaultStatistics statistics, Throwable t);

    /**
     * 原方法执行结果
     *
     * @param statistics 统计类
     * @param result     返回值
     */
    Object end(DefaultStatistics statistics, Object result);


}