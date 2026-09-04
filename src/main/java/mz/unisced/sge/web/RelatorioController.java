package mz.unisced.sge.web;

import java.nio.charset.StandardCharsets;
import mz.unisced.sge.service.RelatorioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Relatorios e indicadores do sistema. */
@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping
    public String relatorio(Model modelo) {
        modelo.addAttribute("relatorio", relatorioService.gerar());
        return "relatorios/expedientes";
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv() {
        byte[] conteudo = relatorioService.exportarCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expedientes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(conteudo);
    }
}
