package ru.panic.tgsimzakazsmsorchestrator.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimZakazIncomingSms {
    private String id;
    private String modem;

    @JsonProperty("phone_from")
    private String phoneFrom;

    @JsonProperty("phone_to")
    private String phoneTo;

    private String text;

    private String date;
}
