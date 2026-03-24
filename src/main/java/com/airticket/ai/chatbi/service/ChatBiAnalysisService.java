package com.airticket.ai.chatbi.service;

import com.airticket.ai.chatbi.agent.AnalysisAgent;
import com.airticket.ai.chatbi.dto.ChatBiQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ChatBiAnalysisService {

    private static final Pattern MARKDOWN_SQL_PREFIX = Pattern.compile("(?is)^```sql\\s*");
    private static final Pattern MARKDOWN_SUFFIX = Pattern.compile("(?is)```$");
    private static final Pattern TRAILING_LIMIT = Pattern.compile("(?is)\\s+LIMIT\\s+\\d+(\\s*,\\s*\\d+)?\\s*$");

    private final AnalysisAgent analysisAgent;
    private final SqlSchemaProvider sqlSchemaProvider;
    private final SqlExecutorService sqlExecutorService;
    private final ChatBiResultFormatter chatBiResultFormatter;
    private final ObjectMapper objectMapper;

    public ChatBiAnalysisService(AnalysisAgent analysisAgent,
                                 SqlSchemaProvider sqlSchemaProvider,
                                 SqlExecutorService sqlExecutorService,
                                 ChatBiResultFormatter chatBiResultFormatter,
                                 ObjectMapper objectMapper) {
        this.analysisAgent = analysisAgent;
        this.sqlSchemaProvider = sqlSchemaProvider;
        this.sqlExecutorService = sqlExecutorService;
        this.chatBiResultFormatter = chatBiResultFormatter;
        this.objectMapper = objectMapper;
    }

    public ChatBiQueryResponse analyze(String question) {
        String schema = sqlSchemaProvider.getSchema();
        String originalSql = normalizeSql(analysisAgent.generateSql(schema, question));

        try {
            List<Map<String, Object>> rows = sqlExecutorService.executeQuery(originalSql);
            if (shouldRetryForEmptyRows(question, originalSql, rows)) {
                String repairedSql = normalizeSql(
                    analysisAgent.repairSql(
                        schema,
                        originalSql,
                        """
                            SQL executed successfully but returned 0 rows.
                            In this system, ticket revenue queries should prefer aggregating paid tickets.price.
                            Only use payments when the user explicitly asks about payment method/channel or payment records.
                            """.trim(),
                        question
                    )
                );
                List<Map<String, Object>> repairedRows = sqlExecutorService.executeQuery(repairedSql);
                return buildResponse(question, repairedSql, originalSql, repairedRows, true);
            }
            return buildResponse(question, originalSql, null, rows, false);
        } catch (Exception firstError) {
            String repairedSql = normalizeSql(
                analysisAgent.repairSql(schema, originalSql, safeErrorMessage(firstError), question)
            );

            try {
                List<Map<String, Object>> rows = sqlExecutorService.executeQuery(repairedSql);
                return buildResponse(question, repairedSql, originalSql, rows, true);
            } catch (Exception secondError) {
                throw new IllegalStateException(
                    "SQL 执行失败，自动修正后仍未成功: " + safeErrorMessage(secondError),
                    secondError
                );
            }
        }
    }

    private ChatBiQueryResponse buildResponse(String question,
                                              String sql,
                                              String originalSql,
                                              List<Map<String, Object>> rows,
                                              boolean repaired) {
        List<Map<String, Object>> formattedRows = chatBiResultFormatter.formatRows(rows);
        String chartType = inferChartType(question, formattedRows);
        return new ChatBiQueryResponse(
            question,
            sql,
            originalSql,
            chartType,
            formattedRows,
            formattedRows.size(),
            repaired
        );
    }

    private String inferChartType(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "TABLE";
        }

        try {
            String chartPrompt = """
                用户问题：
                %s

                返回列：
                %s

                数据样例：
                %s
                """.formatted(
                question,
                rows.get(0).keySet(),
                objectMapper.writeValueAsString(rows.stream().limit(10).toList())
            );
            return normalizeChartType(analysisAgent.suggestChartType(chartPrompt), rows);
        } catch (JsonProcessingException ex) {
            return fallbackChartType(rows);
        }
    }

    private String normalizeChartType(String rawChartType, List<Map<String, Object>> rows) {
        if (rawChartType == null) {
            return fallbackChartType(rows);
        }

        String normalized = rawChartType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BAR", "LINE", "PIE", "TABLE" -> normalized;
            default -> fallbackChartType(rows);
        };
    }

    private String fallbackChartType(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "TABLE";
        }

        Map<String, Object> firstRow = rows.get(0);
        if (firstRow.size() < 2) {
            return "TABLE";
        }

        boolean containsTemporalColumn = firstRow.keySet().stream()
            .filter(Objects::nonNull)
            .map(key -> key.toLowerCase(Locale.ROOT))
            .anyMatch(key -> key.contains("date") || key.contains("time") || key.contains("month") || key.contains("day"));

        if (containsTemporalColumn) {
            return "LINE";
        }

        if (rows.size() <= 8) {
            return "PIE";
        }

        return "BAR";
    }

    private String normalizeSql(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new IllegalArgumentException("LLM 未返回有效 SQL");
        }

        String cleaned = MARKDOWN_SQL_PREFIX.matcher(rawSql.trim()).replaceFirst("");
        cleaned = MARKDOWN_SUFFIX.matcher(cleaned).replaceFirst("");
        cleaned = cleaned.trim().replaceAll(";+$", "").trim();

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("LLM 未返回有效 SQL");
        }

        String upperSql = cleaned.toUpperCase(Locale.ROOT);
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            throw new IllegalArgumentException("LLM 返回了非查询 SQL");
        }

        if (cleaned.contains(";")) {
            throw new IllegalArgumentException("检测到多语句 SQL");
        }

        cleaned = TRAILING_LIMIT.matcher(cleaned).replaceFirst("");
        return cleaned + " LIMIT 100";
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return (message == null || message.isBlank()) ? exception.getClass().getSimpleName() : message;
    }

    private boolean shouldRetryForEmptyRows(String question, String sql, List<Map<String, Object>> rows) {
        if (rows == null || !rows.isEmpty()) {
            return false;
        }

        String normalizedQuestion = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String normalizedSql = sql == null ? "" : sql.toLowerCase(Locale.ROOT);

        boolean revenueIntent = normalizedQuestion.contains("收入")
            || normalizedQuestion.contains("营收")
            || normalizedQuestion.contains("营业额")
            || normalizedQuestion.contains("销售额")
            || normalizedQuestion.contains("排行");

        boolean dependsOnPayments = normalizedSql.contains(" payments ")
            || normalizedSql.contains("payments ")
            || normalizedSql.contains("payments.");

        return revenueIntent && dependsOnPayments;
    }
}
