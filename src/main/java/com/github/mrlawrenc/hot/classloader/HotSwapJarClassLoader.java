package com.github.mrlawrenc.hot.classloader;

import com.github.mrlawrenc.agentutils.AgentConstant;
import com.github.mrlawrenc.agentutils.LogUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author : MrLawrenc
 * date  2020/6/7 10:40
 * <p>
 * 专门加载jar文件的calssloader
 */
@Slf4j
public class HotSwapJarClassLoader extends ClassLoader {

    public static final String SEPARATOR = Matcher.quoteReplacement(File.separator);

    private String classPath;

    private List<String> loadClzList = new ArrayList<>();

    public HotSwapJarClassLoader() {
    }

    public HotSwapJarClassLoader(String classPath, String fullName) throws Exception {
    }


    public Class<?> defineClass0(String name, byte[] b, int off, int len) {
        return defineClass(name, b, off, len);
    }

    private void loadHotClass(File file) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (null != listFiles) {
                for (File currentFile : listFiles) {
                    loadHotClass(currentFile);
                }
            }
        } else {
            String fileName = file.getName();

            if (!fileName.contains(AgentConstant.PKG_SEPARATOR)) {
                LogUtil.debug("ignore file  : {}", fileName);
                return;
            }

            String endName = fileName.substring(fileName.lastIndexOf("."));


            if (AgentConstant.CLASS_END.equals(endName)) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] bytes = new byte[(int) file.length()];
                    inputStream.read(bytes);


                    String temp = file.getAbsolutePath().replace(classPath, "")
                            .replaceAll(Matcher.quoteReplacement(File.separator), ".");
                    if (temp.startsWith(".")) {
                        temp = temp.substring(1);
                    }
                    String className = temp.substring(0, temp.lastIndexOf("."));
                    LogUtil.debug("load class : {}", className);
                    //加载进jvm虚拟机
                    defineClass(className, bytes, 0, bytes.length);
                    loadClzList.add(className);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (AgentConstant.JAR_END.equals(endName)) {
                //jar文件
            } else {
                LogUtil.debug("ignore file  : {}", fileName);
            }
        }
    }


    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            if (loadClzList.contains(name)) {
                throw new ClassNotFoundException(String.format("current clz(%s) has been loaded", name));
            } else {
                //交由App加载
                result = getSystemClassLoader().loadClass(name);
            }
        }

        return result;
    }
}