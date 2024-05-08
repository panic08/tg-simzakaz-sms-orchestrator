package ru.panic.tgsimzakazsmsorchestrator.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.panic.tgsimzakazsmsorchestrator.api.SimZakazApi;
import ru.panic.tgsimzakazsmsorchestrator.api.payload.SimZakazNumber;
import ru.panic.tgsimzakazsmsorchestrator.model.SavedTelegramChatId;
import ru.panic.tgsimzakazsmsorchestrator.repository.SavedTelegramChatIdRepository;
import ru.panic.tgsimzakazsmsorchestrator.bot.callback.AdminCallback;

import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    public TelegramBot(SavedTelegramChatIdRepository savedTelegramChatIdRepository, SimZakazApi simZakazApi) {
        this.savedTelegramChatIdRepository = savedTelegramChatIdRepository;
        this.simZakazApi = simZakazApi;

        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/start", "\uD83D\uDD04 Перезапустить"));

        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    private final Map<Long, Integer> addTelegramChatIdSteps = new HashMap<>();
    private final Map<Long, Integer> removeTelegramChatIdSteps = new HashMap<>();

    private SavedTelegramChatIdRepository savedTelegramChatIdRepository;
    private final SimZakazApi simZakazApi;

    @Value("${admin.telegramUserId}")
    private Long adminUserId;

    @Value("${telegram.bots.bot-token}")
    private String botToken;

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return "some";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            int messageId = update.getMessage().getMessageId();
            String text = update.getMessage().getText();

            if (addTelegramChatIdSteps.get(userId) != null) {
                handleAddChatId(chatId, messageId, addTelegramChatIdSteps.get(userId), userId, text);
                return;
            }

            if (removeTelegramChatIdSteps.get(userId) != null) {
                handleRemoveChatId(chatId, messageId, removeTelegramChatIdSteps.get(userId), userId, text);
                return;
            }

            switch (text) {
                case "➕ Добавить CHAT-ID" -> {
                    sendAddChatIdMessage(chatId, messageId, userId);
                    return;
                }

                case  "➖ Удалить CHAT-ID" -> {
                    sendRemoveChatIdMessage(chatId, messageId, userId);
                    return;
                }

                case "/numbers", "/номера", "\uD83D\uDCF1 Получить список номеров" -> {
                    sendGetNumberListMessage(chatId, messageId);
                    return;
                }

                case "/start" -> {
                    sendDefaultMessage(chatId, userId);
                    return;
                }
            }
        } else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            long userId = update.getCallbackQuery().getFrom().getId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackData = update.getCallbackQuery().getData();

            switch (callbackData) {
                case AdminCallback.BACK_FROM_ADD_CHAT_ID_CALLBACK -> {
                    addTelegramChatIdSteps.remove(userId);

                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                }

                case AdminCallback.BACK_FROM_REMOVE_CHAT_ID_CALLBACK -> {
                    removeTelegramChatIdSteps.remove(userId);

                    handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                }
            }
        }
    }

    private void sendGetNumberListMessage(long chatId, int messageId) {
        handleDeleteMessage(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());

        List<SimZakazNumber> simZakazNumbers = simZakazApi.getAllNumbers();

        StringBuilder simZakazNumbersSendMessageStringBuilder = new StringBuilder();

        for (SimZakazNumber simZakazNumber : simZakazNumbers) {
            simZakazNumber.setNumber(simZakazNumber.getNumber().substring(1));

            simZakazNumbersSendMessageStringBuilder.append("\uD83D\uDCF2 <b>Номер телефона:</b> <code>").append(simZakazNumber.getNumber()).append("</code>\n")
                    .append("\uD83D\uDD0C <b>Порт:</b> <code>").append(simZakazNumber.getPort()).append("</code>\n\n");
        }

        handleSendMessage(SendMessage.builder()
                .chatId(chatId)
                .text(simZakazNumbersSendMessageStringBuilder.toString())
                .parseMode("html")
                .build());
    }


    private void handleAddChatId(long chatId, int messageId, int oldMessageId, long userId, String text) {
        long newChatId = Long.parseLong(text);

        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

        addTelegramChatIdSteps.remove(userId);

        savedTelegramChatIdRepository.save(SavedTelegramChatId.builder()
                .telegramChatId(newChatId)
                .build());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton backFromAddChatIdButton = InlineKeyboardButton.builder()
                .callbackData(AdminCallback.BACK_FROM_ADD_CHAT_ID_CALLBACK)
                .text("↩\uFE0F Назад")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(backFromAddChatIdButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        handleEditMessageText(EditMessageText.builder()
                .chatId(chatId)
                .messageId(oldMessageId)
                .text("✅ <b>Новый CHAT-ID успешно добавлен</b>\n\n"
                + "Теперь входящие смс будут рассылаться на этот <i>CHAT-ID</i>")
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build());
    }

    private void handleRemoveChatId(long chatId, int messageId, int oldMessageId, long userId, String text) {
        long oldChatId = Long.parseLong(text);

        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

        removeTelegramChatIdSteps.remove(userId);

        savedTelegramChatIdRepository.deleteByTelegramChatId(oldChatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton backFromRemoveChatIdButton = InlineKeyboardButton.builder()
                .callbackData(AdminCallback.BACK_FROM_REMOVE_CHAT_ID_CALLBACK)
                .text("↩\uFE0F Назад")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(backFromRemoveChatIdButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        handleEditMessageText(EditMessageText.builder()
                .chatId(chatId)
                .messageId(oldMessageId)
                .text("✅ <b>Старый CHAT-ID успешно удален</b>\n\n"
                        + "Теперь входящие смс не будут рассылаться на этот <i>CHAT-ID</i>")
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build());
    }

    private void sendAddChatIdMessage(long chatId, int messageId, long userId) {
        if (!adminUserId.equals(userId)) {
            return;
        }

        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton backFromAddChatIdButton = InlineKeyboardButton.builder()
                .callbackData(AdminCallback.BACK_FROM_ADD_CHAT_ID_CALLBACK)
                .text("↩\uFE0F Назад")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(backFromAddChatIdButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        int newAddChatIdMessageId = handleSendMessage(SendMessage.builder()
                .chatId(chatId)
                .text("➕ <b>Добавить CHAT-ID</b>\n\n"
                + "Введите <i>CHAT-ID</i>, на который будет ввестись рассылка входящих смс")
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build());

        addTelegramChatIdSteps.put(userId, newAddChatIdMessageId);
    }

    private void sendRemoveChatIdMessage(long chatId, int messageId, long userId) {
        if (!adminUserId.equals(userId)) {
            return;
        }

        handleDeleteMessage(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton backFromRemoveChatIdButton = InlineKeyboardButton.builder()
                .callbackData(AdminCallback.BACK_FROM_REMOVE_CHAT_ID_CALLBACK)
                .text("↩\uFE0F Назад")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(backFromRemoveChatIdButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        int newRemoveChatIdMessageId = handleSendMessage(SendMessage.builder()
                .chatId(chatId)
                .text("➕ <b>Удалить CHAT-ID</b>\n\n"
                        + "Введите <i>CHAT-ID</i>, на котором прекратится вестись рассылка входящих смс")
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build());

        removeTelegramChatIdSteps.put(userId, newRemoveChatIdMessageId);
    }

    private void sendDefaultMessage(long chatId, long userId) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow1 = new KeyboardRow();

        KeyboardButton getNumberList = new KeyboardButton("\uD83D\uDCF1 Получить список номеров");

        keyboardRow1.add(getNumberList);

        keyboardRows.add(keyboardRow1);

        if (adminUserId.equals(userId)) {
            KeyboardButton addChatIdButton = new KeyboardButton("➕ Добавить CHAT-ID");
            KeyboardButton removeChatIdButton = new KeyboardButton("➖ Удалить CHAT-ID");

            KeyboardRow keyboardRow2 = new KeyboardRow();

            keyboardRow2.add(addChatIdButton);
            keyboardRow2.add(removeChatIdButton);

            keyboardRows.add(keyboardRow2);
        }

        replyKeyboardMarkup.setKeyboard(keyboardRows);


        handleSendMessage(SendMessage.builder()
                .chatId(chatId)
                .text("⚠\uFE0F <b>Для регулярного получения смс в боте или чате, нужно, чтобы администратор добавил ваш CHAT-ID или вашего чата</b>\n\n"
                        + "Для получения <i>CHAT-ID</i> вашего чата, вам следует воспользоваться ботом @GetMyChatID_Bot (добавить его в ваш чат и воспользоваться специальной командой, для получения <i>CHAT-ID</i> вашего чата и последующей отправки его администратоу)\n\n"
                        + "Для получения вашего <i>CHAT-ID</i>, вам нужно скопировать значение снизу и соответственно, отправить его администратору\n\n"
                        + "\uD83E\uDEAA <b>Ваш собственный CHAT-ID:</b> <code>" + userId + "</code>")
                .replyMarkup(replyKeyboardMarkup)
                .parseMode("html")
                .build());
    }

    //Sys

    public Integer handleSendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            log.error(e.getMessage());

            throw new RuntimeException(e.getMessage());
        }
    }

    public void handleEditMessageText(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void handleDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}

