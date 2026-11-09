package org.test.djhaicode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.test.djhaicode.ai.model.message.*;
import org.test.djhaicode.ai.tools.BaseTool;
import org.test.djhaicode.ai.tools.ToolManager;
import org.test.djhaicode.model.entity.User;
import org.test.djhaicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息。
 * 只负责解析流，流结束后的历史落库交给 StreamHistoryRecorder 统一处理。
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Autowired
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
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
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        // doFinally 在完成/异常/取消时都会触发，用 AtomicBoolean 保证本轮 AI 行只落库一次
        AtomicBoolean flushed = new AtomicBoolean(false);
        // doFinally 拿不到异常对象，先用 doOnError 暂存，供 ON_ERROR 分支写失败原因
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnError(errorRef::set)
                .doFinally(signalType -> {
                    // 完成/异常/取消都只落一次库；具体 success 和失败原因由 StreamHistoryRecorder 统一处理
                    if (!flushed.compareAndSet(false, true)) {
                        return;
                    }
                    StreamHistoryRecorder.recordTerminal(signalType, errorRef.get(),
                            chatHistoryStringBuilder.toString(),
                            chatHistoryService, appId, turnId, loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    //根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    //返回格式化的工具调用信息
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                //根据工具名称获取工具实例并生成相应的结果格式
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
