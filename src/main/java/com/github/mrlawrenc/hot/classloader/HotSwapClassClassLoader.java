package com.github.mrlawrenc.hot.classloader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : MrLawrenc
 * date  2020/6/7 10:40
 * <p>
 * 专门加载.class文件的calssloader
 */
@Slf4j
public class HotSwapClassClassLoader extends ClassLoader {


    /**
     * 当前classloader被更新时才会被赋值
     */
    private HotSwapClassClassLoader newLoader = null;

    private Lock monitor = new ReentrantLock();

    /**
     * 标志位，标记当前classloader是否改变
     * <p>
     * 防止和类全限定名重复，包名不能用数字开头，因此使用0.开头
     */
    private static final String FLAG = "0.flag";

    /**
     * key为class的全限定名
     * <p>
     * 当key为{@link HotSwapClassClassLoader#FLAG}时，value作为标志位，为null则当前classloader未改变
     */
    @Getter
    @Setter
    private Map<String, byte[]> byteMap = new HashMap<String, byte[]>();

    public HotSwapClassClassLoader() {
        //存放当前loader对象是否改变的标志位
        byteMap.put(FLAG, null);
    }

    /**
     * 改变当前class loader标志位
     */
    public void changeClassLoader(HotSwapClassClassLoader newLoader) {
        monitor.lock();
        try {
            byteMap.put(FLAG, new byte[]{});
            //不复制map，在使用新的标志位之前会重新更正标志位的值
            newLoader.setByteMap(this.getByteMap());
            this.newLoader = newLoader;
        } finally {
            monitor.unlock();
        }
    }


    public Class<?> defineClass0(String name, byte[] b, int off, int len) throws ClassNotFoundException {
        Class<?> defineClass;
        monitor.lock();
        try {
            defineClass = defineClass(name, b, off, len);
            byteMap.put(name, b);
        } catch (Exception e) {
            defineClass = getSystemClassLoader().loadClass(name);
        } finally {
            monitor.unlock();
        }
        return defineClass;
    }


    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result;
        monitor.lock();
        try {
            System.out.println("class 文件的loader ###" + name + "### loadClass()  this loader " + this + "  new loader " + newLoader);
            result = findLoadedClass(name);
        } finally {
            monitor.unlock();
        }
        if (result == null) {
            //交由App加载
            result = getSystemClassLoader().loadClass(name);
        }

        return result;
    }

}