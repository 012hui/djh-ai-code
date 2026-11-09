package org.test.djhaicode.core.handler;

import org.test.djhaicode.model.entity.User;
import org.test.djhaicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应。
 * 只负责累积文本，流结束后的历史落库交给 StreamHistoryRecorder 统一处理。
 */
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param turnId             本轮轮次ID
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser, long turnId) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        // doFinally 在完成/异常/取消时都会触发，用 AtomicBoolean 保证本轮 AI 行只落库一次
        AtomicBoolean flushed = new AtomicBoolean(false);
        // doFinally 拿不到异常对象，先用 doOnError 暂存，供 ON_ERROR 分支写失败原因
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return originFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnError(errorRef::set)
                .doFinally(signalType -> {
                    // 完成/异常/取消都只落一次库；具体 success 和失败原因由 StreamHistoryRecorder 统一处理
                    if (!flushed.compareAndSet(false, true)) {
                        return;
                    }
                    StreamHistoryRecorder.recordTerminal(signalType, errorRef.get(),
                            aiResponseBuilder.toString(),
                            chatHistoryService, appId, turnId, loginUser.getId());
                });
    }
}
