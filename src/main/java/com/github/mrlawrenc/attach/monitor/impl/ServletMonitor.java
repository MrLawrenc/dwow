package com.github.mrlawrenc.attach.monitor.impl;

import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.statistics.ServletStatistics;
import com.github.mrlawrenc.attach.statistics.Statistics;
import com.github.mrlawrenc.attach.write.WriterResp;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author : MrLawrenc
 * date  2020/7/4 0:16
 * <p>
 * JDBC监控器实现
 */
@Slf4j
public class ServletMonitor extends AbstractMonitor {
    private static final String TARGET_CLZ = "javax.servlet.http.HttpServlet";
    public static AbstractMonitor INSTANCE;

    @Override
    public void init() {
        ServletMonitor.INSTANCE = this;
    }


    @Override
    public boolean isTarget(String className) {
        return TARGET_CLZ.equals(className.replace("/", "."));
    }

    @Override
    public CtMethod targetMethod(ClassPool pool, CtClass clz) throws NotFoundException {
        return clz.getDeclaredMethod("service", new CtClass[]{pool
                .get("javax.servlet.http.HttpServletRequest"), pool.get("javax.servlet.http.HttpServletResponse")});
    }

    @Override
    public MethodInfo getMethodInfo(String methodName) {
        return MethodInfo.newBuilder().createVoidBody(this, methodName);
    }

    @Override
    public Statistics begin(Object obj, Object... args) {
        ServletStatistics statistics = new ServletStatistics("0");
        HttpServletRequest servletRequest = (HttpServletRequest) args[0];
        HttpServletResponse servletResponse = (HttpServletResponse) args[1];
        StringBuffer url = servletRequest.getRequestURL();
        statistics.setUrl(url.toString());
        statistics.setArgs(args);
        statistics.setStartTime(System.currentTimeMillis());
        statistics.setType(servletRequest.getMethod());
        return statistics;
    }

    @Override
    public void exception(Statistics statistics, Throwable t) {
        statistics.setT(t);
    }

    @Override
    public Object end(Statistics current, Object obj) {
        current.setEndTime(System.currentTimeMillis());
        log.info("servlet cost time:" + (current.getEndTime() - current.getStartTime()));
        return obj;
    }

    @Override
    public WriterResp write(Statistics statistics) {
        return null;
    }
}