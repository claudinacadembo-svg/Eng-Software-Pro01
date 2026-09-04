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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Papel (role) do modelo RBAC. Agrega um conjunto de permissoes que sao
 * atribuidas em bloco aos utilizadores que desempenham essa funcao.
 */
@Entity
@Table(name = "papel")
public class Papel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nome;

    @Column(nullable = false, length = 150)
    private String descricao;

    /** Papeis de sistema nao podem ser eliminados, para garantir o arranque do RBAC. */
    @Column(nullable = false)
    private boolean sistema = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "papel_permissao",
            joinColumns = @JoinColumn(name = "papel_id"),
            inverseJoinColumns = @JoinColumn(name = "permissao_id"))
    private Set<Permissao> permissoes = new LinkedHashSet<>();

    public Papel() {
    }

    public Papel(String nome, String descricao, boolean sistema) {
        this.nome = nome;
        this.descricao = descricao;
        this.sistema = sistema;
    }

    public boolean temPermissao(String codigo) {
        return permissoes.stream().anyMatch(p -> p.getCodigo().equals(codigo));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isSistema() {
        return sistema;
    }

    public void setSistema(boolean sistema) {
        this.sistema = sistema;
    }

    public Set<Permissao> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(Set<Permissao> permissoes) {
        this.permissoes = permissoes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Papel)) {
            return false;
        }
        return Objects.equals(nome, ((Papel) o).nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome);
    }

    @Override
    public String toString() {
        return nome;
    }
}
