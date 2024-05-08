package ru.panic.tgsimzakazsmsorchestrator.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.panic.tgsimzakazsmsorchestrator.model.CheckedSimZakazId;

@Repository
public interface CheckedSimZakazIdRepository extends CrudRepository<CheckedSimZakazId, Long> {

    @Query("SELECT COUNT(*) FROM checked_sim_zakaz_ids_table WHERE checked_id = :checkedId")
    long countByCheckedId(@Param("checkedId") String checkedId);
}
