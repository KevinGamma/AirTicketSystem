package com.airticket.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationResolveResult {

    private String originalInput;
    private boolean resolved;
    private boolean cityLevelSearch;
    private boolean clarificationRequired;
    private String canonicalQuery;
    private String message;
    private List<AirportCandidateView> candidates = new ArrayList<>();

    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public boolean isCityLevelSearch() {
        return cityLevelSearch;
    }

    public void setCityLevelSearch(boolean cityLevelSearch) {
        this.cityLevelSearch = cityLevelSearch;
    }

    public boolean isClarificationRequired() {
        return clarificationRequired;
    }

    public void setClarificationRequired(boolean clarificationRequired) {
        this.clarificationRequired = clarificationRequired;
    }

    public String getCanonicalQuery() {
        return canonicalQuery;
    }

    public void setCanonicalQuery(String canonicalQuery) {
        this.canonicalQuery = canonicalQuery;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AirportCandidateView> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AirportCandidateView> candidates) {
        this.candidates = candidates;
    }
}
