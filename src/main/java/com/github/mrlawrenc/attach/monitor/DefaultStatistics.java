package com.github.mrlawrenc.attach.monitor;

import lombok.Data;

/**
 * @author : MrLawrenc
 * date  2020/7/5 19:10
 * <p>
 * 默认收集器
 */
@Data
public class DefaultStatistics implements Statistics {
    private Throwable t;
    private Object oldResult;
    /**
     * 被代理之后的结果对象（条件出现）
     */
    private Object newResult;


    private long start;
    private long end;

}