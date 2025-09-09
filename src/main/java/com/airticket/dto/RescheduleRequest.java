package com.airticket.dto;

import java.util.List;

public class RescheduleRequest {
    private Long newFlightId;
    private String reason;
    private List<Long> connectingFlightIds; // Added to support connecting flights in reschedule

    public RescheduleRequest() {}

    public RescheduleRequest(Long newFlightId, String reason) {
        this.newFlightId = newFlightId;
        this.reason = reason;
    }
    
    public RescheduleRequest(Long newFlightId, String reason, List<Long> connectingFlightIds) {
        this.newFlightId = newFlightId;
        this.reason = reason;
        this.connectingFlightIds = connectingFlightIds;
    }

    public Long getNewFlightId() {
        return newFlightId;
    }

    public void setNewFlightId(Long newFlightId) {
        this.newFlightId = newFlightId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<Long> getConnectingFlightIds() {
        return connectingFlightIds;
    }

    public void setConnectingFlightIds(List<Long> connectingFlightIds) {
        this.connectingFlightIds = connectingFlightIds;
    }
}