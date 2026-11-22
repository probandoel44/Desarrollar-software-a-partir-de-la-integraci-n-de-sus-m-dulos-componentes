package com.appmiparqueo.appmiparqueo.controller;

import javax.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.appmiparqueo.appmiparqueo.service.DisponibilidadEventPublisher;

@Controller
@RequestMapping("/view/cliente/disponibilidad")
public class DisponibilidadController {
    private final DisponibilidadEventPublisher eventPublisher;

    public DisponibilidadController(DisponibilidadEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<SseEmitter> eventos(HttpSession session) {
        if (session.getAttribute("authenticatedUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(eventPublisher.subscribe());
    }
}