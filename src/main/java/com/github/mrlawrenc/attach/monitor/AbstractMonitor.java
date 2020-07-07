package com.github.mrlawrenc.attach.monitor;

import com.github.mrlawrenc.attach.write.Writer;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:02
 */
public abstract class AbstractMonitor implements Monitor, Writer {

    /**
     * 内部调用，初始化所有单例对象，子类自行实现
     */
    public abstract void init();
}