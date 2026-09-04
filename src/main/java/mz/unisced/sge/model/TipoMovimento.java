package mz.unisced.sge.model;

public enum TipoMovimento {
    REGISTO("Registo"),
    ENCAMINHAMENTO("Encaminhamento"),
    DESPACHO("Despacho"),
    ARQUIVAMENTO("Arquivamento"),
    CANCELAMENTO("Cancelamento");

    private final String descricao;

    TipoMovimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
