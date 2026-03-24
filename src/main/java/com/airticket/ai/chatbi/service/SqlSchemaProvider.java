package com.airticket.ai.chatbi.service;

import org.springframework.stereotype.Component;

@Component
public class SqlSchemaProvider {

    private static final String BUSINESS_GUIDE = """
        业务语义映射：
        1. “航司/航空公司”信息不在 flights.airline 字段中，真实字段来自 airlines.name，需通过 flights.airline_id = airlines.id 关联。
        2. “出发地/起飞机场/起飞机场城市”通常来自 airports 表，通过 flights.departure_airport_id = airports.id 关联。
        3. “目的地/到达机场/到达城市”通常来自 airports 表，通过 flights.arrival_airport_id = airports.id 关联。
        4. “订单/票务订单”在本系统中优先映射为 tickets；涉及支付金额或支付方式时再关联 payments。
        5. “票务收入/航司收入/销售收入/营收排行”默认优先使用已支付票据：
           - tickets.status = 'PAID' 时优先聚合 tickets.price
           - 只有用户明确要求“支付金额”“支付流水”“支付方式”“支付渠道”时，才优先使用 payments.amount
        6. “票价/均价/平均票价/航班价格”优先使用 flights.price，除非用户明确要求“已支付票价”“已成交票价”“订单票价”。
        7. “出票量/订单量/购票量”优先统计 tickets。
        8. 时间字段优先使用真实字段名：
           - tickets.created_at / tickets.booking_time / tickets.payment_time
           - payments.created_at / payments.payment_time
           - flights.departure_time / flights.arrival_time
        9. 不要使用不存在的 orders、origin、destination、airline、create_time 等演示字段。
        10. payments.payment_method = 'RESCHEDULE_TRANSFER' 表示改签支付承接的内部修复记录，金额通常为 0，默认不纳入用户支付方式分析和收入统计。
        """;

    private static final String FLIGHTS_DDL = """
        CREATE TABLE flights (
            id BIGINT PRIMARY KEY,
            flight_number VARCHAR(64),
            airline_id BIGINT,
            departure_airport_id BIGINT,
            arrival_airport_id BIGINT,
            departure_time DATETIME,
            arrival_time DATETIME,
            total_seats INT,
            available_seats INT,
            price DECIMAL(10, 2),
            status VARCHAR(32),
            aircraft_type VARCHAR(64),
            created_at DATETIME,
            updated_at DATETIME
        );
        """;

    private static final String TICKETS_DDL = """
        CREATE TABLE tickets (
            id BIGINT PRIMARY KEY,
            ticket_number VARCHAR(64),
            user_id BIGINT,
            flight_id BIGINT,
            seat_number VARCHAR(32),
            passenger_name VARCHAR(64),
            passenger_id_number VARCHAR(64),
            ticket_type VARCHAR(32),
            price DECIMAL(10, 2),
            status VARCHAR(32),
            booking_time DATETIME,
            payment_time DATETIME,
            created_at DATETIME,
            updated_at DATETIME,
            deleted_by_user BOOLEAN,
            service_fee DECIMAL(10, 2),
            original_ticket_id BIGINT,
            rescheduled_to_ticket_id BIGINT,
            is_original_ticket BOOLEAN
        );
        """;

    private static final String PAYMENTS_DDL = """
        CREATE TABLE payments (
            id BIGINT PRIMARY KEY,
            ticket_id BIGINT,
            payment_number VARCHAR(64),
            alipay_trade_no VARCHAR(128),
            amount DECIMAL(10, 2),
            currency VARCHAR(16),
            payment_method VARCHAR(32),
            status VARCHAR(32),
            subject VARCHAR(255),
            body VARCHAR(255),
            buyer_logon_id VARCHAR(128),
            buyer_user_id VARCHAR(128),
            payment_time DATETIME,
            sandbox_mode BOOLEAN,
            sandbox_buyer_account VARCHAR(128),
            created_at DATETIME,
            updated_at DATETIME
        );
        """;

    private static final String AIRLINES_DDL = """
        CREATE TABLE airlines (
            id BIGINT PRIMARY KEY,
            code VARCHAR(32),
            name VARCHAR(128),
            full_name VARCHAR(255),
            logo_url VARCHAR(255),
            description VARCHAR(255),
            active BOOLEAN,
            created_at DATETIME,
            updated_at DATETIME
        );
        """;

    private static final String AIRPORTS_DDL = """
        CREATE TABLE airports (
            id BIGINT PRIMARY KEY,
            code VARCHAR(32),
            name VARCHAR(128),
            city VARCHAR(128),
            country VARCHAR(128),
            timezone VARCHAR(64),
            created_at DATETIME
        );
        """;

    public String getSchema() {
        return String.join(
            System.lineSeparator() + System.lineSeparator(),
            BUSINESS_GUIDE,
            FLIGHTS_DDL,
            TICKETS_DDL,
            PAYMENTS_DDL,
            AIRLINES_DDL,
            AIRPORTS_DDL
        );
    }
}
