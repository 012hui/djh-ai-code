package org.test.djhaicode.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具执行完成消息（SSE 流中的 TOOL_EXECUTED 事件）。
 *
 * <p>由 AiCodeGeneratorFacade 在工具执行完成后发出，字段来自 ToolExecution：
 * id=toolCallId，name=toolName，arguments=模型传入参数，result=工具真实返回文本。</p>
 *
 * <p>result 当前是 ToolResult envelope；它不是给前端展示的文本，
 * 前端/历史展示由 BaseTool.generateDisplayText 生成。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {

    /**
     * 工具调用 ID，用于关联工具请求和结果
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

    /**
     * 工具真实返回文本（当前是 ToolResult envelope）
     */
    private String result;

    public ToolExecutedMessage(ToolExecution toolExecution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        // ToolExecution 是 LangChain4j 的工具执行对象：
        // request 是工具调用请求，result 是工具方法的真实返回文本
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}
