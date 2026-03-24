package com.airticket.ai.chatbi.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SqlExecutorService {

    private static final Pattern BLOCKED_KEYWORDS =
        Pattern.compile("\\b(DROP|DELETE|UPDATE|INSERT|TRUNCATE)\\b", Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;

    public SqlExecutorService(@Qualifier("chatBiJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        validateSql(sql);
        return jdbcTemplate.queryForList(sql);
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        String normalized = sql.trim();
        String upperSql = normalized.toUpperCase(Locale.ROOT);

        if (BLOCKED_KEYWORDS.matcher(upperSql).find()) {
            throw new IllegalArgumentException("检测到危险关键字，拒绝执行 SQL");
        }

        if (normalized.contains(";")) {
            throw new IllegalArgumentException("不允许执行多语句 SQL");
        }

        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            throw new IllegalArgumentException("只允许执行只读查询 SQL");
        }
    }
}
