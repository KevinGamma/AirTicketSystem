package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        try {
            Map<String, Object> stats = statisticsService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success("获取统计数据成功", stats));
        } catch (Exception e) {
            System.err.println("Error in getDashboardStats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFlightStatistics(@PathVariable Long flightId) {
        Map<String, Object> stats = statisticsService.getFlightStatistics(flightId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/daily-flights")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyFlightVolume(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<Map<String, Object>> data = statisticsService.getDailyFlightVolume(days);
            return ResponseEntity.ok(ApiResponse.success("获取每日航班量成功", data));
        } catch (Exception e) {
            System.err.println("Error in getDailyFlightVolume: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取每日航班量失败: " + e.getMessage()));
        }
    }

    @GetMapping("/airline-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAirlineDistribution() {
        try {
            System.out.println("StatisticsController: Received request for airline distribution");
            List<Map<String, Object>> data = statisticsService.getAirlineFlightDistribution();
            System.out.println("StatisticsController: Airline distribution data size: " + (data != null ? data.size() : "null"));
            return ResponseEntity.ok(ApiResponse.success("获取航空公司分布成功", data));
        } catch (Exception e) {
            System.err.println("Error in getAirlineDistribution: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取航空公司分布失败: " + e.getMessage()));
        }
    }

    @GetMapping("/booking-trends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookingTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<Map<String, Object>> data = statisticsService.getBookingTrends(days);
            return ResponseEntity.ok(ApiResponse.success("获取预订趋势成功", data));
        } catch (Exception e) {
            System.err.println("Error in getBookingTrends: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取预订趋势失败: " + e.getMessage()));
        }
    }

    @GetMapping("/revenue-analysis")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueAnalysis(
            @RequestParam(defaultValue = "12") int months) {
        try {
            List<Map<String, Object>> data = statisticsService.getRevenueAnalysis(months);
            return ResponseEntity.ok(ApiResponse.success("获取收入分析成功", data));
        } catch (Exception e) {
            System.err.println("Error in getRevenueAnalysis: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取收入分析失败: " + e.getMessage()));
        }
    }

    


    @GetMapping("/popular-routes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPopularRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> data = statisticsService.getPopularRoutes(limit);
            return ResponseEntity.ok(ApiResponse.success("获取热门航线成功", data));
        } catch (Exception e) {
            System.err.println("Error in getPopularRoutes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取热门航线失败: " + e.getMessage()));
        }
    }
}