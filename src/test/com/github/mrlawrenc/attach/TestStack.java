package com.github.mrlawrenc.attach;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author : MrLawrenc
 * date  2020/7/6 21:01
 * <p>
 * 测试线程堆栈信息获取，包括线程池线程复用的情况
 */
public class TestStack {

    public static void main(String[] args) throws Exception {
        //普通堆栈获取
        new TestStack().t1();
        System.out.println("============simple===============");
        //线程池线程复用情况下，堆栈是否会循环
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> submit1 = service.submit(() -> new TestStack().t1());
        submit1.get();
        System.out.println("==============one=============");
        Future<?> submit2 = service.submit(() -> new TestStack().t1());
        submit2.get();
        System.out.println("==============end=============");
        Future<?> submit3 = service.submit(() -> new TestStack().t1());
        submit3.get();
        System.out.println("#################################################");
        testMethod(new SecurityManagerMethod());
    }


    public void t1() {
        t2();
    }

    public void t2() {
        t3();
    }

    public void t3() {
        t4();
    }

    public void t4() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            System.out.println(stackTraceElement);
        }
    }


    private static void testMethod(GetCallerClassNameMethod method) {
        long startTime = System.nanoTime();
        String className = null;
        for (int i = 0; i < 1000000; i++) {
            className = method.getCallerClassName(2);
        }
        printElapsedTime(method.getMethodName(), startTime);
    }

    private static void printElapsedTime(String title, long startTime) {
        System.out.println(title + ": " + ((double) (System.nanoTime() - startTime)) / 1000000 + " ms.");
    }

    static abstract class GetCallerClassNameMethod {
        public abstract String getCallerClassName(int callStackDepth);

        public abstract String getMethodName();
    }

    static class SecurityManagerMethod extends GetCallerClassNameMethod {
        public String getCallerClassName(int callStackDepth) {
            return mySecurityManager.getCallerClassName(callStackDepth);
        }

        public String getMethodName() {
            return "SecurityManager";
        }

        /**
         * A custom security manager that exposes the getClassContext() information
         */
        static class MySecurityManager extends SecurityManager {
            public String getCallerClassName(int callStackDepth) {
                System.out.println("======1=====");
                for (int i = 0; i < getClassContext().length; i++) {
                    System.out.println(getClassContext()[i].getName());
                }
                System.out.println("=======2====");
                return getClassContext()[callStackDepth].getName();
            }
        }

        private final static MySecurityManager mySecurityManager =
                new MySecurityManager();
    }


}