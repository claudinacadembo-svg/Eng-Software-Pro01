package mz.unisced.sge.repository;

import java.util.List;
import java.util.Optional;
import mz.unisced.sge.model.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    Optional<Utilizador> findByUsername(String username);

    Optional<Utilizador> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Utilizador> findByActivoTrueOrderByNomeCompletoAsc();

    List<Utilizador> findAllByOrderByNomeCompletoAsc();

    long countByActivoTrue();
}
