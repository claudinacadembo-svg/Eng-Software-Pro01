package mz.unisced.sge.model;

/** Natureza do expediente quanto ao seu fluxo na instituicao. */
public enum TipoExpediente {
    ENTRADA("Entrada"),
    SAIDA("Saida"),
    INTERNO("Interno");

    private final String descricao;

    TipoExpediente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
