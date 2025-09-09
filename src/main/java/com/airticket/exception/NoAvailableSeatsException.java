package com.airticket.exception;

public class NoAvailableSeatsException extends RuntimeException {
    public NoAvailableSeatsException(String message) {
        super(message);
    }
    
    public NoAvailableSeatsException() {
        super("No available seats on this flight");
    }
}