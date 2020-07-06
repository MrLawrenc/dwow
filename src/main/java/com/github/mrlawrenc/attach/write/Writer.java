package com.github.mrlawrenc.attach.write;

import com.github.mrlawrenc.attach.monitor.Statistics;

/**
 * @author : MrLawrenc
 * date  2020/7/6 21:31
 * <p>
 * 统计信息对外输出接口
 */
public interface Writer {

    /**
     * 统计信息对外输出
     *
     * @param statistics 统计具体实现类
     * @return 输出结果
     */
    WriterResp write(Statistics statistics);
}