package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.service.ResetService;
import com.airticket.config.DataInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reset")
@PreAuthorize("hasRole('ADMIN')")
public class ResetController {

    @Autowired
    private ResetService resetService;
    
    @Autowired
    private DataInitializer dataInitializer;

    /**
     * 获取重置统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ResetService.ResetStatistics>> getResetStatistics() {
        try {
            ResetService.ResetStatistics statistics = resetService.getResetStatistics();
            return ResponseEntity.ok(ApiResponse.success("获取重置统计信息成功", statistics));
        } catch (Exception e) {
            System.err.println("Error getting reset statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取重置统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 重置整个数据库
     */
    @PostMapping("/database")
    public ResponseEntity<ApiResponse<String>> resetEntireDatabase() {
        try {
            resetService.resetEntireDatabase();
            
            // 重新初始化基础数据
            reinitializeBaseData();
            
            return ResponseEntity.ok(ApiResponse.success("数据库重置成功，基础数据已重新初始化", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("数据库重置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置用户数据
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<String>> resetUserData() {
        try {
            resetService.resetUserData();
            return ResponseEntity.ok(ApiResponse.success("用户数据重置成功", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("用户数据重置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置机票数据
     */
    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<String>> resetTicketData() {
        try {
            resetService.resetTicketData();
            return ResponseEntity.ok(ApiResponse.success("机票数据重置成功", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("机票数据重置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置航班数据
     */
    @PostMapping("/flights")
    public ResponseEntity<ApiResponse<String>> resetFlightData() {
        try {
            resetService.resetFlightData();
            
            // 重新初始化航班数据
            try {
                dataInitializer.reinitializeData();
            } catch (Exception initException) {
                System.err.println("重新初始化航班数据时发生错误: " + initException.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.success("航班数据重置成功，新航班数据已生成", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("航班数据重置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置航空公司数据
     */
    @PostMapping("/airlines")
    public ResponseEntity<ApiResponse<String>> resetAirlineData() {
        try {
            resetService.resetAirlineData();
            
            // 重新初始化航空公司和航班数据
            try {
                dataInitializer.reinitializeData();
            } catch (Exception initException) {
                System.err.println("重新初始化航空公司数据时发生错误: " + initException.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.success("航空公司数据重置成功，新航空公司和航班数据已生成", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("航空公司数据重置失败: " + e.getMessage()));
        }
    }

    /**
     * 重新初始化基础数据的辅助方法
     */
    private void reinitializeBaseData() {
        try {
            dataInitializer.reinitializeData();
            System.out.println("基础数据重新初始化完成");
        } catch (Exception e) {
            System.err.println("重新初始化基础数据时发生错误: " + e.getMessage());
        }
    }
}