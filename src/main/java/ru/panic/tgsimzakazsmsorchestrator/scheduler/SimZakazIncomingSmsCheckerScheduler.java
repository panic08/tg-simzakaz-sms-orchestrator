package ru.panic.tgsimzakazsmsorchestrator.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.panic.tgsimzakazsmsorchestrator.api.SimZakazApi;
import ru.panic.tgsimzakazsmsorchestrator.api.payload.SimZakazIncomingSms;
import ru.panic.tgsimzakazsmsorchestrator.bot.TelegramBot;
import ru.panic.tgsimzakazsmsorchestrator.model.CheckedSimZakazId;
import ru.panic.tgsimzakazsmsorchestrator.model.SavedTelegramChatId;
import ru.panic.tgsimzakazsmsorchestrator.repository.CheckedSimZakazIdRepository;
import ru.panic.tgsimzakazsmsorchestrator.repository.SavedTelegramChatIdRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SimZakazIncomingSmsCheckerScheduler {
    private final TelegramBot telegramBot;
    private final SavedTelegramChatIdRepository savedTelegramChatIdRepository;
    private final CheckedSimZakazIdRepository checkedSimZakazIdRepository;
    private final SimZakazApi simZakazApi;


    @Scheduled(fixedDelay = 4000)
    public void incomingSmsCheck() {
        List<CheckedSimZakazId> newCheckedSimZakazIdList = new ArrayList<>();

        List<SimZakazIncomingSms> simZakazIncomingSmsList =
                simZakazApi.getAllSms(20);

        for (SimZakazIncomingSms simZakazIncomingSms : simZakazIncomingSmsList) {
            if (checkedSimZakazIdRepository.countByCheckedId(simZakazIncomingSms.getId()) == 0) {
                newCheckedSimZakazIdList.add(CheckedSimZakazId.builder().checkedId(simZakazIncomingSms.getId()).build());

                Iterable<SavedTelegramChatId> savedTelegramChatIdList =
                        savedTelegramChatIdRepository.findAll();

                for (SavedTelegramChatId savedTelegramChatId : savedTelegramChatIdList) {
                    String sendMessageText = simZakazIncomingSms.getText();

                    sendMessageText = sendMessageText.replace("<", "");
                    sendMessageText = sendMessageText.replace(">", "");

                    telegramBot.handleSendMessage(SendMessage.builder()
                            .chatId(savedTelegramChatId.getTelegramChatId())
                            .text("❗\uFE0F <b>Пришло новое смс</b>\n\n"
                            + "\uD83D\uDC64 <b>Отправитель:</b> <code>" + simZakazIncomingSms.getPhoneFrom() + "</code>\n\n"
                            + "\uD83D\uDCF2 <b>На номер:</b> <code>" + simZakazIncomingSms.getPhoneTo() + "</code>\n\n"
                            + "\uD83D\uDCAC <b>Сообщение:</b> <code>" + sendMessageText + "</code>")
                            .parseMode("html")
                            .build());
                }
            }
        }

        if (!newCheckedSimZakazIdList.isEmpty()) {
            checkedSimZakazIdRepository.saveAll(newCheckedSimZakazIdList);
        }
    }
}
