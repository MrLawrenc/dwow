package com.github.mrlawrenc.attach.statistics;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author : MrLawrenc
 * date  2020/7/7 14:10
 * <p>
 * Servlet收集器
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString(callSuper = true)
public class ServletStatistics extends Statistics {

    public ServletStatistics(String idx) {
        super(idx);
    }

    /**
     * 请求的url地址
     */
    private String url;
    /**
     * 请求方式，如post get
     */
    private String method;

    private String reqParam;

    public enum ReqType {
        POST(), GET();
    }
}