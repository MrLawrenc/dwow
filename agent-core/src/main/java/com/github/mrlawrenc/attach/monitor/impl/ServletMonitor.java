package com.github.mrlawrenc.attach.monitor.impl;

import cn.hutool.json.JSONUtil;
import com.github.mrlawrenc.attach.StatisticsType;
import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.stack.StackNode;
import com.github.mrlawrenc.attach.statistics.ServletStatistics;
import com.github.mrlawrenc.attach.statistics.Statistics;
import com.github.mrlawrenc.attach.util.GlobalUtil;
import com.github.mrlawrenc.attach.util.ThreadLocalUtil;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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
    public StatisticsType type() {
        return StatisticsType.SERVLET;
    }

    @Override
    public Statistics begin(Object obj, Object... args) {
        ThreadLocalUtil.globalThreadLocal.set(new StackNode());

        ServletStatistics statistics = GlobalUtil.createStatistics(ServletStatistics.class);
        HttpServletRequest servletRequest = (HttpServletRequest) args[0];
        StringBuffer url = servletRequest.getRequestURL();
        statistics.setUrl(url.toString());
        statistics.setArgs(args);
        statistics.setStartTime(System.currentTimeMillis());

        statistics.setMethod(servletRequest.getMethod());

        // if (Objects.nonNull(servletRequest.getContentType()) && servletRequest.getContentType().contains("json")) {
        //读了req之后就没法读取了 ,所以将req包装为可重复读取的httpRequest
        MultiReadHttpServletRequest wrapperRequest = new MultiReadHttpServletRequest(servletRequest);
        args[0] = wrapperRequest;


        try {
            BufferedReader reader = wrapperRequest.getReader();
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            statistics.setBodyData(sb.toString());
        } catch (IOException e) {
            log.error("read body data error", e);
        }
        //url参数
        Enumeration<String> parameterNames = wrapperRequest.getParameterNames();
        Map<String, String> urlParam = new HashMap<>();
        while (parameterNames.hasMoreElements()) {
            String element = parameterNames.nextElement();
            urlParam.put(element, wrapperRequest.getParameter(element));
        }
        statistics.setUrlData(JSONUtil.toJsonStr(urlParam));
        return statistics;
    }

    @Override
    public void exception(Statistics statistics, Throwable t) {
        statistics.setT(t);
    }

    @Override
    public Object end(Statistics current, Object obj) {
        current.setEndTime(System.currentTimeMillis());
        ServletStatistics servletStatistics = (ServletStatistics) current;
        HttpServletResponse servletResponse = (HttpServletResponse) servletStatistics.getArgs()[1];
        servletStatistics.setRespStatus(servletResponse.getStatus());
        log.info("monitor data:{}", JSONUtil.toJsonStr(servletStatistics));
        return obj;
    }

    @Override
    public WriterResp write(Writeable statistics) {
        return null;
    }

    /**
     * 包装req对象，使得req可以重复读取.
     * <p>RequestWrapper
     * 当在使用地获取input stream时 实际获取的是包装之后的input stream{@link CachedServletInputStream}
     */
    public static class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
        private final ByteArrayOutputStream cachedBytes;

        public MultiReadHttpServletRequest(HttpServletRequest request) {
            super(request);
            cachedBytes = new ByteArrayOutputStream();

            //复制 req 流到内存中,切记不能关inputStream
            try {
                ServletInputStream inputStream = request.getInputStream();
                byte[] data = new byte[1024];

                int start = 0;
                while (inputStream.read(data) > 0) {
                    cachedBytes.write(data, start, data.length);
                    start += data.length;
                }
            } catch (IOException e) {
                log.error("copy wrapper request fail!", e);
            }

        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedServletInputStream(cachedBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }

    public static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;

        public CachedServletInputStream(ByteArrayOutputStream cachedBytes) {
            // create a new input stream from the cached request body
            byte[] bytes = cachedBytes.toByteArray();
            input = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
    }
}