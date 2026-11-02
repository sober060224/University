package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    /**
     * 对话历史：取最近 200 条（按钮 newest-first 取，调用方自行反转成时间正序）。
     *
     * <p>shortcut: 这里必须是 Desc。写 Asc 时 Top200 先截取「最早」的 200 行，
     * 长期用户的第 201 次提问之后就永远看不到新消息了。
     */
    List<ChatMessage> findTop200ByUserIdOrderByIdDesc(Integer userId);

    /**
     * 批量删除（一条 SQL）。派生 deleteByUserId 是先 SELECT 全量再逐条 remove，
     * 上千行历史就是上千次往返。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ChatMessage m where m.userId = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);
}
