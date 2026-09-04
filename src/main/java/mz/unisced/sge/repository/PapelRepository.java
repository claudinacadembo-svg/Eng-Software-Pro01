package mz.unisced.sge.repository;

import java.util.List;
import java.util.Optional;
import mz.unisced.sge.model.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepository extends JpaRepository<Papel, Long> {

    Optional<Papel> findByNome(String nome);

    List<Papel> findAllByOrderByNomeAsc();
}
