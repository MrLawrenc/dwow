package com.github.mrlawrenc.attach;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author hz20035009-逍遥
 * date   2020/9/27 14:14
 * <p>
 * 该类加载器用于加载 agnet-core中类，实现隔离
 */
public class OnionClassLoader extends URLClassLoader {

    public OnionClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }
        // 优先从parent（SystemClassLoader）里加载系统类，避免抛出ClassNotFoundException
        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }
        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.loadClass(name, resolve);
    }

}