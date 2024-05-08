package ru.panic.tgsimzakazsmsorchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "checked_sim_zakaz_ids_table")
@Data
@Builder
public class CheckedSimZakazId {
    @Id
    private Long id;

    @JsonProperty("checked_id")
    private String checkedId;
}
