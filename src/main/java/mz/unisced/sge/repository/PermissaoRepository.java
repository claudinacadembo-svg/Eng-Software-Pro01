package mz.unisced.sge.repository;

import java.util.List;
import java.util.Optional;
import mz.unisced.sge.model.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

    Optional<Permissao> findByCodigo(String codigo);

    List<Permissao> findAllByOrderByModuloAscCodigoAsc();
}
