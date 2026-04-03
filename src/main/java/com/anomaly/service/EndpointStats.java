package com.anomaly.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EndpointStats {

    public static class EventSnapshot {
        public final OffsetDateTime timestamp;
        public final Long latency;
        public final boolean isError;

        public EventSnapshot(OffsetDateTime timestamp, Long latency, boolean isError) {
            this.timestamp = timestamp;
            this.latency = latency;
            this.isError = isError;
        }
    }

    private final ConcurrentLinkedDeque<EventSnapshot> window = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<EventSnapshot> longTermWindow = new ConcurrentLinkedDeque<>();

    public void addEvent(OffsetDateTime timestamp, Long latency, boolean isError) {
        if (timestamp == null) return;
        EventSnapshot snapshot = new EventSnapshot(timestamp, latency, isError);
        window.addLast(snapshot);
        longTermWindow.addLast(snapshot);
        prune(timestamp);
    }

    private void prune(OffsetDateTime now) {
        OffsetDateTime windowStart = now.minusSeconds(60);
        while (!window.isEmpty() && window.peekFirst().timestamp.isBefore(windowStart)) {
            window.removeFirst();
        }

        // Long term keeps 6 minutes total (5 min average + 1 min exclusion)
        OffsetDateTime longTermStart = now.minusMinutes(6);
        while (!longTermWindow.isEmpty() && longTermWindow.peekFirst().timestamp.isBefore(longTermStart)) {
            longTermWindow.removeFirst();
        }
    }

    public boolean isBaselineReady() {
        if (window.size() < 30) return false;
        EventSnapshot first = window.peekFirst();
        EventSnapshot last = window.peekLast();
        if (first == null || last == null || first.timestamp == null || last.timestamp == null) return false;
        long durationMs = ChronoUnit.MILLIS.between(first.timestamp, last.timestamp);
        return durationMs >= 15000;
    }

    public boolean hasSufficientTraffic() {
        return window.size() >= 10;
    }

    public double getRate() {
        if (window.isEmpty()) return 0.0;
        EventSnapshot first = window.peekFirst();
        EventSnapshot last = window.peekLast();
        if (first == null || last == null || first.timestamp == null || last.timestamp == null) return 0.0;
        long durationSec = ChronoUnit.SECONDS.between(first.timestamp, last.timestamp);
        double duration = Math.max(1.0, durationSec);
        return window.size() / duration;
    }

    public double getErrorRate() {
        if (window.isEmpty()) return 0.0;
        long errCount = 0;
        for (EventSnapshot e : window) {
            if (e.isError) errCount++;
        }
        return (double) errCount / window.size();
    }

    public double getLatencyAverage() {
        long sum = 0;
        int count = 0;
        for (EventSnapshot e : window) {
            if (e.latency != null) {
                sum += e.latency;
                count++;
            }
        }
        return count == 0 ? 0.0 : (double) sum / count;
    }

    public double getLongTermRate() {
        if (longTermWindow.isEmpty()) return 0.0;
        EventSnapshot last = longTermWindow.peekLast();
        if (last == null || last.timestamp == null) return 0.0;
        OffsetDateTime excludeStart = last.timestamp.minusSeconds(60);
        int totalEvents = 0;
        OffsetDateTime oldest = null;
        OffsetDateTime newest = null;

        for (EventSnapshot e : longTermWindow) {
            if (e.timestamp != null && e.timestamp.isBefore(excludeStart)) {
                totalEvents++;
                if (oldest == null) oldest = e.timestamp;
                newest = e.timestamp;
            }
        }

        if (totalEvents == 0) return 0.0;

        long durationSec = ChronoUnit.SECONDS.between(oldest, newest);
        double duration = Math.max(1.0, durationSec);
        return totalEvents / duration;
    }

    public double getLongTermErrorRate() {
        if (longTermWindow.isEmpty()) return 0.0;
        EventSnapshot last = longTermWindow.peekLast();
        if (last == null || last.timestamp == null) return 0.0;
        OffsetDateTime excludeStart = last.timestamp.minusSeconds(60);
        long errCount = 0;
        long totalCount = 0;

        for (EventSnapshot e : longTermWindow) {
            if (e.timestamp != null && e.timestamp.isBefore(excludeStart)) {
                totalCount++;
                if (e.isError) errCount++;
            }
        }

        if (totalCount == 0) return 0.0;
        return (double) errCount / totalCount;
    }

    public double getLongTermLatencyAverage() {
        if (longTermWindow.isEmpty()) return 0.0;
        EventSnapshot last = longTermWindow.peekLast();
        if (last == null || last.timestamp == null) return 0.0;
        OffsetDateTime excludeStart = last.timestamp.minusSeconds(60);
        long sum = 0;
        int count = 0;

        for (EventSnapshot e : longTermWindow) {
            if (e.timestamp != null && e.timestamp.isBefore(excludeStart)) {
                if (e.latency != null) {
                    sum += e.latency;
                    count++;
                }
            }
        }

        if (count == 0) return 0.0;
        return (double) sum / count;
    }
}
