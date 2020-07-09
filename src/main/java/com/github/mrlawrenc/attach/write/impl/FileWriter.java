package com.github.mrlawrenc.attach.write.impl;

import com.github.mrlawrenc.attach.write.AbstractWriter;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;

import java.io.File;

/**
 * @author : MrLawrenc
 * date  2020/7/6 22:30
 * <p>
 * 文件缓存
 */
public class FileWriter extends AbstractWriter<String> {

    private static String filePath = new File("").getAbsolutePath();

    @Override
    protected void init(String s) {

    }

    @Override
    protected void destroy() {

    }

    @Override
    public WriterResp write(Writeable writeable) {
        return null;
    }
}