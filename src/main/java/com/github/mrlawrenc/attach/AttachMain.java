package com.github.mrlawrenc.attach;

import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;

/**
 * @author hz20035009-逍遥
 * date   2020/6/10 14:58
 * <p>
 * attach注入的入口
 */
@Slf4j
public class AttachMain {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("######################################################################");
        System.out.println("######################################################################");
        System.out.println("#######                     Attach Success                     #######");
        System.out.println("######################################################################");
        System.out.println("######################################################################");


    }
}