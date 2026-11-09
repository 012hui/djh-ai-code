package org.test.djhaicode.core.handler;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.test.djhaicode.model.enums.ChatHistoryMessageTypeEnum;
import org.test.djhaicode.service.ChatHistoryService;
import reactor.core.publisher.SignalType;

/**
 * 生成流终态落库工具。
 *
 * <p>VUE 和 HTML/MULTI_FILE 两个 handler 的流解析方式不同，
 * 但“完成 / 异常 / 取消后如何写入本轮 ai 行”的规则完全一致。
 * 统一放在这里，避免两边各写一套 success 和兜底逻辑。</p>
 */
@Slf4j
public final class StreamHistoryRecorder {

    /**
     * 本轮成功
     */
    private static final int SUCCESS = 1;

    /**
     * 本轮失败
     */
    private static final int FAILURE = 0;

    private StreamHistoryRecorder() {
        // 工具类不允许实例化
    }

    /**
     * 根据流的终止信号写入本轮 ai 行。
     *
     * @param signalType         终止信号：正常完成、异常、取消
     * @param error              异常对象；正常完成或取消时为 null
     * @param partial            已经生成的部分内容
     * @param chatHistoryService 对话历史服务
     * @param appId              应用ID
     * @param turnId             本轮轮次ID
     * @param userId             用户ID
     */
    public static void recordTerminal(SignalType signalType, Throwable error, String partial,
                                      ChatHistoryService chatHistoryService, long appId,
                                      long turnId, Long userId) {
        if (signalType == SignalType.ON_COMPLETE) {
            // 正常完成：本轮成功，保存完整输出
            saveAiMessageSafely(chatHistoryService, appId, turnId, SUCCESS, partial, userId);
        } else if (signalType == SignalType.ON_ERROR) {
            // 模型/网络异常：本轮失败，保留部分输出和失败原因
            String reason = error == null
                    ? "AI回复失败: 未知错误"
                    : "AI回复失败: " + error.getMessage();
            saveAiMessageSafely(chatHistoryService, appId, turnId, FAILURE,
                    buildFailureMessage(partial, reason), userId);
        } else if (signalType == SignalType.CANCEL) {
            // 用户关闭页面或取消：本轮失败，保留部分输出
            saveAiMessageSafely(chatHistoryService, appId, turnId, FAILURE,
                    buildFailureMessage(partial, "用户中断"), userId);
        }
    }

    /**
     * 失败时把已经生成的部分内容和失败原因拼在一起，避免历史里只剩一条错误信息。
     */
    private static String buildFailureMessage(String partial, String reason) {
        if (StrUtil.isBlank(partial)) {
            return reason;
        }
        return partial + "\n\n[生成中断] " + reason;
    }

    /**
     * 保存 AI 对话历史。落库失败只记录日志，不影响已经完成的生成结果；
     * 同时补一条短兜底消息，避免下一轮只有 user 行、没有 ai 行。
     */
    private static void saveAiMessageSafely(ChatHistoryService chatHistoryService, long appId,
                                            long turnId, Integer success, String message, Long userId) {
        try {
            chatHistoryService.addChatMessage(appId, message,
                    ChatHistoryMessageTypeEnum.AI.getValue(), userId, turnId, success);
        } catch (Exception e) {
            log.error("保存AI对话历史失败，appId={}, turnId={}, success={}, messageLength={}",
                    appId, turnId, success, message == null ? 0 : message.length(), e);
            saveFallbackAiMessage(chatHistoryService, appId, turnId, success, userId);
        }
    }

    /**
     * 完整 AI 消息落库失败时写入一条短兜底消息。
     * 兜底消息沿用同一个 turnId 和 success，保证这一轮的轮次关系不丢。
     */
    private static void saveFallbackAiMessage(ChatHistoryService chatHistoryService, long appId,
                                              long turnId, Integer success, Long userId) {
        String fallbackMessage = "AI 回复未能完整保存到对话历史。"
                + "本轮已执行的工具和生成文件仍保留在工作目录，请结合文件和对话继续。";
        try {
            chatHistoryService.addChatMessage(appId, fallbackMessage,
                    ChatHistoryMessageTypeEnum.AI.getValue(), userId, turnId, success);
        } catch (Exception e) {
            log.error("保存AI对话历史兜底消息也失败，appId={}, turnId={}", appId, turnId, e);
        }
    }
}
