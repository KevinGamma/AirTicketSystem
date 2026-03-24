package com.airticket.mapper;

import com.airticket.model.Ticket;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TicketMapper {
    
    @Select("SELECT * FROM tickets")
    List<Ticket> findAll();
    
    @Select("SELECT * FROM tickets WHERE id = #{id}")
    Ticket findById(Long id);
    
    @Select("SELECT * FROM tickets WHERE user_id = #{userId} AND deleted_by_user = false ORDER BY created_at DESC")
    List<Ticket> findByUserId(Long userId);
    
    @Select("SELECT * FROM tickets WHERE ticket_number = #{ticketNumber}")
    Ticket findByTicketNumber(String ticketNumber);
    
    @Select("SELECT COUNT(*) FROM tickets WHERE flight_id = #{flightId}")
    int countByFlightId(Long flightId);

    @Select("SELECT * FROM tickets WHERE id = (SELECT original_ticket_id FROM tickets WHERE id = #{newTicketId})")
    Ticket findOriginalTicketByNewTicketId(Long newTicketId);

    @Select("SELECT * FROM tickets WHERE id = (SELECT rescheduled_to_ticket_id FROM tickets WHERE id = #{originalTicketId})")
    Ticket findNewTicketByOriginalTicketId(Long originalTicketId);

    @Select("SELECT * FROM tickets WHERE user_id = #{userId} AND " +
            "(status = '已改签' OR original_ticket_id IS NOT NULL) AND " +
            "deleted_by_user = false ORDER BY created_at DESC")
    List<Ticket> findRescheduledTicketsByUserId(Long userId);
    
    @Insert("INSERT INTO tickets (ticket_number, user_id, flight_id, seat_number, " +
            "passenger_name, passenger_id_number, ticket_type, price, status, booking_time, created_at, updated_at, " +
            "original_ticket_id, rescheduled_to_ticket_id, is_original_ticket) " +
            "VALUES (#{ticketNumber}, #{userId}, #{flightId}, #{seatNumber}, " +
            "#{passengerName}, #{passengerIdNumber}, #{ticketType}, #{price}, #{status}, " +
            "#{bookingTime}, #{createdAt}, #{updatedAt}, #{originalTicketId}, #{rescheduledToTicketId}, #{isOriginalTicket})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ticket ticket);
    
    @Update("UPDATE tickets SET status = #{status}, payment_time = #{paymentTime}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int updateStatus(Ticket ticket);
    
    @Update("UPDATE tickets SET status = #{status}, payment_time = #{paymentTime}, updated_at = NOW() " +
            "WHERE id = #{ticketId}")
    int updateTicketStatus(@Param("ticketId") Long ticketId, 
                          @Param("status") String status, 
                          @Param("paymentTime") java.time.Instant paymentTime);
    
    @Update("UPDATE tickets SET flight_id = #{newFlightId} WHERE id = #{ticketId}")
    int changeTicketFlight(@Param("ticketId") Long ticketId, @Param("newFlightId") Long newFlightId);
    
    @Delete("DELETE FROM tickets WHERE id = #{id}")
    int deleteById(Long id);
    
    
    @Update("UPDATE tickets SET status = '已改签', service_fee = #{serviceFee}, " +
            "reschedule_reason = #{rescheduleReason}, reschedule_time = NOW(), " +
            "rescheduled_to_ticket_id = #{newTicketId}, is_original_ticket = true " +
            "WHERE id = #{originalTicketId}")
    int markTicketAsRescheduled(@Param("originalTicketId") Long originalTicketId,
                               @Param("newTicketId") Long newTicketId,
                               @Param("serviceFee") BigDecimal serviceFee,
                               @Param("rescheduleReason") String rescheduleReason);
    
    
    @Update("UPDATE tickets SET original_ticket_id = #{originalTicketId} WHERE id = #{newTicketId}")
    int linkNewTicketToOriginal(@Param("newTicketId") Long newTicketId, 
                               @Param("originalTicketId") Long originalTicketId);
    
    @Update("UPDATE tickets SET status = 'REFUNDED', service_fee = #{serviceFee}, " +
            "refund_reason = #{refundReason}, refund_time = NOW() WHERE id = #{ticketId}")
    int refundTicket(@Param("ticketId") Long ticketId, 
                    @Param("serviceFee") BigDecimal serviceFee,
                    @Param("refundReason") String refundReason);
    
    @Update("UPDATE tickets SET deleted_by_user = #{deletedByUser} WHERE id = #{ticketId}")
    int updateDeletedByUser(@Param("ticketId") Long ticketId, @Param("deletedByUser") boolean deletedByUser);
    
    @Delete("DELETE FROM tickets")
    int deleteAll();
    
    @Select("SELECT t.* FROM tickets t " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "WHERE t.status = 'BOOKED' " +
            "AND t.booking_time IS NOT NULL " +
            "AND (t.booking_time < DATE_SUB(#{currentTime}, INTERVAL 10 MINUTE) " +
            "OR f.departure_time < DATE_ADD(#{currentTime}, INTERVAL 40 MINUTE))")
    List<Ticket> findExpiredBookedTickets(@Param("currentTime") java.time.Instant currentTime);
    
    @Select("SELECT * FROM tickets WHERE status = #{status}")
    List<Ticket> findByStatus(String status);

    @Select("SELECT * FROM tickets WHERE original_ticket_id IS NOT NULL AND status = #{status}")
    List<Ticket> findRescheduledTicketsByStatus(@Param("status") String status);
    
    
    @Select("SELECT DISTINCT user_id FROM tickets WHERE flight_id = #{flightId} AND status IN ('PAID', 'BOOKED')")
    List<Long> findUserIdsByFlightId(@Param("flightId") Long flightId);
    
    @Select("SELECT t.* FROM tickets t " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "WHERE t.status = 'PAID' " +
            "AND t.deleted_by_user = FALSE " +
            "AND f.departure_time > UTC_TIMESTAMP()")
    List<Ticket> findPaidTicketsForReminderScheduling();
    
    
    @Insert("INSERT INTO ticket_connecting_flights (ticket_id, connecting_flight_id, sequence_number) " +
            "VALUES (#{ticketId}, #{connectingFlightId}, #{sequenceNumber})")
    int insertTicketConnectingFlight(@Param("ticketId") Long ticketId, 
                                   @Param("connectingFlightId") Long connectingFlightId,
                                   @Param("sequenceNumber") int sequenceNumber);
    
    @Select("SELECT tcf.connecting_flight_id FROM ticket_connecting_flights tcf " +
            "WHERE tcf.ticket_id = #{ticketId} ORDER BY tcf.sequence_number")
    List<Long> findConnectingFlightIdsByTicketId(@Param("ticketId") Long ticketId);
    
    @Delete("DELETE FROM ticket_connecting_flights WHERE ticket_id = #{ticketId}")
    int deleteTicketConnectingFlightsByTicketId(@Param("ticketId") Long ticketId);
}
