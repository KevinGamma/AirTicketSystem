package com.airticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "service.fees")
public class ServiceFeeConfig {
    
    private BigDecimal rescheduleBaseFee = new BigDecimal("200.00");
    private BigDecimal reschedulePercentageFee = new BigDecimal("0.10");
    private BigDecimal refundBaseFee = new BigDecimal("300.00");
    private BigDecimal refundPercentageFee = new BigDecimal("0.15");

    private int highFeeThresholdHours = 24;
    private BigDecimal highRescheduleBaseFee = new BigDecimal("400.00");
    private BigDecimal highReschedulePercentageFee = new BigDecimal("0.20");
    private BigDecimal highRefundBaseFee = new BigDecimal("500.00");
    private BigDecimal highRefundPercentageFee = new BigDecimal("0.25");

    public BigDecimal getRescheduleBaseFee() {
        return rescheduleBaseFee;
    }

    public void setRescheduleBaseFee(BigDecimal rescheduleBaseFee) {
        this.rescheduleBaseFee = rescheduleBaseFee;
    }

    public BigDecimal getReschedulePercentageFee() {
        return reschedulePercentageFee;
    }

    public void setReschedulePercentageFee(BigDecimal reschedulePercentageFee) {
        this.reschedulePercentageFee = reschedulePercentageFee;
    }

    public BigDecimal getRefundBaseFee() {
        return refundBaseFee;
    }

    public void setRefundBaseFee(BigDecimal refundBaseFee) {
        this.refundBaseFee = refundBaseFee;
    }

    public BigDecimal getRefundPercentageFee() {
        return refundPercentageFee;
    }

    public void setRefundPercentageFee(BigDecimal refundPercentageFee) {
        this.refundPercentageFee = refundPercentageFee;
    }

    public int getHighFeeThresholdHours() {
        return highFeeThresholdHours;
    }

    public void setHighFeeThresholdHours(int highFeeThresholdHours) {
        this.highFeeThresholdHours = highFeeThresholdHours;
    }

    public BigDecimal getHighRescheduleBaseFee() {
        return highRescheduleBaseFee;
    }

    public void setHighRescheduleBaseFee(BigDecimal highRescheduleBaseFee) {
        this.highRescheduleBaseFee = highRescheduleBaseFee;
    }

    public BigDecimal getHighReschedulePercentageFee() {
        return highReschedulePercentageFee;
    }

    public void setHighReschedulePercentageFee(BigDecimal highReschedulePercentageFee) {
        this.highReschedulePercentageFee = highReschedulePercentageFee;
    }

    public BigDecimal getHighRefundBaseFee() {
        return highRefundBaseFee;
    }

    public void setHighRefundBaseFee(BigDecimal highRefundBaseFee) {
        this.highRefundBaseFee = highRefundBaseFee;
    }

    public BigDecimal getHighRefundPercentageFee() {
        return highRefundPercentageFee;
    }

    public void setHighRefundPercentageFee(BigDecimal highRefundPercentageFee) {
        this.highRefundPercentageFee = highRefundPercentageFee;
    }
}