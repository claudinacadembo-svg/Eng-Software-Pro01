package mz.unisced.sge.dto;

import java.util.Map;

/** Dados agregados apresentados no modulo de relatorios. */
public record RelatorioExpedientes(
        long total,
        long emCurso,
        long arquivados,
        long atrasados,
        Map<String, Long> porEstado,
        Map<String, Long> porTipo,
        Map<String, Long> porPrioridade,
        Map<String, Long> porResponsavel) {
}
