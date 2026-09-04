package mz.unisced.sge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Ficheiro digitalizado associado a um expediente. */
@Entity
@Table(name = "anexo")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(nullable = false, length = 200)
    private String nomeFicheiro;

    @Column(nullable = false, length = 120)
    private String tipoConteudo;

    @Column(nullable = false)
    private long tamanho;

    @Lob
    @Column(nullable = false)
    private byte[] conteudo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "carregado_por_id", nullable = false)
    private Utilizador carregadoPor;

    @Column(nullable = false)
    private LocalDateTime dataCarregamento = LocalDateTime.now();

    public String getTamanhoLegivel() {
        if (tamanho < 1024) {
            return tamanho + " B";
        }
        if (tamanho < 1024 * 1024) {
            return String.format("%.1f KB", tamanho / 1024.0);
        }
        return String.format("%.1f MB", tamanho / (1024.0 * 1024.0));
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

    public String getNomeFicheiro() {
        return nomeFicheiro;
    }

    public void setNomeFicheiro(String nomeFicheiro) {
        this.nomeFicheiro = nomeFicheiro;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public void setTipoConteudo(String tipoConteudo) {
        this.tipoConteudo = tipoConteudo;
    }

    public long getTamanho() {
        return tamanho;
    }

    public void setTamanho(long tamanho) {
        this.tamanho = tamanho;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public void setConteudo(byte[] conteudo) {
        this.conteudo = conteudo;
    }

    public Utilizador getCarregadoPor() {
        return carregadoPor;
    }

    public void setCarregadoPor(Utilizador carregadoPor) {
        this.carregadoPor = carregadoPor;
    }

    public LocalDateTime getDataCarregamento() {
        return dataCarregamento;
    }

    public void setDataCarregamento(LocalDateTime dataCarregamento) {
        this.dataCarregamento = dataCarregamento;
    }
}
