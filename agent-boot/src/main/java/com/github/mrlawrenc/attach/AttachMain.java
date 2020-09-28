package com.github.mrlawrenc.attach;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Objects;

/**
 * @author hz20035009-逍遥
 * date   2020/6/10 14:58
 * <p>
 * attach注入的入口
 * 该类是有AppclassLoader加载的，需要借助{@link OnionClassLoader}加载器实现其余类加载的隔离。具体原因如下:
 * 为什么要实现隔离？
 * 隔离是避免Agent污染应用自身，使开发Java Agent无需考虑引入的jar包是否与目标应用引入的jar包冲突。
 * <p>
 * Java Agent与Spring Boot应用相遇时会发生什么？
 * Spring Boot应用打包后，将Agent附着到应用启动可能会抛出醒目的NoClassDefFoundError异常，这在IDEA中测试是不会发生的，而背后的原因是Agent与打包后的Spring Boot应用使用了不同的类加载器。
 * 我们可能会在Agent中调用被监控的SpringBoot应用的代码，也可能调用Agent依赖的第三方jar包的API，而这些jar包恰好在SpringBoot应用中也有导入，就可能会出现NoClassDefFoundError。
 * Agent的jar包由AppClassLoader类加载器（系统类加载器）所加载。
 * 在IDEA中，项目的class文件和第三方库是通过AppClassLoader加载的，而使用-javaagent指定的jar也是通过AppClassLoader加载，所以在idea中测试不会遇到这个问题。
 * SpringBoot应用打包后，JVM进程启动入口不再是我们写的main方法，而是SpringBoot生成的启动类。SpringBoot使用自定义的类加载器（LaunchedClassLoader）加载jar中的类和第三方jar包中的类，该类加载器的父类加载器为AppClassLoader。
 * 也就是说，SpringBoot应用打包后，加载javaagent包下的类使用的类加载器是SpringBoot使用的类加载器的父类加载器。
 * <p>
 * 如何实现隔离？
 * 让加载agent包不使用AppClassLoader加载器加载，而是使用自定义的类加载器加载。
 * 参考Alibaba开源的Arthas的实现，自定义URLClassLoader加载agent包以及agent依赖的第三方jar包。
 * 由于premain或者agentmain方法所在的类由jvm使用AppClassLoader所加载，所以必须将agent拆分为两个jar包。核心功能放在agent-core包下，premain或者agentmain方法所在的类放在agent-boot包下。在premain或者agentmain方法中使用自定义的URLClassLoader类加载器加载agent-core。
 */
public class AttachMain {

    public static void premain(String agentOps, Instrumentation inst) {
        // 0 首先加载第三方依赖包


        try {
            // 0 获取core包位置
            String userHome = System.getProperty("user.home");
            File[] files = new File(userHome).listFiles((dir, name) -> name.startsWith("agent-core"));
            if (Objects.isNull(files) || files.length != 1) {
                return;
            }
            System.out.println("load core:" + files[0].getName());
            // 1
            File agentJarFile = files[0];
            final ClassLoader agentLoader = new OnionClassLoader(new URL[]{agentJarFile.toURI().toURL()});
            // 2
            Class<?> transFormer = agentLoader.loadClass("com.github.mrlawrenc.attach.TransformerService");
            // 3
            Constructor<?> constructor = transFormer.getConstructor(ClassLoader.class);
            Object instance = constructor.newInstance(agentLoader);
            /*// 4 参考Arthas 将需要注入的代码单独放在一个jar包，交由启动类加载器加载，这样在被插桩的代码里面就能找到该类
            inst.appendToBootstrapClassLoaderSearch(new JarFile(new File("a")));*/

            // 5
            inst.addTransformer((ClassFileTransformer) instance, true);


          /*  String userHome = System.getProperty("user.home");
            File[] files = new File(userHome).listFiles((dir, name) -> name.startsWith("agent-external-lib"));
            if (Objects.isNull(files) || files.length != 1) {
                return;
            }
            System.out.println("load core:" + files[0].getName());
            File agentJarFile = files[0];
            final ClassLoader agentLoader = new OnionClassLoader(new URL[]{agentJarFile.toURI().toURL()});
            // 2
            Class<?> transFormer = agentLoader.loadClass("com.github.mrlawrenc.attach.TransformerService");*/


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
    }

}