package com.github.mrlawrenc.attach.monitor;

import lombok.Data;

import java.lang.reflect.Modifier;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:27
 * <p>
 * doc:https://www.cnblogs.com/scy251147/p/11100961.html
 */
@Data
public class MethodInfo {
    private boolean isNewInfo;

    private String newBody;


    private String oldName;
    private String beginSrc;
    private String endSrc;
    private String catchSrc;
    private String tryBody;
    private String catchBody;
    private String finallyBody;
    /**
     * 手动添加try catch finally的源资源
     */
    private static final String SOURCE = "{\n"
            + "%s"
            + "Object c=($w)$0.%s($$);"
            + "%s"
            + "return ($r)result;"
            + "\n}";
    /**
     * 原生包含try catch finally的源资源
     */
    private static final String NEW_SOURCE = "{\n" +
            "%s" +
            "Object result;" +
            "try{" +
            "   result=($w)$0.%s($$);" +
            "}catch(java.lang.Throwable t){" +
            "   %s" +
            "   throw t;" +
            "}finally{" +
            "  %s" +
            "}" +
            "   return ($r) result;" +
            "}";
    private static final String NEW_SOURCE0 = "{\n" +
            "%s\n" +
            "Object result;\n" +
            "try{\n" +
            "   result=($w)$0.%s($$);\n" +
            "}catch(java.lang.Throwable t){\n" +
            "   %s\n" +
            "   throw t;\n" +
            "}finally{\n" +
            "  %s\n" +
            "}\n" +
            "   return ($r) result;\n" +
            "}";

    public static MethodBuilder newBuilder() {
        return new MethodBuilder();
    }


    public static class MethodBuilder {
        MethodInfo methodInfo = new MethodInfo();

        /**
         * 以{@link MethodInfo#NEW_SOURCE}来构建
         */
        public MethodBuilder newBody(String begin, String oldName, String end, String exception) {
            methodInfo.setOldName(oldName);
            methodInfo.setEndSrc(end);
            methodInfo.setBeginSrc(begin);
            return this;
        }

        public MethodInfo createNewBody(String begin, String catchSrc, String end, String oldName) {
            methodInfo.setNewInfo(true);
            String body = String.format(NEW_SOURCE, begin, oldName, catchSrc, end);
            methodInfo.setNewBody(body);
            return methodInfo;
        }

        /**
         * <pre>
         *  {
         * com.github.mrlawrenc.attach.service.JdbcCollector monitor =  new com.github.mrlawrenc.attach.service.JdbcCollector();
         * com.github.mrlawrenc.attach.service.Monitor$Statistics statistic = monitor.begin($0,$$);
         * Object result=null;
         * try{
         *    result=($w)$0.方法名($$);
         * }catch(java.lang.Throwable t){
         *    monitor.exception(statistic,t);
         *    throw t;
         * }finally{
         *   monitor.end(statistic);
         * }
         *    return ($r) result;
         * }
         * </pre>
         *
         * @param monitor       具体的监控实现类
         * @param oldMethodName 方法名
         * @return 构造好的方法体
         */
        public MethodInfo createBody(Monitor monitor, String oldMethodName) {
            int modifiers = monitor.getClass().getModifiers();
            if (Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers)) {
                throw new IllegalStateException("The current class cannot be interface and abstract!");
            }
            String clzName = monitor.getClass().getName();
            String begin = clzName + " monitor = " + clzName + ".INSTANCE;\n";
            String statisticName = DefaultStatistics.class.getName();
            begin += statisticName + " statistic = monitor.begin($0,$args);";

            String exception = "monitor.exception(statistic,t);";

            String end = "result = monitor.end(statistic,(Object)result);";
            String body = String.format(NEW_SOURCE0, begin, oldMethodName, exception, end);

            System.out.println(body);
            methodInfo.setNewBody(body);
            methodInfo.setNewInfo(true);
            return methodInfo;
        }

        public MethodBuilder tryBody(String begin, String oldName, String end) {
            methodInfo.setOldName(oldName);
            methodInfo.setEndSrc(end);
            methodInfo.setBeginSrc(begin);
            return this;
        }


        public MethodBuilder catchBody(String catchBody) {
            methodInfo.setCatchBody(catchBody);
            return this;
        }

        public MethodBuilder finallyBody(String finallyBody) {
            methodInfo.setFinallyBody(finallyBody);
            return this;
        }


        public MethodInfo create() {
            String tryBody = String.format(SOURCE, methodInfo.getBeginSrc(), methodInfo.getOldName(), methodInfo.getEndSrc());
            methodInfo.setTryBody(tryBody);
            return methodInfo;
        }
    }
}