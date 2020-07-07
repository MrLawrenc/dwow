package com.github.mrlawrenc.attach.write.impl;

import com.github.mrlawrenc.attach.statistics.Statistics;
import com.github.mrlawrenc.attach.write.AbstractWriter;
import com.github.mrlawrenc.attach.write.WriterResp;

/**
 * @author : MrLawrenc
 * date  2020/7/6 22:30
 */
public class RedisWriter  extends AbstractWriter<String> {
    @Override
    protected void init(String s) {

    }

    @Override
    protected void destroy() {

    }

    @Override
    public WriterResp write(Statistics statistics) {
        return null;
    }
}