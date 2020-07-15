package com.github.mrlawrenc.attach.statistics;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.sql.ResultSet;

/**
 * @author : MrLawrenc
 * date  2020/7/5 19:10
 * <p>
 * Jdbc收集器
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(chain = true)
public class JdbcStatistics extends Statistics {
    private JdbcStatistics(String id) {
        super(id);
    }

    /**
     * 连接信息
     */
    private String url;

    /**
     * 执行的sql语句 可能是预编译语句
     */
    private String sql;
    /**
     * sql查询结果
     */
    private ResultSet resultSet;

    /**
     * sql insert update delete 影响的行数
     */
    private long count;
    /**
     * sql是否执行成功
     */
    private boolean success;

}