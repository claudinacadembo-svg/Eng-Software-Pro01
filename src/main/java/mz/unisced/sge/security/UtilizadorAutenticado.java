package mz.unisced.sge.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import mz.unisced.sge.model.Papel;
import mz.unisced.sge.model.Permissao;
import mz.unisced.sge.model.Utilizador;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador entre a entidade Utilizador e o modelo de seguranca do Spring.
 *
 * <p>Traduz o RBAC guardado na base de dados em autoridades: cada papel origina
 * uma autoridade com prefixo {@code ROLE_} e cada permissao do papel origina uma
 * autoridade com o proprio codigo (ex.: {@code EXPEDIENTE_DESPACHAR}). E esta
 * traducao que permite usar {@code hasRole(...)} e {@code hasAuthority(...)}
 * nas regras de acesso.
 */
public class UtilizadorAutenticado implements UserDetails {

    private final Utilizador utilizador;
    private final List<GrantedAuthority> autoridades;

    public UtilizadorAutenticado(Utilizador utilizador) {
        this.utilizador = utilizador;
        this.autoridades = construirAutoridades(utilizador);
    }

    private static List<GrantedAuthority> construirAutoridades(Utilizador utilizador) {
        List<GrantedAuthority> lista = new ArrayList<>();
        for (Papel papel : utilizador.getPapeis()) {
            lista.add(new SimpleGrantedAuthority("ROLE_" + papel.getNome()));
            for (Permissao permissao : papel.getPermissoes()) {
                SimpleGrantedAuthority autoridade = new SimpleGrantedAuthority(permissao.getCodigo());
                if (!lista.contains(autoridade)) {
                    lista.add(autoridade);
                }
            }
        }
        return List.copyOf(lista);
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public String getNomeCompleto() {
        return utilizador.getNomeCompleto();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public String getPassword() {
        return utilizador.getPalavraPasse();
    }

    @Override
    public String getUsername() {
        return utilizador.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return utilizador.isActivo();
    }
}
