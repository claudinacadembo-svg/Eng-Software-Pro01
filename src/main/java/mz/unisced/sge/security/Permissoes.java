package mz.unisced.sge.security;

/**
 * Catalogo central de permissoes do sistema. Manter os codigos como constantes
 * evita divergencias entre a base de dados, as anotacoes @PreAuthorize e os
 * templates.
 */
public final class Permissoes {

    // Modulo: Utilizadores
    public static final String UTILIZADOR_LER = "UTILIZADOR_LER";
    public static final String UTILIZADOR_CRIAR = "UTILIZADOR_CRIAR";
    public static final String UTILIZADOR_ACTUALIZAR = "UTILIZADOR_ACTUALIZAR";
    public static final String UTILIZADOR_ELIMINAR = "UTILIZADOR_ELIMINAR";

    // Modulo: Papeis e permissoes (RBAC)
    public static final String PAPEL_LER = "PAPEL_LER";
    public static final String PAPEL_GERIR = "PAPEL_GERIR";

    // Modulo: Expedientes
    public static final String EXPEDIENTE_LER = "EXPEDIENTE_LER";
    public static final String EXPEDIENTE_CRIAR = "EXPEDIENTE_CRIAR";
    public static final String EXPEDIENTE_ACTUALIZAR = "EXPEDIENTE_ACTUALIZAR";
    public static final String EXPEDIENTE_ELIMINAR = "EXPEDIENTE_ELIMINAR";
    public static final String EXPEDIENTE_TRAMITAR = "EXPEDIENTE_TRAMITAR";
    public static final String EXPEDIENTE_DESPACHAR = "EXPEDIENTE_DESPACHAR";
    public static final String EXPEDIENTE_ARQUIVAR = "EXPEDIENTE_ARQUIVAR";

    // Modulo: Auditoria e relatorios
    public static final String AUDITORIA_VER = "AUDITORIA_VER";
    public static final String RELATORIO_VER = "RELATORIO_VER";

    // Papeis pre-definidos
    public static final String PAPEL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String PAPEL_GESTOR_EXPEDIENTE = "GESTOR_EXPEDIENTE";
    public static final String PAPEL_TECNICO = "TECNICO";
    public static final String PAPEL_AUDITOR = "AUDITOR";

    private Permissoes() {
    }
}
