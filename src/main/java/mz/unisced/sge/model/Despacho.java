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
 * Decisao formal proferida sobre um expediente por quem detem a permissao
 * EXPEDIENTE_DESPACHAR. Um expediente pode ter varios despachos ao longo da
 * sua tramitacao.
 */
@Entity
@Table(name = "despacho")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id", nullable = false)
    private Utilizador autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisaoDespacho decisao;

    @Column(nullable = false, length = 1500)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime dataDespacho = LocalDateTime.now();

    public Despacho() {
    }

    public Despacho(Utilizador autor, DecisaoDespacho decisao, String texto) {
        this.autor = autor;
        this.decisao = decisao;
        this.texto = texto;
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

    public Utilizador getAutor() {
        return autor;
    }

    public void setAutor(Utilizador autor) {
        this.autor = autor;
    }

    public DecisaoDespacho getDecisao() {
        return decisao;
    }

    public void setDecisao(DecisaoDespacho decisao) {
        this.decisao = decisao;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getDataDespacho() {
        return dataDespacho;
    }

    public void setDataDespacho(LocalDateTime dataDespacho) {
        this.dataDespacho = dataDespacho;
    }
}
