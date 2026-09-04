package mz.unisced.sge.repository;

import java.util.List;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.model.NivelConfidencialidade;
import mz.unisced.sge.model.Prioridade;
import mz.unisced.sge.model.TipoExpediente;
import mz.unisced.sge.model.Utilizador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    boolean existsByNumeroRegisto(String numeroRegisto);

    long countByEstado(EstadoExpediente estado);

    long countByPrioridade(Prioridade prioridade);

    List<Expediente> findByResponsavelActualAndEstadoNotInOrderByDataRegistoDesc(
            Utilizador responsavel, List<EstadoExpediente> estados);

    @Query("select max(e.numeroRegisto) from Expediente e where e.numeroRegisto like concat(:prefixo, '%')")
    String maiorNumeroRegistoComPrefixo(@Param("prefixo") String prefixo);

    /**
     * Pesquisa com filtros opcionais: quando um parametro vem a nulo o respectivo
     * criterio e ignorado. A ultima condicao implementa a regra de
     * confidencialidade - quem nao tem privilegio de ver expedientes
     * confidenciais so ve os que criou ou dos quais e responsavel.
     */
    @Query("""
            select e from Expediente e
            left join e.responsavelActual r
            where (:texto is null
                   or lower(e.assunto) like lower(concat('%', :texto, '%'))
                   or lower(e.numeroRegisto) like lower(concat('%', :texto, '%'))
                   or lower(e.remetente) like lower(concat('%', :texto, '%')))
              and (:estado is null or e.estado = :estado)
              and (:tipo is null or e.tipo = :tipo)
              and (:verConfidenciais = true
                   or e.confidencialidade <> :nivelConfidencial
                   or e.criadoPor.id = :utilizadorId
                   or r.id = :utilizadorId)
            order by e.dataRegisto desc
            """)
    Page<Expediente> pesquisar(@Param("texto") String texto,
                               @Param("estado") EstadoExpediente estado,
                               @Param("tipo") TipoExpediente tipo,
                               @Param("verConfidenciais") boolean verConfidenciais,
                               @Param("nivelConfidencial") NivelConfidencialidade nivelConfidencial,
                               @Param("utilizadorId") Long utilizadorId,
                               Pageable pageable);

    @Query("select e.estado, count(e) from Expediente e group by e.estado order by e.estado")
    List<Object[]> contarPorEstado();

    @Query("select e.tipo, count(e) from Expediente e group by e.tipo order by e.tipo")
    List<Object[]> contarPorTipo();

    @Query("select e.prioridade, count(e) from Expediente e group by e.prioridade order by e.prioridade")
    List<Object[]> contarPorPrioridade();

    @Query("""
            select r.nomeCompleto, count(e) from Expediente e
            join e.responsavelActual r
            group by r.nomeCompleto
            order by count(e) desc
            """)
    List<Object[]> contarPorResponsavel();
}
