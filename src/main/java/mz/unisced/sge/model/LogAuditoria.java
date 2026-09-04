package mz.unisced.sge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registo imutavel de auditoria. Toda a accao relevante (autenticacao, criacao,
 * alteracao, tramitacao, despacho, arquivo e tentativas de acesso negadas) fica
 * aqui gravada para efeitos de rastreabilidade.
 */
@Entity
@Table(name = "log_auditoria", indexes = {
        @Index(name = "idx_log_data", columnList = "dataHora"),
        @Index(name = "idx_log_username", columnList = "username")
})
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false, length = 40)
    private String username;

    /** Accao executada, ex.: LOGIN_SUCESSO, EXPEDIENTE_CRIADO, ACESSO_NEGADO. */
    @Column(nullable = false, length = 60)
    private String accao;

    @Column(length = 40)
    private String entidade;

    private Long entidadeId;

    @Column(length = 500)
    private String detalhe;

    @Column(length = 45)
    private String enderecoIp;

    @Column(nullable = false)
    private boolean sucesso = true;

    public LogAuditoria() {
    }

    public LogAuditoria(String username, String accao, String entidade, Long entidadeId,
                        String detalhe, String enderecoIp, boolean sucesso) {
        this.username = username;
        this.accao = accao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhe = detalhe;
        this.enderecoIp = enderecoIp;
        this.sucesso = sucesso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccao() {
        return accao;
    }

    public void setAccao(String accao) {
        this.accao = accao;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(Long entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(String detalhe) {
        this.detalhe = detalhe;
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        this.enderecoIp = enderecoIp;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }
}
