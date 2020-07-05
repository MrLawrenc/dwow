package com.github.mrlawrenc.attach.monitor.impl;

import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.DefaultStatistics;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author : MrLawrenc
 * date  2020/7/4 0:16
 * <p>
 * JDBC监控器实现
 */
@Slf4j
public class JdbcMonitor extends AbstractMonitor {
    private static final String TARGET_CLZ = "com.mysql.cj.jdbc.NonRegisteringDriver";

    /**
     * {@link Connection}中需要代理的方法名集合
     */
    private static final String[] PROXY_CONNECTION_METHOD = new String[]{"prepareStatement"};
    /**
     * {@link PreparedStatement}中需要代理的方法名集合
     * 若执行查询时，没有使用{@link PreparedStatement#executeQuery()}方法获取查询结果，
     * 则之后会调用{@link PreparedStatement#getResultSet()}方法获取结果集
     */
    private static final String[] STATEMENT_METHOD = new String[]{"executeUpdate", "execute", "executeQuery", "getResultSet"};


    private String begin = "long start = System.currentTimeMillis();" +
            "com.github.mrlawrenc.attach.monitor.impl.JdbcMonitor collector=com.github.mrlawrenc.attach.monitor.impl.JdbcMonitor.INSTANCE;";

    private String end = "java.sql.Connection result=collector.proxyConnection((java.sql.Connection)c);" +
            "long cos = System.currentTimeMillis()-start;" +
            "System.out.println(\"方法耗时:\"+cos);";

    private String catchSrc = "{ $e.printStackTrace();" +
            "throw $e;}";

    private String finallySrc = "{Long end=System.nanoTime();\n" +
            "System.out.println(\"finally end:\");}";

    @Override
    public void init() {
        JdbcMonitor.INSTANCE = this;
    }

    /**
     * 生成connection代理对象
     *
     * @param connection 源对象
     * @return 代理对象
     */
    public Connection proxyConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(JdbcMonitor.class.getClassLoader(), new Class[]{Connection.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(connection, args);
                if (Arrays.asList(PROXY_CONNECTION_METHOD).contains(method.getName()) && result instanceof PreparedStatement) {
                    System.out.println("url:" + connection.getMetaData().getURL());
                    System.out.println("sql:" + args[0]);
                    result = proxyStatement((PreparedStatement) result);
                }
                return result;
            }
        });
    }

    /**
     * 生成PreparedStatement代理对象
     *
     * @param statement connection对象执行prepareStatement方法返回结果
     * @return 代理connection#prepareStatement()结果对象
     */
    private PreparedStatement proxyStatement(PreparedStatement statement) {
        return (PreparedStatement) Proxy.newProxyInstance(JdbcMonitor.class.getClassLoader()
                , new Class[]{PreparedStatement.class}
                , (proxy, method, args) -> {
                    Object result = method.invoke(statement, args);
                    System.out.println("方法名:" + method.getName() + "参数:" + args + " 结果:" + result);
                    if (Arrays.asList(STATEMENT_METHOD).contains(method.getName())) {
                        if (result instanceof ResultSet) {
                            System.out.println("执行查询操作");
                        } else {
                            System.out.println("执行增、删、改操作");
                        }
                    }
                    return result;
                });
    }


    @Override
    public boolean isTarget(String className) {
        return TARGET_CLZ.equals(className.replace("/", "."));
    }

    @Override
    public CtMethod targetMethod(CtClass clz) throws NotFoundException {
        return clz.getMethod("connect", "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");
    }

    @Override
    public MethodInfo getMethodInfo(String methodName) {
        return MethodInfo.newBuilder().createBody(this, methodName);

/*        return MethodInfo.newBuilder()
                .tryBody(begin, methodName, end)
                .catchBody(catchSrc)
                .finallyBody(finallySrc)
                .create();*/
    }

    @Override
    public DefaultStatistics begin(Object obj, Object... args) {
        DefaultStatistics defaultStatistics = new DefaultStatistics();
        log.info("begin class:{} args:{}", obj.getClass(), args);
        defaultStatistics.setStart(System.currentTimeMillis());
        return defaultStatistics;
    }

    @Override
    public void exception(DefaultStatistics statistics, Throwable t) {
        statistics.setT(t);
    }

    @Override
    public Object end(DefaultStatistics current, Object obj) {
        Object result = obj;
        if (Objects.nonNull(obj) && obj instanceof Connection) {
            current.setOldResult(obj);
            result = proxyConnection((Connection) current.getOldResult());
            current.setNewResult(result);
        }

        current.setEnd(System.currentTimeMillis());
        log.info("cost time:{}", current.getEnd() - current.getStart());
        log.info("statistics:{}", current);
        return result;
    }


}