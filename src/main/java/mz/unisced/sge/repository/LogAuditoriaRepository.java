package mz.unisced.sge.repository;

import java.time.LocalDateTime;
import java.util.List;
import mz.unisced.sge.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    @Query("""
            select l from LogAuditoria l
            where (:username is null or lower(l.username) like lower(concat('%', :username, '%')))
              and (:accao is null or l.accao = :accao)
              and (:inicio is null or l.dataHora >= :inicio)
              and (:fim is null or l.dataHora <= :fim)
            order by l.dataHora desc
            """)
    Page<LogAuditoria> pesquisar(@Param("username") String username,
                                 @Param("accao") String accao,
                                 @Param("inicio") LocalDateTime inicio,
                                 @Param("fim") LocalDateTime fim,
                                 Pageable pageable);

    @Query("select distinct l.accao from LogAuditoria l order by l.accao")
    List<String> listarAccoes();

    List<LogAuditoria> findTop10ByOrderByDataHoraDesc();

    long countBySucessoFalse();
}
