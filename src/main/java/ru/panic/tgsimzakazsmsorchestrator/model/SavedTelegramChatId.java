package ru.panic.tgsimzakazsmsorchestrator.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "saved_telegram_chat_ids_table")
@Data
@Builder
public class SavedTelegramChatId {
    @Id
    private Long id;

    @Column("telegram_chat_id")
    private Long telegramChatId;
}
