package com.github.mrlawrenc.hot.classloader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : MrLawrenc
 * date  2020/6/7 10:40
 * <p>
 * 专门加载.class文件的calssloader
 */
@Slf4j
public class HotSwapClassClassLoader extends ClassLoader {


    @Getter
    @Setter
    private Map<String, byte[]> byteMap = new HashMap<String, byte[]>();

    public HotSwapClassClassLoader() {
    }


    public Class<?> defineClass0(String name, byte[] b, int off, int len) {
        Class<?> defineClass = defineClass(name, b, off, len);
        byteMap.put(name, b);
        return defineClass;
    }


    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            //交由App加载
            result = getSystemClassLoader().loadClass(name);
        }

        return result;
    }

    /**
     * 更新已经加载的类到新的类加载器
     *
     * @param name 类名
     */
    public void updateClass(String name) {
        byte[] bytes = byteMap.get(name);
        defineClass0(name, bytes, 0, bytes.length);
    }
}