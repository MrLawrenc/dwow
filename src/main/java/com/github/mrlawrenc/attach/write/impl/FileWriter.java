package com.github.mrlawrenc.attach.write.impl;

import com.github.mrlawrenc.attach.write.AbstractWriter;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author : MrLawrenc
 * date  2020/7/6 22:30
 * <p>
 * 文件缓存
 */
@Slf4j
public class FileWriter extends AbstractWriter<String> {

    private static final String PROJECT_PATH = new File("").getAbsolutePath() + File.separator;


    private static final RandomAccessFile STACK_FILE;
    private static final RandomAccessFile STATISTICS_FILE;

    private long stackOff = 0;

    static {
        try {
            STACK_FILE = new RandomAccessFile(new File(PROJECT_PATH + "stackInfo.txt"), "rw");
            STATISTICS_FILE = new RandomAccessFile(new File(PROJECT_PATH + "statisticsFileInfo.txt"), "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("init target file fail");
        }
    }

    @Override
    protected void init(String s) {

    }

    @Override
    public void destroy() {
        try {
            STACK_FILE.close();
            STATISTICS_FILE.close();
        } catch (IOException e) {
            log.error("close file( fail", e);
        }
    }

    @Override
    public synchronized WriterResp write(Writeable writeable) {
      /*  if (writeable instanceof StackNode) {
            StackNode source = (StackNode) writeable;
            StackTraceElement[] stackTraceElements = source.getCurrentStackElements();

            StringBuilder result = new StringBuilder();
            String threadName = Thread.currentThread().getName();
            try {
                if (stackOff == 0) {
                    //排除addNode和getStackTrace的堆栈信息
                    for (int i = 2; i < stackTraceElements.length; i++) {
                        result.append(LocalDateTime.now()).append(" ").append(threadName).append(" ");
                        StackTraceElement element = stackTraceElements[i];
                        result.append(element.getLineNumber()).append(" ").append(element.getClassName()).append("#").append(element.getMethodName()).append("  ").append(element.getFileName()).append("\n");
                    }
                    STACK_FILE.seek(stackOff);
                    STACK_FILE.writeBytes(result.toString());
                    stackOff += result.length();
                } else {
                    long currentOff = stackOff;

                    STACK_FILE.seek(0);
                    String firstLine = STACK_FILE.readLine();
                    String[] split = firstLine.split("#");
                    int startIdx = split[0].lastIndexOf(" ");
                    int endIdx = firstLine.indexOf(" ");
                    String fullName = firstLine.substring(startIdx, endIdx);
                    System.out.println("fullName:" + fullName);

                    STACK_FILE.seek(currentOff);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        return null;
    }

}