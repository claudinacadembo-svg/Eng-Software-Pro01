package mz.unisced.sge.model;

/**
 * Nivel de confidencialidade do expediente. Complementa o RBAC: mesmo com a
 * permissao EXPEDIENTE_LER, um expediente CONFIDENCIAL so e visivel ao autor,
 * ao responsavel actual e a quem tenha permissao de auditoria.
 */
public enum NivelConfidencialidade {
    PUBLICO("Publico"),
    RESTRITO("Restrito"),
    CONFIDENCIAL("Confidencial");

    private final String descricao;

    NivelConfidencialidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
