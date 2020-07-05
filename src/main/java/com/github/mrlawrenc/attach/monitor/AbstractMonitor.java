package com.github.mrlawrenc.attach.monitor;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:02
 */
public abstract class AbstractMonitor implements Monitor {
    public static AbstractMonitor INSTANCE;


    /**
     * 时间开始逻辑
     */
    protected String dateStart = "";
    /**
     * 时间结束逻辑
     */
    protected String dateEnd = "";
    /**
     * 异常逻辑
     */
    protected String throwException = "";



    /**
     * 内部调用，初始化所有单例对象，子类自行实现
     */
    public abstract void init();
}