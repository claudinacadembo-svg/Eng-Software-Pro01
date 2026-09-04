package mz.unisced.sge.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import mz.unisced.sge.service.AuditoriaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Consulta dos registos de auditoria. */
@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    private static final int TAMANHO_PAGINA = 20;

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String username,
                         @RequestParam(required = false) String accao,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model modelo) {
        LocalDateTime instanteInicial = inicio == null ? null : inicio.atStartOfDay();
        LocalDateTime instanteFinal = fim == null ? null : LocalDateTime.of(fim, LocalTime.MAX);

        modelo.addAttribute("pagina", auditoriaService.pesquisar(username, accao, instanteInicial,
                instanteFinal, PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA)));
        modelo.addAttribute("accoes", auditoriaService.accoesRegistadas());
        modelo.addAttribute("username", username);
        modelo.addAttribute("accaoSeleccionada", accao);
        modelo.addAttribute("inicio", inicio);
        modelo.addAttribute("fim", fim);
        modelo.addAttribute("totalRegistos", auditoriaService.totalRegistos());
        modelo.addAttribute("acessosNegados", auditoriaService.totalAcessosNegados());
        return "auditoria/lista";
    }
}
