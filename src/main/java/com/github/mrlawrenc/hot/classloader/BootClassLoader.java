package com.github.mrlawrenc.hot.classloader;

import java.io.File;
import java.util.regex.Matcher;

/**
 * @author hz20035009-逍遥
 * date   2020/6/8 17:21
 */
public interface BootClassLoader {
    /**
     * 文件分隔符
     */
    String SEPARATOR = Matcher.quoteReplacement(File.separator);

     String CLASS_FLAG = "class";
    String JAR_FLAG = "class";
}
