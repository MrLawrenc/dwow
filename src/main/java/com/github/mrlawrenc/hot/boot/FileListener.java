package com.github.mrlawrenc.hot.boot;


import com.github.mrlawrenc.hot.classloader.ContextClassLoader;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;

/**
 * @author : MrLawrenc
 * date  2020/6/7 15:23
 * <p>
 * 文件监听
 * <p>
 * 使用nio的监听和自己的线程轮训也可以实现
 * @see java.nio.file.WatchService
 */
public class FileListener extends FileAlterationListenerAdaptor {
    private ContextClassLoader loader;

    public FileListener(ContextClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void onFileChange(File file) {
        try {
            System.out.println("file change : " + file.getAbsolutePath());


            loader.getBoot().reloadClz(loader,file);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        // super.onFileChange(file);
    }
}