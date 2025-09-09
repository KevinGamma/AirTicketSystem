package com.airticket.mapper;

import com.airticket.model.Notification;
import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notifications (user_id, ticket_id, flight_id, request_id, notification_type, title, message, is_read, scheduled_time, sent_time, created_at, updated_at) " +
            "VALUES (#{userId}, #{ticketId}, #{flightId}, #{requestId}, #{notificationType}, #{title}, #{message}, #{isRead}, #{scheduledTime}, #{sentTime}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Notification notification);

    @Select("SELECT * FROM notifications WHERE id = #{id}")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "ticket_id", property = "ticketId"),
        @Result(column = "flight_id", property = "flightId"),
        @Result(column = "request_id", property = "requestId"),
        @Result(column = "notification_type", property = "notificationType"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "scheduled_time", property = "scheduledTime"),
        @Result(column = "sent_time", property = "sentTime"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    Notification findById(@Param("id") Long id);

    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "ticket_id", property = "ticketId"),
        @Result(column = "flight_id", property = "flightId"),
        @Result(column = "request_id", property = "requestId"),
        @Result(column = "notification_type", property = "notificationType"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "scheduled_time", property = "scheduledTime"),
        @Result(column = "sent_time", property = "sentTime"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<Notification> findByUserIdPaginated(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY created_at DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "ticket_id", property = "ticketId"),
        @Result(column = "flight_id", property = "flightId"),
        @Result(column = "request_id", property = "requestId"),
        @Result(column = "notification_type", property = "notificationType"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "scheduled_time", property = "scheduledTime"),
        @Result(column = "sent_time", property = "sentTime"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<Notification> findByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = FALSE")
    int countUnreadByUserId(@Param("userId") Long userId);

    @Update("UPDATE notifications SET is_read = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void markAsRead(@Param("id") Long id);

    @Update("UPDATE notifications SET is_read = TRUE, updated_at = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM notifications WHERE scheduled_time IS NOT NULL AND scheduled_time <= #{now} AND sent_time IS NULL ORDER BY scheduled_time ASC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "ticket_id", property = "ticketId"),
        @Result(column = "flight_id", property = "flightId"),
        @Result(column = "request_id", property = "requestId"),
        @Result(column = "notification_type", property = "notificationType"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "scheduled_time", property = "scheduledTime"),
        @Result(column = "sent_time", property = "sentTime"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<Notification> findPendingScheduledNotifications(@Param("now") Instant now);

    @Update("UPDATE notifications SET sent_time = #{sentTime}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void markAsSent(@Param("id") Long id, @Param("sentTime") Instant sentTime);

    @Delete("DELETE FROM notifications WHERE user_id = #{userId} AND created_at < #{cutoffTime}")
    void deleteOldNotificationsByUserId(@Param("userId") Long userId, @Param("cutoffTime") Instant cutoffTime);

    @Select("SELECT * FROM notifications WHERE ticket_id = #{ticketId} AND notification_type = #{notificationType}")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "ticket_id", property = "ticketId"),
        @Result(column = "flight_id", property = "flightId"),
        @Result(column = "request_id", property = "requestId"),
        @Result(column = "notification_type", property = "notificationType"),
        @Result(column = "is_read", property = "isRead"),
        @Result(column = "scheduled_time", property = "scheduledTime"),
        @Result(column = "sent_time", property = "sentTime"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<Notification> findByTicketIdAndType(@Param("ticketId") Long ticketId, @Param("notificationType") String notificationType);
}