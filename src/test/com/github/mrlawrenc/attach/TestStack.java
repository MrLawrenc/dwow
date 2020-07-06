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
}