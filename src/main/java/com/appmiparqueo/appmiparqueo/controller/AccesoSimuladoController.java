package com.appmiparqueo.appmiparqueo.controller;

import com.appmiparqueo.appmiparqueo.service.ParqueaderoService;
import com.appmiparqueo.appmiparqueo.service.SimuladorAccesoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccesoSimuladoController {
    private final SimuladorAccesoService simuladorAccesoService;
    private final ParqueaderoService parqueaderoService;

    public AccesoSimuladoController(SimuladorAccesoService simuladorAccesoService,
            ParqueaderoService parqueaderoService) {
        this.simuladorAccesoService = simuladorAccesoService;
        this.parqueaderoService = parqueaderoService;
    }

    @PostMapping("/movimientos/acceso-simulado")
    public String procesarLectura(@RequestParam String lectura, @RequestParam String tipoLectura,
            @RequestParam String operacion, @RequestParam(required = false) String metodoPago,
            RedirectAttributes redirectAttributes) {
        try {
            SimuladorAccesoService.Resultado resultado = simuladorAccesoService
                    .procesarLectura(lectura, tipoLectura, operacion, metodoPago);
            redirectAttributes.addFlashAttribute("mensaje", "Lectura válida para " + resultado.placa() + ". "
                    + resultado.estadoBarrera());
        } catch (IllegalArgumentException ex) {
            parqueaderoService.registrarIncidente("Error de acceso simulado", ex.getMessage());
            redirectAttributes.addFlashAttribute("error", ex.getMessage()
                    + " Se registró el incidente para revisión administrativa.");
        }
        return "redirect:/movimientos";
    }
}