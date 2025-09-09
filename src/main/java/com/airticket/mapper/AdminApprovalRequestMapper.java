package com.airticket.mapper;

import com.airticket.model.AdminApprovalRequest;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminApprovalRequestMapper {
    
    @Select("SELECT aar.*, " +
            "t.ticket_number, t.passenger_name, t.status as ticket_status, t.price as ticket_price, " +
            "u.username, u.full_name as user_full_name, " +
            "admin.username as admin_username, admin.full_name as admin_full_name, " +
            "f.flight_number as current_flight_number, f.departure_time as current_departure_time, f.arrival_time as current_arrival_time, f.price as current_flight_price, " +
            "da.name as current_departure_airport, aa.name as current_arrival_airport, " +
            "nf.flight_number as new_flight_number, nf.departure_time as new_departure_time, nf.arrival_time as new_arrival_time, nf.price as new_flight_price, " +
            "nda.name as new_departure_airport, naa.name as new_arrival_airport " +
            "FROM admin_approval_requests aar " +
            "LEFT JOIN tickets t ON aar.ticket_id = t.id " +
            "LEFT JOIN users u ON aar.user_id = u.id " +
            "LEFT JOIN users admin ON aar.approved_by_admin_id = admin.id " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN flights nf ON aar.new_flight_id = nf.id " +
            "LEFT JOIN airports nda ON nf.departure_airport_id = nda.id " +
            "LEFT JOIN airports naa ON nf.arrival_airport_id = naa.id " +
            "ORDER BY aar.request_time DESC")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "ticket.ticketNumber", column = "ticket_number"),
        @Result(property = "ticket.passengerName", column = "passenger_name"),
        @Result(property = "ticket.status", column = "ticket_status"),
        @Result(property = "ticket.price", column = "ticket_price"),
        @Result(property = "ticket.flight.flightNumber", column = "current_flight_number"),
        @Result(property = "ticket.flight.departureTime", column = "current_departure_time"),
        @Result(property = "ticket.flight.arrivalTime", column = "current_arrival_time"),
        @Result(property = "ticket.flight.price", column = "current_flight_price"),
        @Result(property = "ticket.flight.departureAirport.name", column = "current_departure_airport"),
        @Result(property = "ticket.flight.arrivalAirport.name", column = "current_arrival_airport"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.fullName", column = "user_full_name"),
        @Result(property = "approvedByAdmin.username", column = "admin_username"),
        @Result(property = "approvedByAdmin.fullName", column = "admin_full_name"),
        @Result(property = "newFlight.flightNumber", column = "new_flight_number"),
        @Result(property = "newFlight.departureTime", column = "new_departure_time"),
        @Result(property = "newFlight.arrivalTime", column = "new_arrival_time"),
        @Result(property = "newFlight.price", column = "new_flight_price"),
        @Result(property = "newFlight.departureAirport.name", column = "new_departure_airport"),
        @Result(property = "newFlight.arrivalAirport.name", column = "new_arrival_airport")
    })
    List<AdminApprovalRequest> findAll();
    
    
    @Select("SELECT aar.*, " +
            "t.ticket_number, t.passenger_name, t.status as ticket_status, t.price as ticket_price, " +
            "u.username, u.full_name as user_full_name, " +
            "admin.username as admin_username, admin.full_name as admin_full_name, " +
            "f.flight_number as current_flight_number, f.departure_time as current_departure_time, f.arrival_time as current_arrival_time, f.price as current_flight_price, " +
            "da.name as current_departure_airport, aa.name as current_arrival_airport, " +
            "nf.flight_number as new_flight_number, nf.departure_time as new_departure_time, nf.arrival_time as new_arrival_time, nf.price as new_flight_price, " +
            "nda.name as new_departure_airport, naa.name as new_arrival_airport " +
            "FROM admin_approval_requests aar " +
            "LEFT JOIN tickets t ON aar.ticket_id = t.id " +
            "LEFT JOIN users u ON aar.user_id = u.id " +
            "LEFT JOIN users admin ON aar.approved_by_admin_id = admin.id " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN flights nf ON aar.new_flight_id = nf.id " +
            "LEFT JOIN airports nda ON nf.departure_airport_id = nda.id " +
            "LEFT JOIN airports naa ON nf.arrival_airport_id = naa.id " +
            "WHERE aar.id = #{id}")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "ticket.ticketNumber", column = "ticket_number"),
        @Result(property = "ticket.passengerName", column = "passenger_name"),
        @Result(property = "ticket.status", column = "ticket_status"),
        @Result(property = "ticket.price", column = "ticket_price"),
        @Result(property = "ticket.flight.flightNumber", column = "current_flight_number"),
        @Result(property = "ticket.flight.departureTime", column = "current_departure_time"),
        @Result(property = "ticket.flight.arrivalTime", column = "current_arrival_time"),
        @Result(property = "ticket.flight.price", column = "current_flight_price"),
        @Result(property = "ticket.flight.departureAirport.name", column = "current_departure_airport"),
        @Result(property = "ticket.flight.arrivalAirport.name", column = "current_arrival_airport"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.fullName", column = "user_full_name"),
        @Result(property = "approvedByAdmin.username", column = "admin_username"),
        @Result(property = "approvedByAdmin.fullName", column = "admin_full_name"),
        @Result(property = "newFlight.flightNumber", column = "new_flight_number"),
        @Result(property = "newFlight.departureTime", column = "new_departure_time"),
        @Result(property = "newFlight.arrivalTime", column = "new_arrival_time"),
        @Result(property = "newFlight.price", column = "new_flight_price"),
        @Result(property = "newFlight.departureAirport.name", column = "new_departure_airport"),
        @Result(property = "newFlight.arrivalAirport.name", column = "new_arrival_airport")
    })
    AdminApprovalRequest findById(Long id);
    
    @Select("SELECT aar.*, " +
            "t.ticket_number, t.passenger_name, t.status as ticket_status, t.price as ticket_price, " +
            "u.username, u.full_name as user_full_name, " +
            "admin.username as admin_username, admin.full_name as admin_full_name, " +
            "f.flight_number as current_flight_number, f.departure_time as current_departure_time, f.arrival_time as current_arrival_time, f.price as current_flight_price, " +
            "da.name as current_departure_airport, aa.name as current_arrival_airport, " +
            "nf.flight_number as new_flight_number, nf.departure_time as new_departure_time, nf.arrival_time as new_arrival_time, nf.price as new_flight_price, " +
            "nda.name as new_departure_airport, naa.name as new_arrival_airport " +
            "FROM admin_approval_requests aar " +
            "LEFT JOIN tickets t ON aar.ticket_id = t.id " +
            "LEFT JOIN users u ON aar.user_id = u.id " +
            "LEFT JOIN users admin ON aar.approved_by_admin_id = admin.id " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN flights nf ON aar.new_flight_id = nf.id " +
            "LEFT JOIN airports nda ON nf.departure_airport_id = nda.id " +
            "LEFT JOIN airports naa ON nf.arrival_airport_id = naa.id " +
            "WHERE aar.status = #{status} " +
            "ORDER BY aar.request_time DESC")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "ticket.ticketNumber", column = "ticket_number"),
        @Result(property = "ticket.passengerName", column = "passenger_name"),
        @Result(property = "ticket.status", column = "ticket_status"),
        @Result(property = "ticket.price", column = "ticket_price"),
        @Result(property = "ticket.flight.flightNumber", column = "current_flight_number"),
        @Result(property = "ticket.flight.departureTime", column = "current_departure_time"),
        @Result(property = "ticket.flight.arrivalTime", column = "current_arrival_time"),
        @Result(property = "ticket.flight.price", column = "current_flight_price"),
        @Result(property = "ticket.flight.departureAirport.name", column = "current_departure_airport"),
        @Result(property = "ticket.flight.arrivalAirport.name", column = "current_arrival_airport"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.fullName", column = "user_full_name"),
        @Result(property = "approvedByAdmin.username", column = "admin_username"),
        @Result(property = "approvedByAdmin.fullName", column = "admin_full_name"),
        @Result(property = "newFlight.flightNumber", column = "new_flight_number"),
        @Result(property = "newFlight.departureTime", column = "new_departure_time"),
        @Result(property = "newFlight.arrivalTime", column = "new_arrival_time"),
        @Result(property = "newFlight.price", column = "new_flight_price"),
        @Result(property = "newFlight.departureAirport.name", column = "new_departure_airport"),
        @Result(property = "newFlight.arrivalAirport.name", column = "new_arrival_airport")
    })
    List<AdminApprovalRequest> findByStatus(String status);
    
    @Select("SELECT aar.*, " +
            "t.ticket_number, t.passenger_name, t.status as ticket_status, t.price as ticket_price, " +
            "u.username, u.full_name as user_full_name, " +
            "admin.username as admin_username, admin.full_name as admin_full_name, " +
            "f.flight_number as current_flight_number, f.departure_time as current_departure_time, f.arrival_time as current_arrival_time, f.price as current_flight_price, " +
            "da.name as current_departure_airport, aa.name as current_arrival_airport, " +
            "nf.flight_number as new_flight_number, nf.departure_time as new_departure_time, nf.arrival_time as new_arrival_time, nf.price as new_flight_price, " +
            "nda.name as new_departure_airport, naa.name as new_arrival_airport " +
            "FROM admin_approval_requests aar " +
            "LEFT JOIN tickets t ON aar.ticket_id = t.id " +
            "LEFT JOIN users u ON aar.user_id = u.id " +
            "LEFT JOIN users admin ON aar.approved_by_admin_id = admin.id " +
            "LEFT JOIN flights f ON t.flight_id = f.id " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN flights nf ON aar.new_flight_id = nf.id " +
            "LEFT JOIN airports nda ON nf.departure_airport_id = nda.id " +
            "LEFT JOIN airports naa ON nf.arrival_airport_id = naa.id " +
            "WHERE aar.user_id = #{userId} " +
            "ORDER BY aar.request_time DESC")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "ticket.ticketNumber", column = "ticket_number"),
        @Result(property = "ticket.passengerName", column = "passenger_name"),
        @Result(property = "ticket.status", column = "ticket_status"),
        @Result(property = "ticket.price", column = "ticket_price"),
        @Result(property = "ticket.flight.flightNumber", column = "current_flight_number"),
        @Result(property = "ticket.flight.departureTime", column = "current_departure_time"),
        @Result(property = "ticket.flight.arrivalTime", column = "current_arrival_time"),
        @Result(property = "ticket.flight.price", column = "current_flight_price"),
        @Result(property = "ticket.flight.departureAirport.name", column = "current_departure_airport"),
        @Result(property = "ticket.flight.arrivalAirport.name", column = "current_arrival_airport"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.fullName", column = "user_full_name"),
        @Result(property = "approvedByAdmin.username", column = "admin_username"),
        @Result(property = "approvedByAdmin.fullName", column = "admin_full_name"),
        @Result(property = "newFlight.flightNumber", column = "new_flight_number"),
        @Result(property = "newFlight.departureTime", column = "new_departure_time"),
        @Result(property = "newFlight.arrivalTime", column = "new_arrival_time"),
        @Result(property = "newFlight.price", column = "new_flight_price"),
        @Result(property = "newFlight.departureAirport.name", column = "new_departure_airport"),
        @Result(property = "newFlight.arrivalAirport.name", column = "new_arrival_airport")
    })
    List<AdminApprovalRequest> findByUserId(Long userId);
    
    @Insert("INSERT INTO admin_approval_requests (ticket_id, user_id, request_type, reason, service_fee, new_flight_id, status, request_time) " +
            "VALUES (#{ticketId}, #{userId}, #{requestType}, #{reason}, #{serviceFee}, #{newFlightId}, #{status}, #{requestTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminApprovalRequest request);
    
    @Update("UPDATE admin_approval_requests SET " +
            "status = #{status}, " +
            "approved_by_admin_id = #{approvedByAdminId}, " +
            "rejection_reason = #{rejectionReason}, " +
            "processed_time = #{processedTime}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                    @Param("status") String status,
                    @Param("approvedByAdminId") Long approvedByAdminId,
                    @Param("rejectionReason") String rejectionReason,
                    @Param("processedTime") LocalDateTime processedTime);
    
    @Update("UPDATE admin_approval_requests SET " +
            "status = #{status}, " +
            "approved_by_admin_id = #{approvedByAdminId}, " +
            "rejection_reason = #{rejectionReason}, " +
            "processed_time = #{processedTime}, " +
            "total_amount = #{totalAmount}, " +
            "refund_amount = #{refundAmount}, " +
            "payment_status = #{paymentStatus}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateStatusWithPaymentInfo(@Param("id") Long id,
                                   @Param("status") String status,
                                   @Param("approvedByAdminId") Long approvedByAdminId,
                                   @Param("rejectionReason") String rejectionReason,
                                   @Param("processedTime") LocalDateTime processedTime,
                                   @Param("totalAmount") BigDecimal totalAmount,
                                   @Param("refundAmount") BigDecimal refundAmount,
                                   @Param("paymentStatus") String paymentStatus);
    
    @Update("UPDATE admin_approval_requests SET refund_number = #{refundNumber}, updated_at = NOW() WHERE id = #{id}")
    int updateRefundNumber(@Param("id") Long id, @Param("refundNumber") String refundNumber);
    
    @Update("UPDATE admin_approval_requests SET payment_number = #{paymentNumber}, updated_at = NOW() WHERE id = #{id}")
    int updatePaymentNumber(@Param("id") Long id, @Param("paymentNumber") String paymentNumber);
    
    @Delete("DELETE FROM admin_approval_requests WHERE id = #{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM admin_approval_requests WHERE ticket_id = #{ticketId}")
    int deleteByTicketId(Long ticketId);
    
    @Delete("DELETE FROM admin_approval_requests")
    int deleteAll();
    
    @Select("SELECT * FROM admin_approval_requests WHERE ticket_id = #{ticketId}")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<AdminApprovalRequest> findByTicketId(Long ticketId);
    
    @Select("SELECT * FROM admin_approval_requests WHERE ticket_id = #{ticketId} AND status IN ('PENDING', 'AWAITING_PAYMENT')")
    @Results({
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "requestType", column = "request_type"),
        @Result(property = "serviceFee", column = "service_fee"),
        @Result(property = "newFlightId", column = "new_flight_id"),
        @Result(property = "approvedByAdminId", column = "approved_by_admin_id"),
        @Result(property = "rejectionReason", column = "rejection_reason"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "processedTime", column = "processed_time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<AdminApprovalRequest> findPendingRequestsByTicketId(Long ticketId);
}