package mz.unisced.sge.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mz.unisced.sge.dto.RelatorioExpedientes;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.repository.ExpedienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Produz os indicadores e a exportacao do modulo de relatorios. */
@Service
public class RelatorioService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ExpedienteRepository expedienteRepository;
    private final AuditoriaService auditoriaService;

    public RelatorioService(ExpedienteRepository expedienteRepository, AuditoriaService auditoriaService) {
        this.expedienteRepository = expedienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAuthority('RELATORIO_VER')")
    @Transactional(readOnly = true)
    public RelatorioExpedientes gerar() {
        long total = expedienteRepository.count();
        long arquivados = expedienteRepository.countByEstado(EstadoExpediente.ARQUIVADO);
        long cancelados = expedienteRepository.countByEstado(EstadoExpediente.CANCELADO);
        long atrasados = expedienteRepository.findAll().stream().filter(Expediente::isAtrasado).count();

        return new RelatorioExpedientes(
                total,
                total - arquivados - cancelados,
                arquivados,
                atrasados,
                agrupar(expedienteRepository.contarPorEstado()),
                agrupar(expedienteRepository.contarPorTipo()),
                agrupar(expedienteRepository.contarPorPrioridade()),
                agrupar(expedienteRepository.contarPorResponsavel()));
    }

    @PreAuthorize("hasAuthority('RELATORIO_VER')")
    @Transactional(readOnly = true)
    public String exportarCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Numero;Assunto;Tipo;Estado;Prioridade;Confidencialidade;Remetente;Responsavel;Data de registo\n");
        for (Expediente e : expedienteRepository.findAll()) {
            csv.append(escapar(e.getNumeroRegisto())).append(';')
               .append(escapar(e.getAssunto())).append(';')
               .append(e.getTipo().getDescricao()).append(';')
               .append(e.getEstado().getDescricao()).append(';')
               .append(e.getPrioridade().getDescricao()).append(';')
               .append(e.getConfidencialidade().getDescricao()).append(';')
               .append(escapar(e.getRemetente())).append(';')
               .append(e.getResponsavelActual() == null ? "-" : escapar(e.getResponsavelActual().getNomeCompleto()))
               .append(';')
               .append(e.getDataRegisto().format(FORMATO_DATA))
               .append('\n');
        }
        auditoriaService.registar("RELATORIO_EXPORTADO", "Expediente", null,
                "Exportacao CSV da lista de expedientes");
        return csv.toString();
    }

    private Map<String, Long> agrupar(List<Object[]> linhas) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Object[] linha : linhas) {
            String chave = linha[0] instanceof Enum<?> valor ? descricaoDe(valor) : String.valueOf(linha[0]);
            resultado.put(chave, ((Number) linha[1]).longValue());
        }
        return resultado;
    }

    private String descricaoDe(Enum<?> valor) {
        try {
            return (String) valor.getClass().getMethod("getDescricao").invoke(valor);
        } catch (ReflectiveOperationException excepcao) {
            return valor.name();
        }
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(';', ',').replace('\n', ' ');
    }
}
