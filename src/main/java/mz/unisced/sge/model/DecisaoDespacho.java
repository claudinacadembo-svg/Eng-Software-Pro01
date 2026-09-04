package mz.unisced.sge.model;

public enum DecisaoDespacho {
    DEFERIDO("Deferido"),
    INDEFERIDO("Indeferido"),
    ENCAMINHADO("Encaminhado para parecer");

    private final String descricao;

    DecisaoDespacho(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
