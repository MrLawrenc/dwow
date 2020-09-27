package com.github.mrlawrenc.attach.monitor.impl;

import cn.hutool.json.JSONUtil;
import com.github.mrlawrenc.attach.StatisticsType;
import com.github.mrlawrenc.attach.monitor.AbstractMonitor;
import com.github.mrlawrenc.attach.monitor.MethodInfo;
import com.github.mrlawrenc.attach.statistics.JdbcStatistics;
import com.github.mrlawrenc.attach.statistics.Statistics;
import com.github.mrlawrenc.attach.util.GlobalUtil;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;
import javassist.ClassPool;
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
    public static AbstractMonitor INSTANCE;
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
            "attach.monitor.impl.JdbcMonitor collector=attach.monitor.impl.JdbcMonitor.INSTANCE;";

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

    @Override
    public StatisticsType type() {
        return StatisticsType.JDBC;
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
                JdbcStatistics statistics = null;
                if (Arrays.asList(PROXY_CONNECTION_METHOD).contains(method.getName())) {
                    statistics = GlobalUtil.createStatistics(JdbcStatistics.class);
                    statistics.setStartTime(System.currentTimeMillis());
                }

                Object result = method.invoke(connection, args);
                if (Arrays.asList(PROXY_CONNECTION_METHOD).contains(method.getName()) && result instanceof PreparedStatement) {
                    if (Objects.nonNull(statistics)) {
                        statistics.setUrl(connection.getMetaData().getURL());
                        statistics.setSql(args[0].toString());
                    }
                    result = proxyStatement((PreparedStatement) result, statistics);
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
    private PreparedStatement proxyStatement(PreparedStatement statement, JdbcStatistics statistics) {
        return (PreparedStatement) Proxy.newProxyInstance(JdbcMonitor.class.getClassLoader()
                , new Class[]{PreparedStatement.class}, (proxy, method, args) -> {
                    Object result = method.invoke(statement, args);
                    if (Arrays.asList(STATEMENT_METHOD).contains(method.getName())) {
                        System.out.println("method name:" + method.getName());
                        statistics.setEndTime(System.currentTimeMillis());
                        if (result instanceof ResultSet) {
                            ResultSet resultSet = (ResultSet) result;
                            statistics.setResultSet(resultSet);

                            //设置行数 完毕归位结果集指针
                            resultSet.last();
                            statistics.setCount(resultSet.getRow());
                            resultSet.beforeFirst();

                        } else if (result instanceof Integer || result instanceof Long) {
                            statistics.setCount((Long) result);
                        } else if (result instanceof Boolean) {
                            statistics.setSuccess((Boolean) result);
                        }
                        log.info("preparedStatement statistics:{}", JSONUtil.toJsonStr(statistics));
                    }
                    return result;
                });
    }


    @Override
    public boolean isTarget(String className) {
        return TARGET_CLZ.equals(className.replace("/", "."));
    }

    @Override
    public CtMethod targetMethod(ClassPool pool, CtClass clz) throws NotFoundException {
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
    public Statistics begin(Object obj, Object... args) {
        Statistics statistics = GlobalUtil.createStatistics(JdbcStatistics.class);
        log.info("begin class:{} args:{}", obj.getClass(), args);
        statistics.setStartTime(System.currentTimeMillis());
        return statistics;
    }

    @Override
    public void exception(Statistics statistics, Throwable t) {
        statistics.setT(t);
    }

    @Override
    public Object end(Statistics current, Object obj) {
        Object result = obj;
        if (Objects.nonNull(obj) && obj instanceof Connection) {
            current.setOldResult(obj);
            result = proxyConnection((Connection) current.getOldResult());
            current.setNewResult(result);
        }
        current.setEndTime(System.currentTimeMillis());
        log.info("statistics:{}", JSONUtil.toJsonStr(current));
        return result;
    }


    @Override
    public WriterResp write(Writeable statistics) {
        return null;
    }
}