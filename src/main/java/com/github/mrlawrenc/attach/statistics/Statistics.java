package com.github.mrlawrenc.attach.statistics;

import com.github.mrlawrenc.attach.monitor.Monitor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MrLawrenc
 * date  2020/7/5 19:02
 * 统计类，贯穿被植入，被监控方法的整个生命周期
 * @see Monitor
 */
@Getter
@Setter@ToString
public abstract class Statistics implements Serializable {

    /**
     * 获取当前统计类索引信息
     * <p>
     * 每一条堆栈由多个统计类构成，且多个统计类会构成一个二叉树单分支
     * <p>
     * 索引值 如0 01 011 012 0121等
     */
    protected String idx;

    public Statistics(String idx) {
        this.idx = idx;
    }

    /**
     * 方法执行者
     */
    private Object executor;

    /**
     * 方法参数
     */
    private Object[] args;

    /**
     * 方法执行产生的异常信息，若无异常则为null
     */
    private Throwable t;
    /**
     * 方法本身返回的对象，当方法无返回结果时，该值为null
     */
    private Object oldResult;
    /**
     * 方法实际返回的对象，该方法本身返回对象可能被代理或者更改，若无更改则和{@link Statistics#oldResult}相等
     */
    private Object newResult;


    /**
     * 方法执行开始时间
     */
    private long startTime;
    /**
     * 方法执行结束时间
     */
    private long endTime;

}