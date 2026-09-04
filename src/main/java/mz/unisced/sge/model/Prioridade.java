package mz.unisced.sge.model;

public enum Prioridade {
    NORMAL("Normal"),
    URGENTE("Urgente"),
    MUITO_URGENTE("Muito urgente");

    private final String descricao;

    Prioridade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
