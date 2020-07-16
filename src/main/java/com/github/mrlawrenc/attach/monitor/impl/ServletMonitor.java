package com.github.mrlawrenc.attach.monitor.impl;

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
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

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
        HttpServletResponse servletResponse = (HttpServletResponse) args[1];
        StringBuffer url = servletRequest.getRequestURL();
        statistics.setUrl(url.toString());
        statistics.setArgs(args);
        statistics.setStartTime(System.currentTimeMillis());

        statistics.setMethod(servletRequest.getMethod());

        // if (Objects.nonNull(servletRequest.getContentType()) && servletRequest.getContentType().contains("json")) {
        //读了req之后就没法读取了 ,所以将req包装为可重复读取的httpRequest
        MultiReadHttpServletRequest wrapperRequest = new MultiReadHttpServletRequest(servletRequest);
        args[0] = wrapperRequest;

        try (ServletInputStream inputStream = wrapperRequest.getInputStream()) {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            System.out.println("请求内容:" + new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Wrapper HttpServletRequest Fail(" + e.getMessage() + ")", e);
        }

        try {
            BufferedReader reader = wrapperRequest.getReader();
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            System.out.println("reader 参数 : " + sb);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //url参数
        Enumeration<String> parameterNames = wrapperRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            System.out.println("url参数" + parameterNames.nextElement());
        }
        // }
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
    public WriterResp write(Writeable statistics) {
        return null;
    }

    /**
     * 包装req对象，使得req可以重复读取.
     * <p>
     * 当在使用地获取input stream时 实际获取的是包装之后的input stream{@link CachedServletInputStream}
     */
    public static class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
        private ByteArrayOutputStream cachedBytes;

        public MultiReadHttpServletRequest(HttpServletRequest request) {
            super(request);
            cachedBytes = new ByteArrayOutputStream();

            //复制 req 流到内存中
            try (ServletInputStream inputStream = super.getInputStream()) {
                byte[] data = new byte[inputStream.available()];
                inputStream.read(data);
                cachedBytes.write(data);
            } catch (IOException e) {
                throw new RuntimeException("req stream copy fail!");
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
        private ByteArrayInputStream input;

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