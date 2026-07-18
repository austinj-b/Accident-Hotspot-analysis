package com.example.hotspot.service;

import com.example.hotspot.model.Incident;
import com.example.hotspot.model.Zone;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IncidentSimulatorService {

    // Real Chennai junctions used as demo hotspot zones.
    public static final List<Zone> ZONES = List.of(
            new Zone("a", "Kathipara Junction", 13.0122, 80.2077, 22, "Elevated flyover interchange"),
            new Zone("b", "Koyambedu Junction", 13.0694, 80.1948, 18, "Bus terminus congestion point"),
            new Zone("c", "Anna Nagar Roundtana", 13.0850, 80.2101, 14, "Signalized roundabout"),
            new Zone("d", "T Nagar, Pondy Bazaar", 13.0418, 80.2341, 20, "Pedestrian-heavy shopping strip"),
            new Zone("e", "Guindy Signal", 13.0067, 80.2206, 15, "Industrial estate junction"),
            new Zone("f", "Adyar Signal", 13.0067, 80.2570, 12, "Bridge approach road"),
            new Zone("g", "Sholinganallur, OMR", 12.9010, 80.2279, 24, "High-speed IT corridor"),
            new Zone("h", "Perungudi Junction", 12.9698, 80.2422, 13, "Service road merge")
    );

    private static final String[] SEVERITIES = {"Minor", "Serious", "Fatal"};
    private static final double[] SEVERITY_WEIGHTS = {0.58, 0.32, 0.10};
    private static final int MAX_HISTORY = 1000;

    private final CopyOnWriteArrayList<Incident> history = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @PostConstruct
    public void seedHistory() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 260; i++) {
            Zone zone = pickZone(rnd);
            int hour = pickHour(rnd);
            int weekday = rnd.nextInt(7);
            String severity = pickSeverity(rnd, SEVERITY_WEIGHTS);
            long ts = System.currentTimeMillis() - rnd.nextLong(0, 86_400_000L * 30);
            history.add(new Incident(idCounter.getAndIncrement(), zone.id(), severity, hour, weekday, ts));
        }
    }

    @Scheduled(fixedRate = 4000)
    public void generateLiveIncident() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Zone zone = pickZone(rnd);
        LocalTime now = LocalTime.now();
        int weekday = LocalDate.now().getDayOfWeek().getValue() - 1; // Monday = 0
        String severity = pickSeverity(rnd, new double[]{0.55, 0.33, 0.12});
        Incident incident = new Incident(idCounter.getAndIncrement(), zone.id(), severity, now.getHour(), weekday, System.currentTimeMillis());

        history.add(incident);
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        broadcast(incident);
    }

    public List<Incident> getHistory() {
        return List.copyOf(history);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    private void broadcast(Incident incident) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("incident").data(incident));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    private Zone pickZone(ThreadLocalRandom rnd) {
        int totalWeight = ZONES.stream().mapToInt(Zone::weight).sum();
        double r = rnd.nextDouble() * totalWeight;
        double acc = 0;
        for (Zone z : ZONES) {
            acc += z.weight();
            if (r <= acc) return z;
        }
        return ZONES.get(ZONES.size() - 1);
    }

    private int pickHour(ThreadLocalRandom rnd) {
        double[] weights = new double[24];
        double total = 0;
        for (int h = 0; h < 24; h++) {
            double w;
            if (h >= 7 && h <= 9) w = 6;
            else if (h >= 17 && h <= 19) w = 6.5;
            else if (h >= 22 || h <= 4) w = 1.2;
            else w = 2.5;
            weights[h] = w;
            total += w;
        }
        double r = rnd.nextDouble() * total;
        double acc = 0;
        for (int h = 0; h < 24; h++) {
            acc += weights[h];
            if (r <= acc) return h;
        }
        return 12;
    }

    private String pickSeverity(ThreadLocalRandom rnd, double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double r = rnd.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < SEVERITIES.length; i++) {
            acc += weights[i];
            if (r <= acc) return SEVERITIES[i];
        }
        return SEVERITIES[SEVERITIES.length - 1];
    }
}
