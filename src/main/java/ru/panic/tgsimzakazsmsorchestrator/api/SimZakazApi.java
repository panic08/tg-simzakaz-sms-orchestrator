package ru.panic.tgsimzakazsmsorchestrator.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.panic.tgsimzakazsmsorchestrator.api.payload.SimZakazIncomingSms;
import ru.panic.tgsimzakazsmsorchestrator.api.payload.SimZakazNumber;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimZakazApi {
    private final String SMS_ZAKAZ_API_URL = "https://api.simzakaz.ru/stubs/handler_api.php";
    @Value("${sms-zakaz.api-key}")
    private String apiKey;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<SimZakazIncomingSms> getAllSms(int limit) {
        ResponseEntity<String> allSmsZakazIncomingSmsString = restTemplate.getForEntity(SMS_ZAKAZ_API_URL + "?api_key=" + apiKey
                + "&action=getSMS&limit=" + limit, String.class);

        try {
            return objectMapper.readValue(allSmsZakazIncomingSmsString.getBody(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    public List<SimZakazNumber> getAllNumbers() {
        ResponseEntity<String> allSmsZakazIncomingSmsString = restTemplate.getForEntity(SMS_ZAKAZ_API_URL + "?api_key=" + apiKey
                + "&action=getNumbers", String.class);

        try {
            return objectMapper.readValue(allSmsZakazIncomingSmsString.getBody(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage());
            return null;
        }
    }
}
