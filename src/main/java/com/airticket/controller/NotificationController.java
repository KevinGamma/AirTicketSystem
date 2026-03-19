package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.model.Notification;
import com.airticket.model.User;
import com.airticket.service.AuthenticatedUserPrincipal;
import com.airticket.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            int offset = page * size;
            List<Notification> notifications = notificationService.getNotificationsByUserId(currentUser.getId(), size, offset);
            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            logger.error("Error fetching notifications for user {}", currentUser.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("获取通知失败"));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            int count = notificationService.getUnreadCount(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            logger.error("Error fetching unread count for user {}", currentUser.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("获取未读通知数量失败"));
        }
    }

    @PutMapping("/{notificationId}/mark-read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", notificationId, e);
            return ResponseEntity.ok(ApiResponse.error("标记通知失败"));
        }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            notificationService.markAllAsRead(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("Error marking all notifications as read for user {}", currentUser.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("标记所有通知失败"));
        }
    }

    @PostMapping("/debug/create-test")
    public ResponseEntity<ApiResponse<Void>> createTestNotification(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            String message = "这是一条测试通知，用于验证通知系统是否正常工作。创建时间: " + java.time.Instant.now();
            notificationService.createTestNotification(currentUser.getId(), "测试通知", message);
            logger.info("Test notification created for user {}", currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("测试通知创建成功", null));
        } catch (Exception e) {
            logger.error("Error creating test notification for user {}", currentUser.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("创建测试通知失败: " + e.getMessage()));
        }
    }

    @GetMapping("/debug/status")
    public ResponseEntity<ApiResponse<String>> getNotificationSystemStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        User currentUser = extractCurrentUser(principal);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        try {
            List<Notification> allNotifications = notificationService.getNotificationsByUserId(currentUser.getId(), 100, 0);
            int unreadCount = notificationService.getUnreadCount(currentUser.getId());

            String status = String.format(
                    "用户 %d 的通知状态:%n- 总通知数: %d%n- 未读通知数: %d%n- 通知服务状态: 正常%n- 检查时间: %s",
                    currentUser.getId(),
                    allNotifications.size(),
                    unreadCount,
                    java.time.Instant.now()
            );

            logger.info("Notification status check for user {}: {} notifications, {} unread",
                    currentUser.getId(), allNotifications.size(), unreadCount);

            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            logger.error("Error checking notification status for user {}", currentUser.getId(), e);
            return ResponseEntity.ok(ApiResponse.error("获取状态失败: " + e.getMessage()));
        }
    }

    private User extractCurrentUser(AuthenticatedUserPrincipal principal) {
        return principal != null ? principal.toSafeUser() : null;
    }
}
