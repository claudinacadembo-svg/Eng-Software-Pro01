package mz.unisced.sge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Movimento de um expediente entre utilizadores. O conjunto das tramitacoes de
 * um expediente constitui o seu historico completo (rastreabilidade).
 */
@Entity
@Table(name = "tramitacao")
public class Tramitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimento tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origem_id")
    private Utilizador origem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destino_id")
    private Utilizador destino;

    @Column(length = 1000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoExpediente estadoResultante;

    @Column(nullable = false)
    private LocalDateTime dataMovimento = LocalDateTime.now();

    public Tramitacao() {
    }

    public Tramitacao(TipoMovimento tipo, Utilizador origem, Utilizador destino, String observacao,
                      EstadoExpediente estadoResultante) {
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.observacao = observacao;
        this.estadoResultante = estadoResultante;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expediente getExpediente() {
        return expediente;
    }

    public void setExpediente(Expediente expediente) {
        this.expediente = expediente;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public Utilizador getOrigem() {
        return origem;
    }

    public void setOrigem(Utilizador origem) {
        this.origem = origem;
    }

    public Utilizador getDestino() {
        return destino;
    }

    public void setDestino(Utilizador destino) {
        this.destino = destino;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public EstadoExpediente getEstadoResultante() {
        return estadoResultante;
    }

    public void setEstadoResultante(EstadoExpediente estadoResultante) {
        this.estadoResultante = estadoResultante;
    }

    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }
}
