package com.github.mrlawrenc.attach.write;

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
     * @param writeable 统计具体实现类
     * @return 输出结果
     */
    WriterResp write(Writeable writeable);

    /**
     * 销毁方法
     */
    void destroy();
}