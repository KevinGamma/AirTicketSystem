package com.airticket.controller;

import com.airticket.dto.AdminApprovalResponse;
import com.airticket.dto.ApiResponse;
import com.airticket.dto.RescheduleFeeInfo;
import com.airticket.model.AdminApprovalRequest;
import com.airticket.model.User;
import com.airticket.service.AdminApprovalService;
import com.airticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/approval-requests")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    @Autowired
    private AdminApprovalService adminApprovalService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminApprovalRequest>>> getAllRequests() {
        try {
            List<AdminApprovalRequest> requests = adminApprovalService.getAllRequests();
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get approval requests: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdminApprovalRequest>>> getPendingRequests() {
        try {
            List<AdminApprovalRequest> requests = adminApprovalService.getPendingRequests();
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get pending requests: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminApprovalRequest>> getRequestById(@PathVariable Long id) {
        try {
            AdminApprovalRequest request = adminApprovalService.getRequestById(id);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get request: " + e.getMessage()));
        }
    }

    



    @GetMapping("/{id}/fee-info")
    public ResponseEntity<ApiResponse<RescheduleFeeInfo>> getApprovalRequestFeeInfo(@PathVariable Long id) {
        try {
            RescheduleFeeInfo feeInfo = adminApprovalService.getApprovalRequestFeeInfo(id);
            return ResponseEntity.ok(ApiResponse.success("Fee information retrieved successfully", feeInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get fee information: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<AdminApprovalRequest>> processRequest(
            @PathVariable Long id, 
            @RequestBody AdminApprovalResponse response) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User admin = userService.findByUsername(username);

            AdminApprovalRequest processedRequest;
            
            if ("APPROVE".equals(response.getAction())) {
                processedRequest = adminApprovalService.approveRequest(id, admin.getId());
                return ResponseEntity.ok(ApiResponse.success("Request approved successfully", processedRequest));
            } else if ("REJECT".equals(response.getAction())) {
                if (response.getRejectionReason() == null || response.getRejectionReason().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Rejection reason is required"));
                }
                processedRequest = adminApprovalService.rejectRequest(id, admin.getId(), response.getRejectionReason());
                return ResponseEntity.ok(ApiResponse.success("Request rejected successfully", processedRequest));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid action. Use 'APPROVE' or 'REJECT'"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process request: " + e.getMessage()));
        }
    }
    
    @PostMapping("/fix-pending-reschedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> fixPendingReschedules() {
        try {
            adminApprovalService.fixPendingRescheduleTickets();
            return ResponseEntity.ok(ApiResponse.success("Successfully processed pending reschedule tickets", "Fix completed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fix pending reschedules: " + e.getMessage()));
        }
    }
}