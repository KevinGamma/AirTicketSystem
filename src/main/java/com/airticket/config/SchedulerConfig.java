package com.airticket.config;

import com.airticket.service.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    
    @Autowired
    private FlightService flightService;

    @Scheduled(fixedRate = 60000)
    public void updateFlightStatuses() {
        flightService.updateAllFlightStatuses();
    }
}