package org.test.djhaicode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.test.djhaicode.ai.model.message.*;
import org.test.djhaicode.ai.tools.BaseTool;
import org.test.djhaicode.ai.tools.ToolResult;
import org.test.djhaicode.ai.tools.ToolManager;
import org.test.djhaicode.model.entity.ChatEvent;
import org.test.djhaicode.model.entity.User;
import org.test.djhaicode.service.ChatEventService;
import org.test.djhaicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    // 工具事件服务，TOOL_EXECUTED 时写入 chat_event
    @Autowired
    private ChatEventService chatEventService;

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
        // 每轮工具事件从 1 开始编号；seq 只在当前订阅内有效
        AtomicInteger seq = new AtomicInteger(0);
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds,
                            appId, turnId, seq);
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
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder,
                                          Set<String> seenToolIds, long appId, long turnId,
                                          AtomicInteger seq) {
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
                // TOOL_REQUEST 表示“模型准备调用工具”，还没有执行结果
                ToolRequestMessage toolRequestEvent = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolCallId = toolRequestEvent.getId();
                String toolName = toolRequestEvent.getName();
                // 检查是否是第一次看到这个工具调用 ID
                if (toolCallId != null && !seenToolIds.contains(toolCallId)) {
                    // 第一次调用这个工具，记录 ID 并返回请求展示文本
                    seenToolIds.add(toolCallId);
                    //根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 只给前端展示“选择了哪个工具”，不是工具真实返回值
                    return tool.generateRequestDisplayText();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // TOOL_EXECUTED 表示“工具已经执行完成”，result 是工具真实返回值
                ToolExecutedMessage executedEvent = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = executedEvent.getName();
                // 模型调用工具时传入的参数 JSON，用于生成前端/历史展示文本
                JSONObject toolArguments = JSONUtil.parseObj(executedEvent.getArguments());
                BaseTool tool = toolManager.getTool(toolName);
                // 工具真实返回值：ToolResult envelope，模型看到的就是它
                String toolResultEnvelope = executedEvent.getResult();
                // 从参数获取 展示文本：给前端和 chat_history 看，不是模型看到的内容
                String displayText = tool.generateDisplayText(toolArguments);
                // 记录工具事件：摘要只存路径/行数/成功标记，不存完整结果
                saveToolEventSafely(tool, toolResultEnvelope, appId, turnId, seq);
                // 输出前端和要持久化的内容
                String displayChunk = String.format("\n\n%s\n\n", displayText);
                chatHistoryStringBuilder.append(displayChunk);
                return displayChunk;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    /**
     * 保存工具执行事件。事件写入失败只记录日志，不影响已经进行的生成。
     */
    private void saveToolEventSafely(BaseTool tool, String toolResultEnvelope,
                                     long appId, long turnId, AtomicInteger seq) {
        try {
            // 工具返回“JSON 头 + 原始正文”；parseEnvelope 把文本解析回 ToolResult
            ToolResult toolResult = ToolResult.parseEnvelope(toolResultEnvelope);
            boolean success = toolResult != null && toolResult.success();
            // eventSummary 是工具执行时自己生成的，handler 只负责写进 chat_event
            // 摘要为空时给一个兜底文本，避免事件插入被参数校验拒绝
            String summary = toolResult == null
                    ? tool.getToolName() + " 结果格式不兼容"
                    : StrUtil.blankToDefault(toolResult.eventSummary(),
                    tool.getToolName() + " 执行完成");
            // 事件表 summary 列是 varchar(1024)，这里再做一次兜底截断
            if (summary.length() > 1024) {
                summary = summary.substring(0, 1024);
            }
            //保存工具执行事件
            ChatEvent event = ChatEvent.builder()
                    .appId(appId)
                    .turnId(turnId)
                    // seq 从 1 递增，保证同一轮事件顺序
                    .seq(seq.incrementAndGet())
                    .toolName(tool.getToolName())
                    .summary(summary)
                    .success(success ? 1 : 0)
                    .build();
            chatEventService.appendEvent(event);
        } catch (Exception e) {
            log.error("保存工具事件失败，appId={}, turnId={}, toolName={}",
                    appId, turnId, tool.getToolName(), e);
        }
    }
}
