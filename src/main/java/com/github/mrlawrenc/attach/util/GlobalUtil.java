package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.Writer;
import com.github.mrlawrenc.attach.write.impl.FileWriter;

import java.util.List;
import java.util.Objects;

/**
 * @author : MrLawrenc
 * date  2020/7/11 17:49
 */
public class GlobalUtil {

    private static final Writer WRITER;

    static {
        WRITER = new FileWriter();
        Runtime.getRuntime().addShutdownHook(new Thread(WRITER::destroy));
    }

    /**
     * 输出数据
     *
     * @param writeable w
     */
    public static void write(Writeable writeable) {
        WRITER.write(writeable);
    }

    public static void printNodeTreeByParent(StackNode.Node parentNode) {
        printNodeTree(parentNode, "");
    }

    private static void printNodeTree(StackNode.Node parentNode, String str) {
        System.out.println(str + parentNode.getStackInfo());
        List<StackNode.Node> child = parentNode.getChild();
        if (Objects.nonNull(child) && child.size() > 0) {
            child.forEach(c -> printNodeTree(c, str + "\t"));
        }
    }


}