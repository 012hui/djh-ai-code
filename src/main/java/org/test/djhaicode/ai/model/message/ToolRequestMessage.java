package org.test.djhaicode.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具请求消息（SSE 流中的 TOOL_REQUEST 事件）。
 * 表示模型准备调用哪个工具，还没有执行结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    /**
     * 工具调用 ID，用于关联后续的工具执行结果
     */
    private String id;

    /**
     * 工具名
     */
    private String name;

    /**
     * 模型调用工具时传入的参数 JSON
     */
    private String arguments;

    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        // 从 LangChain4j 的 ToolExecutionRequest 拷贝工具调用信息
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}
