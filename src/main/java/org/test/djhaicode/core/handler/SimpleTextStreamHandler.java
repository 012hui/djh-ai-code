package org.test.djhaicode.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.test.djhaicode.model.entity.User;
import org.test.djhaicode.model.enums.ChatHistoryMessageTypeEnum;
import org.test.djhaicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    saveAiMessageSafely(chatHistoryService, appId, aiResponse, loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    saveAiMessageSafely(chatHistoryService, appId, errorMessage, loginUser.getId());
                });
    }

    /**
     * 保存 AI 对话历史。历史落库失败只记录日志，不影响已经完成的生成结果。
     */
    private void saveAiMessageSafely(ChatHistoryService chatHistoryService, long appId,
                                     String message, Long userId) {
        try {
            chatHistoryService.addChatMessage(appId, message,
                    ChatHistoryMessageTypeEnum.AI.getValue(), userId);
        } catch (Exception e) {
            log.error("保存AI对话历史失败，appId={}, messageLength={}", appId,
                    message == null ? 0 : message.length(), e);
            saveFallbackAiMessage(chatHistoryService, appId, userId);
        }
    }

    /**
     * 完整 AI 消息落库失败时写入一条短兜底消息，避免下一轮只有 user 行、没有 AI 行的断档。
     */
    private void saveFallbackAiMessage(ChatHistoryService chatHistoryService, long appId, Long userId) {
        String fallbackMessage = "AI 回复过长，未能完整保存到对话历史。"
                + "本轮已执行的工具和生成文件仍保留在工作目录，请结合文件和对话继续。";
        try {
            chatHistoryService.addChatMessage(appId, fallbackMessage,
                    ChatHistoryMessageTypeEnum.AI.getValue(), userId);
        } catch (Exception e) {
            log.error("保存AI对话历史兜底消息也失败，appId={}", appId, e);
        }
    }
}
