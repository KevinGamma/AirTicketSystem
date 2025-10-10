package com.airticket.service;

import com.airticket.mapper.NotificationMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.mapper.FlightMapper;
import com.airticket.model.Notification;
import com.airticket.model.Ticket;
import com.airticket.model.Flight;
import com.airticket.model.AdminApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private TicketMapper ticketMapper;
    
    @Autowired
    private FlightMapper flightMapper;
    
    
    private final Set<String> scheduledNotifications = new HashSet<>();

    


    public List<Notification> getNotificationsByUserId(Long userId, int limit, int offset) {
        return notificationMapper.findByUserIdPaginated(userId, limit, offset);
    }

    


    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    


    @Transactional
    public void markAsRead(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
    }

    


    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsReadByUserId(userId);
    }

    


    @Transactional
    public void scheduleFlightReminder(Ticket ticket) {
        try {
            if (ticket.getFlight() == null || ticket.getFlight().getDepartureTimeUtc() == null) {
                logger.warn("Cannot schedule flight reminder for ticket {} - flight or departure time is null", ticket.getId());
                return;
            }

            String key = "REMINDER_" + ticket.getId();
            if (scheduledNotifications.contains(key)) {
                return; 
            }

            Instant reminderTime = ticket.getFlight().getDepartureTimeUtc().minus(1, ChronoUnit.HOURS);
            
            
            if (reminderTime.isAfter(Instant.now())) {
                Notification notification = new Notification(
                    ticket.getUserId(),
                    Notification.TYPE_FLIGHT_REMINDER,
                    "航班提醒",
                    String.format("您的航班 %s 将在1小时后起飞，请提前到达机场办理值机手续。", 
                                ticket.getFlight().getFlightNumber())
                );
                notification.setTicketId(ticket.getId());
                notification.setFlightId(ticket.getFlightId());
                notification.setScheduledTime(reminderTime);
                
                notificationMapper.insert(notification);
                scheduledNotifications.add(key);
                logger.info("Scheduled flight reminder for ticket {} at {}", ticket.getId(), reminderTime);
            }
        } catch (Exception e) {
            logger.error("Error scheduling flight reminder for ticket " + ticket.getId(), e);
        }
    }

    


    @Transactional
    public void createFlightTakeoffNotification(Flight flight, List<Long> passengerUserIds) {
        try {
            for (Long userId : passengerUserIds) {
                Notification notification = new Notification(
                    userId,
                    Notification.TYPE_FLIGHT_TAKEOFF,
                    "航班起飞",
                    String.format("您的航班 %s 已成功起飞，祝您旅途愉快！", flight.getFlightNumber())
                );
                notification.setFlightId(flight.getId());
                notification.setSentTime(Instant.now());
                
                notificationMapper.insert(notification);
            }
            logger.info("Created takeoff notifications for flight {} for {} passengers", 
                       flight.getFlightNumber(), passengerUserIds.size());
        } catch (Exception e) {
            logger.error("Error creating takeoff notification for flight " + flight.getFlightNumber(), e);
        }
    }

    


    @Transactional
    public void createFlightLandingNotification(Flight flight, List<Long> passengerUserIds) {
        try {
            for (Long userId : passengerUserIds) {
                Notification notification = new Notification(
                    userId,
                    Notification.TYPE_FLIGHT_LANDING,
                    "航班到达",
                    String.format("您的航班 %s 已安全到达目的地，欢迎您再次选择我们的服务！", flight.getFlightNumber())
                );
                notification.setFlightId(flight.getId());
                notification.setSentTime(Instant.now());
                
                notificationMapper.insert(notification);
            }
            logger.info("Created landing notifications for flight {} for {} passengers", 
                       flight.getFlightNumber(), passengerUserIds.size());
        } catch (Exception e) {
            logger.error("Error creating landing notification for flight " + flight.getFlightNumber(), e);
        }
    }

    


    @Transactional
    public void createRescheduleApprovalNotification(AdminApprovalRequest request, Ticket originalTicket, Ticket newTicket) {
        try {
            String message;
            String notificationType = Notification.TYPE_RESCHEDULE_APPROVED;
            
            
            BigDecimal additionalFee = request.getAdditionalFee();
            BigDecimal refundAmount = request.getRefundAmount();
            
            if (additionalFee != null && additionalFee.compareTo(BigDecimal.ZERO) > 0) {
                message = String.format("您的改签申请已通过！新航班：%s，需支付额外费用 ¥%.2f。请及时完成支付。", 
                                      newTicket.getFlight() != null ? newTicket.getFlight().getFlightNumber() : "新航班", 
                                      additionalFee);
                notificationType = Notification.TYPE_PAYMENT_REQUIRED;
            } else if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                message = String.format("您的改签申请已通过！新航班：%s，将退还差额 ¥%.2f 至您的原支付方式。", 
                                      newTicket.getFlight() != null ? newTicket.getFlight().getFlightNumber() : "新航班", 
                                      refundAmount);
                notificationType = Notification.TYPE_REFUND_DIFFERENCE;
            } else {
                message = String.format("您的改签申请已通过！新航班：%s，无需额外费用。", 
                                      newTicket.getFlight() != null ? newTicket.getFlight().getFlightNumber() : "新航班");
            }

            Notification notification = new Notification(
                request.getUserId(),
                notificationType,
                "改签申请通过",
                message
            );
            notification.setTicketId(originalTicket.getId());
            notification.setRequestId(request.getId());
            notification.setSentTime(Instant.now());
            
            notificationMapper.insert(notification);
            logger.info("Created reschedule approval notification for request {}", request.getId());
            
        } catch (Exception e) {
            logger.error("Error creating reschedule approval notification for request " + request.getId(), e);
        }
    }

    


    @Transactional
    public void createRescheduleRejectionNotification(AdminApprovalRequest request, String rejectionReason) {
        try {
            String message = "很抱歉，您的改签申请未通过。";
            if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                message += "原因：" + rejectionReason;
            }

            Notification notification = new Notification(
                request.getUserId(),
                Notification.TYPE_RESCHEDULE_REJECTED,
                "改签申请未通过",
                message
            );
            notification.setRequestId(request.getId());
            notification.setSentTime(Instant.now());
            
            notificationMapper.insert(notification);
            logger.info("Created reschedule rejection notification for request {}", request.getId());
            
        } catch (Exception e) {
            logger.error("Error creating reschedule rejection notification for request " + request.getId(), e);
        }
    }

    


    @Transactional
    public void createRefundProcessedNotification(Long userId, Long ticketId, BigDecimal refundAmount, String ticketNumber) {
        try {
            Notification notification = new Notification(
                userId,
                Notification.TYPE_REFUND_PROCESSED,
                "退款处理完成",
                String.format("您的票据 %s 退款已处理完成，退款金额 ¥%.2f 将在3-5个工作日内退回您的原支付方式。", 
                            ticketNumber != null ? ticketNumber : "未知", refundAmount)
            );
            notification.setTicketId(ticketId);
            notification.setSentTime(Instant.now());
            
            notificationMapper.insert(notification);
            logger.info("Created refund processed notification for user {} ticket {} amount {}", 
                       userId, ticketId, refundAmount);
            
        } catch (Exception e) {
            logger.error("Error creating refund processed notification for user " + userId, e);
        }
    }

    



    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processScheduledNotifications() {
        try {
            Instant now = Instant.now();
            
            
            List<Notification> pendingNotifications = notificationMapper.findPendingScheduledNotifications(now);
            for (Notification notification : pendingNotifications) {
                try {
                    notification.setSentTime(now);
                    notificationMapper.markAsSent(notification.getId(), now);
                    logger.info("Sent scheduled notification {} of type {}", notification.getId(), notification.getNotificationType());
                } catch (Exception e) {
                    logger.error("Failed to send scheduled notification " + notification.getId(), e);
                }
            }

            
            checkFlightStatusNotifications(now);
            
            
            scheduleRemindersForNewTickets();

        } catch (Exception e) {
            logger.error("Error in scheduled notification processing", e);
        }
    }

    


    private void checkFlightStatusNotifications(Instant now) {
        try {
            
            Instant takeoffWindowStart = now.minus(5, ChronoUnit.MINUTES);
            Instant takeoffWindowEnd = now.plus(1, ChronoUnit.MINUTES);
            
            
            Instant landingWindowStart = now.minus(5, ChronoUnit.MINUTES);
            Instant landingWindowEnd = now.plus(1, ChronoUnit.MINUTES);
            
            
            List<Flight> departingFlights = flightMapper.findFlightsByDepartureTimeRange(takeoffWindowStart, takeoffWindowEnd);
            for (Flight flight : departingFlights) {
                
                List<Notification> existingTakeoffNotifs = notificationMapper.findByTicketIdAndType(null, Notification.TYPE_FLIGHT_TAKEOFF);
                boolean alreadySent = existingTakeoffNotifs.stream()
                    .anyMatch(n -> flight.getId().equals(n.getFlightId()));
                    
                if (!alreadySent) {
                    List<Long> passengerIds = ticketMapper.findUserIdsByFlightId(flight.getId());
                    if (!passengerIds.isEmpty()) {
                        createFlightTakeoffNotification(flight, passengerIds);
                    }
                }
            }
            
            
            List<Flight> arrivingFlights = flightMapper.findFlightsByArrivalTimeRange(landingWindowStart, landingWindowEnd);
            for (Flight flight : arrivingFlights) {
                
                List<Notification> existingLandingNotifs = notificationMapper.findByTicketIdAndType(null, Notification.TYPE_FLIGHT_LANDING);
                boolean alreadySent = existingLandingNotifs.stream()
                    .anyMatch(n -> flight.getId().equals(n.getFlightId()));
                    
                if (!alreadySent) {
                    List<Long> passengerIds = ticketMapper.findUserIdsByFlightId(flight.getId());
                    if (!passengerIds.isEmpty()) {
                        createFlightLandingNotification(flight, passengerIds);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error checking flight status notifications", e);
        }
    }

    


    private void scheduleRemindersForNewTickets() {
        try {
            
            List<Ticket> paidTickets = ticketMapper.findPaidTicketsWithoutReminders();
            for (Ticket ticket : paidTickets) {
                scheduleFlightReminder(ticket);
            }
        } catch (Exception e) {
            logger.error("Error scheduling reminders for new tickets", e);
        }
    }

    


    @Scheduled(cron = "0 0 2 * * ?") 
    @Transactional
    public void cleanupOldNotifications() {
        try {
            Instant cutoffTime = Instant.now().minus(30, ChronoUnit.DAYS);
            
            
            
            logger.info("Notification cleanup scheduled (implementation pending)");
            
        } catch (Exception e) {
            logger.error("Error in notification cleanup", e);
        }
    }
    
    


    @Transactional
    public void createTestNotification(Long userId, String title, String message) {
        try {
            Notification testNotification = new Notification(
                userId,
                Notification.TYPE_FLIGHT_REMINDER,
                title != null ? title : "测试通知",
                message != null ? message : "这是一个测试通知，用于验证通知系统是否正常工作。"
            );
            testNotification.setSentTime(Instant.now());
            
            notificationMapper.insert(testNotification);
            logger.info("Test notification created successfully for user {}, notification ID: {}", userId, testNotification.getId());
            
        } catch (Exception e) {
            logger.error("Error creating test notification for user " + userId, e);
            throw e;
        }
    }
}