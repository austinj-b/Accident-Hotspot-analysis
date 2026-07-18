package com.example.hotspot.controller;

import com.example.hotspot.model.Incident;
import com.example.hotspot.model.Zone;
import com.example.hotspot.service.IncidentSimulatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentController {

    private final IncidentSimulatorService service;

    public IncidentController(IncidentSimulatorService service) {
        this.service = service;
    }

    @GetMapping("/zones")
    public List<Zone> zones() {
        return IncidentSimulatorService.ZONES;
    }

    @GetMapping("/incidents")
    public List<Incident> incidents() {
        return service.getHistory();
    }

    @GetMapping("/incidents/stream")
    public SseEmitter stream() {
        return service.subscribe();
    }
}
