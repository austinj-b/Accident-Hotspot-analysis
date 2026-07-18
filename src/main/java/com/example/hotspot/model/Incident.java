package com.example.hotspot.model;

public record Incident(long id, String zoneId, String severity, int hour, int weekday, long ts) {
}
