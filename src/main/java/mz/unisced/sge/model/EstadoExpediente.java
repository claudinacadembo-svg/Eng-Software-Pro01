package mz.unisced.sge.model;

/** Estados do ciclo de vida do expediente: entrada, tramitacao, despacho e arquivo. */
public enum EstadoExpediente {
    REGISTADO("Registado"),
    EM_TRAMITACAO("Em tramitacao"),
    DESPACHADO("Despachado"),
    ARQUIVADO("Arquivado"),
    CANCELADO("Cancelado");

    private final String descricao;

    EstadoExpediente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Um expediente arquivado ou cancelado ja nao admite movimentacao. */
    public boolean isFinal() {
        return this == ARQUIVADO || this == CANCELADO;
    }
}
