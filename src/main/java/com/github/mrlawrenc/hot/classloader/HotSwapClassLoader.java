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
 * 两种方式打破双亲委派{@link ClassLoader#loadClass(String)}
 * <pre>
 *     1. 手动设置parent属性为null
 *     2. 改变loadclass内容，使 findLoadedClass(name);方法能获取到已经加载的类，详见ClassLoader line=570
 * </pre>
 */
@Slf4j
public class HotSwapClassLoader extends ClassLoader {

    public static final String SEPARATOR = Matcher.quoteReplacement(File.separator);

    private String classPath;

    private List<String> loadClzList = new ArrayList<>();

    public HotSwapClassLoader(String classPath) {
        this.classPath = new File(classPath).getAbsolutePath();
        loadHotClass(new File(classPath));
    }

    public HotSwapClassLoader(String classPath, String fullName) throws Exception {
        this.classPath = new File(classPath).getAbsolutePath();
        File file = new File(classPath + SEPARATOR + fullName.replaceAll("\\.", SEPARATOR));
        FileInputStream inputStream = new FileInputStream(file);

        byte[] bytes = new byte[(int) file.length()];
        inputStream.read(bytes);
        defineClass(fullName, bytes, 0, bytes.length);
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
            }else if(AgentConstant.JAR_END.equals(endName)){
                //jar文件
            }else {
                LogUtil.debug("ignore file  : {}", fileName);
            }
        }
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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