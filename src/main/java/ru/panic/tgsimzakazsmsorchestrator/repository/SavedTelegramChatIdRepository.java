package ru.panic.tgsimzakazsmsorchestrator.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.panic.tgsimzakazsmsorchestrator.model.SavedTelegramChatId;

@Repository
public interface SavedTelegramChatIdRepository extends CrudRepository<SavedTelegramChatId, Long> {
    @Query("DELETE FROM saved_telegram_chat_ids_table WHERE telegram_chat_id = :telegramChatId")
    @Modifying
    void deleteByTelegramChatId(@Param("telegramChatId") long telegramChatId);
}
