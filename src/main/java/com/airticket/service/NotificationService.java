package com.airticket.service;

import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.NotificationMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.model.AdminApprovalRequest;
import com.airticket.model.Flight;
import com.airticket.model.Notification;
import com.airticket.model.Ticket;
import com.airticket.util.TimeZoneUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration REMINDER_24_HOURS = Duration.ofHours(24);
    private static final Duration REMINDER_3_HOURS = Duration.ofHours(3);
    private static final Duration REMINDER_1_HOUR = Duration.ofHours(1);
    private static final Duration MIN_URGENT_REMINDER_WINDOW = Duration.ofMinutes(20);

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private FlightMapper flightMapper;

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
            Ticket reminderTicket = loadTicketFlight(ticket);
            if (reminderTicket == null || reminderTicket.getId() == null || reminderTicket.getFlight() == null) {
                logger.debug("Skipping reminder scheduling because ticket or flight data is incomplete");
                return;
            }

            Flight flight = reminderTicket.getFlight();
            Instant departureTime = flight.getDepartureTimeUtc();
            if (departureTime == null) {
                logger.debug("Skipping reminder scheduling for ticket {} because departure time is missing", reminderTicket.getId());
                return;
            }

            Instant now = Instant.now();
            if (!departureTime.isAfter(now)) {
                return;
            }

            boolean scheduledAny = false;
            scheduledAny |= scheduleReminderStage(
                reminderTicket,
                Notification.TYPE_FLIGHT_REMINDER_24H,
                "行程提醒",
                departureTime.minus(REMINDER_24_HOURS),
                buildReminderMessage(reminderTicket, flight, "您的航班将在24小时后起飞，请提前确认行程、证件和出行时间。")
            );
            scheduledAny |= scheduleReminderStage(
                reminderTicket,
                Notification.TYPE_FLIGHT_REMINDER_3H,
                "值机提醒",
                departureTime.minus(REMINDER_3_HOURS),
                buildReminderMessage(reminderTicket, flight, "您的航班将在3小时后起飞，建议尽快办理值机并安排前往机场。")
            );
            scheduledAny |= scheduleReminderStage(
                reminderTicket,
                Notification.TYPE_FLIGHT_REMINDER_1H,
                "登机提醒",
                departureTime.minus(REMINDER_1_HOUR),
                buildReminderMessage(reminderTicket, flight, "您的航班将在1小时后起飞，请留意登机时间和登机口信息。")
            );

            if (!scheduledAny && departureTime.isAfter(now.plus(MIN_URGENT_REMINDER_WINDOW))) {
                scheduleReminderStage(
                    reminderTicket,
                    Notification.TYPE_FLIGHT_REMINDER_FINAL,
                    "临近起飞提醒",
                    now,
                    buildReminderMessage(reminderTicket, flight, "您的航班即将起飞，请立即确认出行安排并尽快前往登机。")
                );
            }
        } catch (Exception e) {
            logger.error("Error scheduling flight reminder for ticket {}", ticket != null ? ticket.getId() : null, e);
        }
    }

    @Transactional
    public void createFlightTakeoffNotification(Flight flight, List<Long> passengerUserIds) {
        if (flight == null || flight.getId() == null || passengerUserIds == null || passengerUserIds.isEmpty()) {
            return;
        }

        try {
            if (!notificationMapper.findByFlightIdAndType(flight.getId(), Notification.TYPE_FLIGHT_TAKEOFF).isEmpty()) {
                return;
            }

            String message = "您乘坐的航班 " + safeFlightNumber(flight) + " 已起飞，祝您旅途顺利。";
            for (Long userId : passengerUserIds) {
                insertImmediateNotification(
                    userId,
                    null,
                    flight.getId(),
                    null,
                    Notification.TYPE_FLIGHT_TAKEOFF,
                    "航班已起飞",
                    message
                );
            }
        } catch (Exception e) {
            logger.error("Error creating takeoff notification for flight {}", flight.getId(), e);
        }
    }

    @Transactional
    public void createFlightLandingNotification(Flight flight, List<Long> passengerUserIds) {
        if (flight == null || flight.getId() == null || passengerUserIds == null || passengerUserIds.isEmpty()) {
            return;
        }

        try {
            if (!notificationMapper.findByFlightIdAndType(flight.getId(), Notification.TYPE_FLIGHT_LANDING).isEmpty()) {
                return;
            }

            String message = "您乘坐的航班 " + safeFlightNumber(flight) + " 已到达目的地，感谢您的使用。";
            for (Long userId : passengerUserIds) {
                insertImmediateNotification(
                    userId,
                    null,
                    flight.getId(),
                    null,
                    Notification.TYPE_FLIGHT_LANDING,
                    "航班已到达",
                    message
                );
            }
        } catch (Exception e) {
            logger.error("Error creating landing notification for flight {}", flight.getId(), e);
        }
    }

    @Transactional
    public void createRescheduleApprovalNotification(AdminApprovalRequest request, Ticket originalTicket, Ticket newTicket) {
        if (request == null) {
            return;
        }

        try {
            Ticket resolvedNewTicket = loadTicketFlight(newTicket);
            String flightSummary = formatFlightSummary(resolvedNewTicket);

            BigDecimal additionalFee = request.getAdditionalFee();
            BigDecimal refundAmount = request.getRefundAmount();

            String notificationType;
            String title;
            String message;

            if (isPositive(additionalFee)) {
                notificationType = Notification.TYPE_PAYMENT_REQUIRED;
                title = "改签审核通过，待支付";
                message = "您的改签申请已通过。新航班" + flightSummary +
                    " 已为您保留，请支付补差 " + formatAmount(additionalFee) + " 后完成改签。";
            } else if (isPositive(refundAmount)) {
                notificationType = Notification.TYPE_REFUND_DIFFERENCE;
                title = "改签审核通过";
                message = "您的改签申请已通过。新航班" + flightSummary +
                    " 已生效，本次改签将退还差价 " + formatAmount(refundAmount) + "。";
            } else {
                notificationType = Notification.TYPE_RESCHEDULE_APPROVED;
                title = "改签审核通过";
                message = "您的改签申请已通过。新航班" + flightSummary + " 已生效，本次改签无需额外支付。";
            }

            insertRequestNotificationIfAbsent(
                request,
                originalTicket != null ? originalTicket.getId() : null,
                resolvedNewTicket != null ? resolvedNewTicket.getFlightId() : null,
                notificationType,
                title,
                message
            );
        } catch (Exception e) {
            logger.error("Error creating reschedule approval notification for request {}", request.getId(), e);
        }
    }

    @Transactional
    public void createReschedulePaymentCompletedNotification(AdminApprovalRequest request, Ticket originalTicket, Ticket newTicket) {
        if (request == null) {
            return;
        }

        try {
            Ticket resolvedNewTicket = loadTicketFlight(newTicket);
            String message = "您的改签补差支付已完成。新航班" + formatFlightSummary(resolvedNewTicket) + " 已出票并生效。";

            insertRequestNotificationIfAbsent(
                request,
                originalTicket != null ? originalTicket.getId() : null,
                resolvedNewTicket != null ? resolvedNewTicket.getFlightId() : null,
                Notification.TYPE_RESCHEDULE_COMPLETED,
                "改签已完成",
                message
            );
        } catch (Exception e) {
            logger.error("Error creating reschedule completion notification for request {}", request.getId(), e);
        }
    }

    @Transactional
    public void createRescheduleRejectionNotification(AdminApprovalRequest request, String rejectionReason) {
        if (request == null) {
            return;
        }

        try {
            String message = "很抱歉，您的改签申请未通过审核。";
            if (rejectionReason != null && !rejectionReason.isBlank()) {
                message += " 原因：" + rejectionReason.trim();
            }

            insertRequestNotificationIfAbsent(
                request,
                request.getTicketId(),
                null,
                Notification.TYPE_RESCHEDULE_REJECTED,
                "改签申请未通过",
                message
            );
        } catch (Exception e) {
            logger.error("Error creating reschedule rejection notification for request {}", request.getId(), e);
        }
    }

    @Transactional
    public void createRefundProcessedNotification(Long userId, Long ticketId, BigDecimal refundAmount, String ticketNumber) {
        if (userId == null || ticketId == null || refundAmount == null) {
            return;
        }

        try {
            if (!notificationMapper.findByTicketIdAndType(ticketId, Notification.TYPE_REFUND_PROCESSED).isEmpty()) {
                return;
            }

            String message = "您的机票 " + (ticketNumber != null ? ticketNumber : "") +
                " 退款已受理，退款金额为 " + formatAmount(refundAmount) + "。";
            insertImmediateNotification(
                userId,
                ticketId,
                null,
                null,
                Notification.TYPE_REFUND_PROCESSED,
                "退款处理完成",
                message.trim()
            );
        } catch (Exception e) {
            logger.error("Error creating refund processed notification for ticket {}", ticketId, e);
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
                    notificationMapper.markAsSent(notification.getId(), now);
                    logger.info("Sent scheduled notification {} of type {}", notification.getId(), notification.getNotificationType());
                } catch (Exception e) {
                    logger.error("Failed to send scheduled notification {}", notification.getId(), e);
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

            List<Flight> departingFlights = flightMapper.findFlightsByDepartureTimeRange(takeoffWindowStart, takeoffWindowEnd);
            for (Flight flight : departingFlights) {
                if (notificationMapper.findByFlightIdAndType(flight.getId(), Notification.TYPE_FLIGHT_TAKEOFF).isEmpty()) {
                    List<Long> passengerIds = ticketMapper.findUserIdsByFlightId(flight.getId());
                    createFlightTakeoffNotification(flight, passengerIds);
                }
            }

            Instant landingWindowStart = now.minus(5, ChronoUnit.MINUTES);
            Instant landingWindowEnd = now.plus(1, ChronoUnit.MINUTES);

            List<Flight> arrivingFlights = flightMapper.findFlightsByArrivalTimeRange(landingWindowStart, landingWindowEnd);
            for (Flight flight : arrivingFlights) {
                if (notificationMapper.findByFlightIdAndType(flight.getId(), Notification.TYPE_FLIGHT_LANDING).isEmpty()) {
                    List<Long> passengerIds = ticketMapper.findUserIdsByFlightId(flight.getId());
                    createFlightLandingNotification(flight, passengerIds);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking flight status notifications", e);
        }
    }

    private void scheduleRemindersForNewTickets() {
        try {
            List<Ticket> paidTickets = ticketMapper.findPaidTicketsForReminderScheduling();
            for (Ticket ticket : paidTickets) {
                scheduleFlightReminder(ticket);
            }
        } catch (Exception e) {
            logger.error("Error scheduling reminders for paid tickets", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            logger.info("Notification cleanup scheduled (implementation pending)");
        } catch (Exception e) {
            logger.error("Error in notification cleanup", e);
        }
    }

    @Transactional
    public void createTestNotification(Long userId, String title, String message) {
        insertImmediateNotification(
            userId,
            null,
            null,
            null,
            Notification.TYPE_FLIGHT_REMINDER,
            title != null ? title : "测试通知",
            message != null ? message : "这是一条测试通知，用于验证通知系统是否正常工作。"
        );
    }

    private boolean scheduleReminderStage(Ticket ticket, String notificationType, String title, Instant scheduledTime, String message) {
        if (ticket == null || ticket.getId() == null || scheduledTime == null) {
            return false;
        }

        Instant now = Instant.now();
        if (scheduledTime.isBefore(now.minusSeconds(30))) {
            return false;
        }

        if (ticket.getUserId() != null && ticket.getFlightId() != null) {
            List<Notification> existingNotifications = notificationMapper.findByUserIdAndFlightIdAndType(
                ticket.getUserId(),
                ticket.getFlightId(),
                notificationType
            );
            if (!existingNotifications.isEmpty()) {
                deduplicateNotifications(existingNotifications);
                return true;
            }
        } else if (!notificationMapper.findByTicketIdAndType(ticket.getId(), notificationType).isEmpty()) {
            return true;
        }

        Notification notification = new Notification(ticket.getUserId(), notificationType, title, message);
        notification.setTicketId(ticket.getId());
        notification.setFlightId(ticket.getFlightId());
        notification.setScheduledTime(scheduledTime);
        notificationMapper.insert(notification);
        logger.info("Scheduled {} for ticket {} at {}", notificationType, ticket.getId(), scheduledTime);
        return true;
    }

    private void insertRequestNotificationIfAbsent(
        AdminApprovalRequest request,
        Long ticketId,
        Long flightId,
        String notificationType,
        String title,
        String message
    ) {
        if (request.getId() != null && !notificationMapper.findByRequestIdAndType(request.getId(), notificationType).isEmpty()) {
            return;
        }

        insertImmediateNotification(
            request.getUserId(),
            ticketId,
            flightId,
            request.getId(),
            notificationType,
            title,
            message
        );
    }

    private void insertImmediateNotification(
        Long userId,
        Long ticketId,
        Long flightId,
        Long requestId,
        String notificationType,
        String title,
        String message
    ) {
        if (userId == null) {
            return;
        }

        Notification notification = new Notification(userId, notificationType, title, message);
        Instant now = Instant.now();
        notification.setTicketId(ticketId);
        notification.setFlightId(flightId);
        notification.setRequestId(requestId);
        notification.setSentTime(now);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notificationMapper.insert(notification);
    }

    private Ticket loadTicketFlight(Ticket ticket) {
        if (ticket == null || ticket.getFlight() != null || ticket.getFlightId() == null) {
            return ticket;
        }

        Flight flight = flightMapper.findById(ticket.getFlightId());
        if (flight != null) {
            ticket.setFlight(flight);
        }
        return ticket;
    }

    private String buildReminderMessage(Ticket ticket, Flight flight, String prefix) {
        return prefix + " 航班信息：" + formatFlightSummary(ticket);
    }

    private String formatFlightSummary(Ticket ticket) {
        if (ticket == null) {
            return "待确认。";
        }
        return formatFlightSummary(loadTicketFlight(ticket) != null ? ticket.getFlight() : null);
    }

    private String formatFlightSummary(Flight flight) {
        if (flight == null) {
            return "待确认。";
        }

        String route = safeCity(flight, true) + " -> " + safeCity(flight, false);
        String departureText = formatDepartureTime(flight);
        return " " + safeFlightNumber(flight) + "（" + route + "，" + departureText + "）";
    }

    private String formatDepartureTime(Flight flight) {
        if (flight == null || flight.getDepartureTimeUtc() == null) {
            return "时间待确认";
        }

        ZoneId zone = resolveDepartureZone(flight);
        String formatted = TimeZoneUtil.formatInTimeZone(flight.getDepartureTimeUtc(), zone);
        return formatted != null ? formatted : "时间待确认";
    }

    private ZoneId resolveDepartureZone(Flight flight) {
        if (flight != null && flight.getDepartureAirport() != null) {
            String timeZone = flight.getDepartureAirport().getTimeZone();
            if (TimeZoneUtil.isValidTimeZone(timeZone)) {
                return ZoneId.of(timeZone);
            }
        }
        return DEFAULT_ZONE;
    }

    private String safeFlightNumber(Flight flight) {
        return flight != null && flight.getFlightNumber() != null ? flight.getFlightNumber() : "待确认航班";
    }

    private String safeCity(Flight flight, boolean departure) {
        if (flight == null) {
            return departure ? "出发地" : "目的地";
        }

        if (departure) {
            if (flight.getDepartureAirport() != null && flight.getDepartureAirport().getCity() != null) {
                return flight.getDepartureAirport().getCity();
            }
            return "出发地";
        }

        if (flight.getArrivalAirport() != null && flight.getArrivalAirport().getCity() != null) {
            return flight.getArrivalAirport().getCity();
        }
        return "目的地";
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "¥0.00" : "¥" + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private void deduplicateNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.size() <= 1) {
            return;
        }

        for (int i = 1; i < notifications.size(); i++) {
            Notification duplicate = notifications.get(i);
            if (duplicate.getId() != null) {
                notificationMapper.deleteById(duplicate.getId());
            }
        }
    }
}
