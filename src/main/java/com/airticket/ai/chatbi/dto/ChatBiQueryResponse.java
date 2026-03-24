package com.airticket.ai.chatbi.dto;

import java.util.List;
import java.util.Map;

public record ChatBiQueryResponse(
    String question,
    String sql,
    String originalSql,
    String chartType,
    List<Map<String, Object>> rows,
    int rowCount,
    boolean repaired
) {
}
