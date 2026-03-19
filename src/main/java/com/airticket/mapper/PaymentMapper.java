package com.airticket.mapper;

import com.airticket.model.Payment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PaymentMapper {
    
    @Insert("INSERT INTO payments (ticket_id, payment_number, alipay_trade_no, amount, currency, " +
            "payment_method, status, subject, body, buyer_logon_id, buyer_user_id, payment_time, " +
            "sandbox_mode, sandbox_buyer_account, created_at, updated_at) " +
            "VALUES (#{ticketId}, #{paymentNumber}, #{alipayTradeNo}, #{amount}, #{currency}, " +
            "#{paymentMethod}, #{status}, #{subject}, #{body}, #{buyerLogonId}, #{buyerUserId}, " +
            "#{paymentTime}, #{sandboxMode}, #{sandboxBuyerAccount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPayment(Payment payment);
    
    @Select("SELECT * FROM payments WHERE payment_number = #{paymentNumber}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "paymentNumber", column = "payment_number"),
        @Result(property = "alipayTradeNo", column = "alipay_trade_no"),
        @Result(property = "buyerLogonId", column = "buyer_logon_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "paymentTime", column = "payment_time"),
        @Result(property = "sandboxMode", column = "sandbox_mode"),
        @Result(property = "sandboxBuyerAccount", column = "sandbox_buyer_account"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Payment findByPaymentNumber(@Param("paymentNumber") String paymentNumber);
    
    @Select("SELECT * FROM payments WHERE ticket_id = #{ticketId} ORDER BY created_at DESC LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "paymentNumber", column = "payment_number"),
        @Result(property = "alipayTradeNo", column = "alipay_trade_no"),
        @Result(property = "buyerLogonId", column = "buyer_logon_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "paymentTime", column = "payment_time"),
        @Result(property = "sandboxMode", column = "sandbox_mode"),
        @Result(property = "sandboxBuyerAccount", column = "sandbox_buyer_account"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Payment findByTicketId(@Param("ticketId") Long ticketId);
    
    @Update("UPDATE payments SET status = #{status}, alipay_trade_no = #{alipayTradeNo}, " +
            "buyer_logon_id = #{buyerLogonId}, buyer_user_id = #{buyerUserId}, " +
            "payment_time = #{paymentTime}, updated_at = NOW() " +
            "WHERE payment_number = #{paymentNumber}")
    void updatePaymentStatus(Payment payment);

    @Update("UPDATE payments SET status = 'SUCCESS', alipay_trade_no = #{alipayTradeNo}, " +
            "payment_time = #{paymentTime}, updated_at = NOW() " +
            "WHERE payment_number = #{paymentNumber} AND status = #{expectedStatus}")
    int markPaymentSuccessIfStatusMatches(@Param("paymentNumber") String paymentNumber,
                                          @Param("expectedStatus") String expectedStatus,
                                          @Param("alipayTradeNo") String alipayTradeNo,
                                          @Param("paymentTime") java.time.Instant paymentTime);
    
    @Update("UPDATE payments SET status = #{status}, updated_at = NOW() " +
            "WHERE payment_number = #{paymentNumber}")
    void updatePaymentStatusOnly(@Param("paymentNumber") String paymentNumber, 
                                @Param("status") String status);
    
    @Delete("DELETE FROM payments")
    void deleteAll();
    
    @Select("SELECT * FROM payments WHERE ticket_id = #{ticketId} ORDER BY created_at DESC")
    @Results({
        @Result(property = "paymentNumber", column = "payment_number"),
        @Result(property = "ticketId", column = "ticket_id"),
        @Result(property = "alipayTradeNo", column = "alipay_trade_no"),
        @Result(property = "buyerLogonId", column = "buyer_logon_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "paymentTime", column = "payment_time"),
        @Result(property = "sandboxMode", column = "sandbox_mode"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Payment> findByTicketIdOrderByCreatedAtDesc(@Param("ticketId") Long ticketId);
}
