package mz.unisced.sge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** Utilizador do sistema. Recebe zero ou mais papeis (RBAC). */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome de utilizador e obrigatorio")
    @Size(min = 3, max = 40, message = "O nome de utilizador deve ter entre 3 e 40 caracteres")
    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @NotBlank(message = "O nome completo e obrigatorio")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nomeCompleto;

    @NotBlank(message = "O email e obrigatorio")
    @Email(message = "Email invalido")
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 120)
    private String palavraPasse;

    @Size(max = 80)
    @Column(length = 80)
    private String departamento;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    private LocalDateTime ultimoAcesso;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "utilizador_papel",
            joinColumns = @JoinColumn(name = "utilizador_id"),
            inverseJoinColumns = @JoinColumn(name = "papel_id"))
    private Set<Papel> papeis = new LinkedHashSet<>();

    public Utilizador() {
    }

    public Utilizador(String username, String nomeCompleto, String email, String palavraPasse) {
        this.username = username;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.palavraPasse = palavraPasse;
    }

    /** Verifica se algum dos papeis atribuidos concede a permissao indicada. */
    public boolean temPermissao(String codigo) {
        return papeis.stream().anyMatch(papel -> papel.temPermissao(codigo));
    }

    public boolean temPapel(String nomePapel) {
        return papeis.stream().anyMatch(papel -> papel.getNome().equals(nomePapel));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPalavraPasse() {
        return palavraPasse;
    }

    public void setPalavraPasse(String palavraPasse) {
        this.palavraPasse = palavraPasse;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(LocalDateTime ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    public Set<Papel> getPapeis() {
        return papeis;
    }

    public void setPapeis(Set<Papel> papeis) {
        this.papeis = papeis;
    }

    @Override
    public String toString() {
        return nomeCompleto + " (" + username + ")";
    }
}
