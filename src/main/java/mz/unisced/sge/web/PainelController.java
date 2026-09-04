package mz.unisced.sge.web;

import java.util.LinkedHashMap;
import java.util.Map;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.ExpedienteRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.service.ExpedienteService;
import mz.unisced.sge.service.SessaoUtilizador;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Painel inicial com os indicadores do utilizador autenticado. */
@Controller
public class PainelController {

    private final ExpedienteRepository expedienteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ExpedienteService expedienteService;
    private final SessaoUtilizador sessao;

    public PainelController(ExpedienteRepository expedienteRepository,
                            UtilizadorRepository utilizadorRepository,
                            ExpedienteService expedienteService,
                            SessaoUtilizador sessao) {
        this.expedienteRepository = expedienteRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.expedienteService = expedienteService;
        this.sessao = sessao;
    }

    @GetMapping("/")
    public String painel(Model modelo) {
        Utilizador utilizador = sessao.actual();

        modelo.addAttribute("utilizador", utilizador);
        modelo.addAttribute("totalExpedientes", expedienteRepository.count());
        modelo.addAttribute("registados", expedienteRepository.countByEstado(EstadoExpediente.REGISTADO));
        modelo.addAttribute("emTramitacao", expedienteRepository.countByEstado(EstadoExpediente.EM_TRAMITACAO));
        modelo.addAttribute("despachados", expedienteRepository.countByEstado(EstadoExpediente.DESPACHADO));
        modelo.addAttribute("arquivados", expedienteRepository.countByEstado(EstadoExpediente.ARQUIVADO));
        modelo.addAttribute("utilizadoresActivos", utilizadorRepository.countByActivoTrue());
        modelo.addAttribute("meusExpedientes", expedienteService.pendentesDe(utilizador));
        modelo.addAttribute("distribuicao", distribuicaoPorEstado());
        return "painel";
    }

    /** Contagem de expedientes por estado, para as barras de proporcao do painel. */
    private Map<String, Long> distribuicaoPorEstado() {
        Map<String, Long> distribuicao = new LinkedHashMap<>();
        for (Object[] linha : expedienteRepository.contarPorEstado()) {
            distribuicao.put(((EstadoExpediente) linha[0]).getDescricao(), ((Number) linha[1]).longValue());
        }
        return distribuicao;
    }
}
