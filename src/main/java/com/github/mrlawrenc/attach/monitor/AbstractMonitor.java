package com.github.mrlawrenc.attach.monitor;

import com.github.mrlawrenc.attach.StatisticsType;
import com.github.mrlawrenc.attach.write.impl.FileWriter;

/**
 * @author : MrLawrenc
 * date  2020/7/4 19:02
 */
public abstract class AbstractMonitor extends FileWriter implements Monitor {

    /**
     * 内部调用，初始化所有单例对象，子类自行实现
     */
    public abstract void init();

    /**
     * 统计类归属类型
     *
     * @return 类型
     */
    public abstract StatisticsType type();
}