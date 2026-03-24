package com.airticket.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseOptimizationService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseOptimizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void optimizeDatabase() {
        ensureAdminApprovalRequestColumns();
        normalizeExistingData();

        ensureIndex("airports", "idx_airports_city", "CREATE INDEX idx_airports_city ON airports (city)");

        ensureIndex(
            "flights",
            "idx_flights_route_status_departure_time",
            "CREATE INDEX idx_flights_route_status_departure_time ON flights (departure_airport_id, arrival_airport_id, status, departure_time)"
        );
        ensureIndex(
            "flights",
            "idx_flights_departure_airport_departure_time",
            "CREATE INDEX idx_flights_departure_airport_departure_time ON flights (departure_airport_id, departure_time)"
        );
        ensureIndex(
            "flights",
            "idx_flights_arrival_airport_departure_time",
            "CREATE INDEX idx_flights_arrival_airport_departure_time ON flights (arrival_airport_id, departure_time)"
        );
        ensureIndex(
            "flights",
            "idx_flights_departure_time_status",
            "CREATE INDEX idx_flights_departure_time_status ON flights (departure_time, status)"
        );

        ensureIndex(
            "tickets",
            "idx_tickets_user_deleted_created_at",
            "CREATE INDEX idx_tickets_user_deleted_created_at ON tickets (user_id, deleted_by_user, created_at)"
        );
        ensureIndex(
            "tickets",
            "idx_tickets_status_booking_time",
            "CREATE INDEX idx_tickets_status_booking_time ON tickets (status, booking_time)"
        );
        ensureIndex(
            "tickets",
            "idx_tickets_status_flight_id",
            "CREATE INDEX idx_tickets_status_flight_id ON tickets (status, flight_id)"
        );

        ensureIndex(
            "notifications",
            "idx_notifications_user_created_at",
            "CREATE INDEX idx_notifications_user_created_at ON notifications (user_id, created_at)"
        );
        ensureIndex(
            "notifications",
            "idx_notifications_scheduled_sent",
            "CREATE INDEX idx_notifications_scheduled_sent ON notifications (scheduled_time, sent_time)"
        );
        ensureIndex(
            "notifications",
            "idx_notifications_ticket_type",
            "CREATE INDEX idx_notifications_ticket_type ON notifications (ticket_id, notification_type)"
        );
        ensureUniqueIndex(
            "payments",
            "payment_number",
            "uk_payments_payment_number",
            "CREATE UNIQUE INDEX uk_payments_payment_number ON payments (payment_number)"
        );
    }

    private void normalizeExistingData() {
        jdbcTemplate.update("UPDATE tickets SET deleted_by_user = FALSE WHERE deleted_by_user IS NULL");
    }

    private void ensureAdminApprovalRequestColumns() {
        ensureColumn(
            "admin_approval_requests",
            "total_amount",
            "ALTER TABLE admin_approval_requests ADD COLUMN total_amount DECIMAL(10,2) NULL"
        );
        ensureColumn(
            "admin_approval_requests",
            "refund_amount",
            "ALTER TABLE admin_approval_requests ADD COLUMN refund_amount DECIMAL(10,2) NULL"
        );
        ensureColumn(
            "admin_approval_requests",
            "payment_status",
            "ALTER TABLE admin_approval_requests ADD COLUMN payment_status VARCHAR(50) NULL"
        );
        ensureColumn(
            "admin_approval_requests",
            "payment_number",
            "ALTER TABLE admin_approval_requests ADD COLUMN payment_number VARCHAR(100) NULL"
        );
        ensureColumn(
            "admin_approval_requests",
            "refund_number",
            "ALTER TABLE admin_approval_requests ADD COLUMN refund_number VARCHAR(100) NULL"
        );
    }

    private void ensureIndex(String tableName, String indexName, String createIndexSql) {
        Integer existingIndexes = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
            Integer.class,
            tableName,
            indexName
        );

        if (existingIndexes != null && existingIndexes == 0) {
            jdbcTemplate.execute(createIndexSql);
        }
    }

    private void ensureUniqueIndex(String tableName, String columnName, String indexName, String createIndexSql) {
        Integer existingUniqueIndexes = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? AND non_unique = 0",
            Integer.class,
            tableName,
            columnName
        );

        if (existingUniqueIndexes != null && existingUniqueIndexes > 0) {
            return;
        }

        Integer duplicateValues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (" +
                "SELECT " + columnName + " FROM " + tableName + " " +
                "WHERE " + columnName + " IS NOT NULL " +
                "GROUP BY " + columnName + " HAVING COUNT(*) > 1" +
            ") duplicated_values",
            Integer.class
        );

        if (duplicateValues != null && duplicateValues > 0) {
            throw new IllegalStateException(
                "Cannot create unique index " + indexName + " on " + tableName + "." + columnName +
                    " because duplicate values already exist"
            );
        }

        jdbcTemplate.execute(createIndexSql);
    }

    private void ensureColumn(String tableName, String columnName, String alterTableSql) {
        Integer existingColumns = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
            Integer.class,
            tableName,
            columnName
        );

        if (existingColumns != null && existingColumns == 0) {
            jdbcTemplate.execute(alterTableSql);
        }
    }
}
