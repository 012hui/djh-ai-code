package org.test.djhaicode.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 工具执行结果。
 *
 * <p>模型看到的文本格式是：</p>
 * <pre>
 * {"success":true,"summary":"readFile src/App.vue（第 1-2000 行）成功"}
 * ---TOOL_CONTENT---
 * 原始正文
 * </pre>
 *
 * <p>头部 JSON 给后端解析 success/summary，分隔符后的正文保持原样，
 * 避免 readFile 的文件内容被 JSON 转义。</p>
 *
 * <p>注意：JSON 头里的 key 叫 summary，对应 Java 字段 eventSummary。</p>
 *
 * <p>eventSummary 和 modelContent 都由工具实现自己生成，不是模型或 handler 生成的。</p>
 *
 * @param success       工具是否达到预期结果
 * @param eventSummary  工具自己生成的短摘要，写入 chat_event，用于压缩和续写
 * @param modelContent  工具自己生成的模型正文，返回给模型
 */
public record ToolResult(boolean success, String eventSummary, String modelContent) {

    /**
     * envelope 头部和正文之间的分隔符
     */
    public static final String HEADER_BODY_SEPARATOR = "\n---TOOL_CONTENT---\n";


    /**
     * 工具执行成功时使用。
     *
     * <p>eventSummary 由工具自己生成，写入 chat_event，作为压缩/续写用的短摘要；
     * modelContent 也由工具自己生成，是返回给模型的正文
     * （readFile 是文件内容，writeFile/deleteFile 等是状态文本）。</p>
     *
     * @param eventSummary 工具生成的短摘要
     * @param modelContent 工具生成的模型正文
     * @return 成功结果
     */
    public static ToolResult success(String eventSummary, String modelContent) {
        return new ToolResult(true, eventSummary, modelContent);
    }

    /**
     * 工具执行失败时使用。
     *
     * <p>eventSummary 由工具自己生成，写入 chat_event；
     * modelContent 是返回给模型的错误文本，让模型知道失败原因。</p>
     *
     * @param eventSummary 工具生成的失败摘要
     * @param modelContent 工具生成的错误正文
     * @return 失败结果
     */
    public static ToolResult failure(String eventSummary, String modelContent) {
        return new ToolResult(false, eventSummary, modelContent);
    }

    /**
     * 对象 → 模型可见文本。
     * 输出格式：JSON 头 + 分隔符 + 原始正文。
     */
    public String toEnvelopeText() {
        String header = new JSONObject()
                .set("success", success)
                .set("summary", eventSummary == null ? "" : eventSummary)
                .toString();
        return header + HEADER_BODY_SEPARATOR + (modelContent == null ? "" : modelContent);
    }

    /**
     * 模型可见文本 → 对象。
     * 输入格式：JSON 头 + 分隔符 + 原始正文；不是 envelope 格式时返回 null。
     * 头部的 summary 就是工具生成时写入的 eventSummary。
     */
    public static ToolResult parseEnvelope(String envelopeText) {
        if (StrUtil.isBlank(envelopeText)) {
            return null;
        }
        int separatorIndex = envelopeText.indexOf(HEADER_BODY_SEPARATOR);
        if (separatorIndex <= 0) {
            return null;
        }
        try {
            JSONObject header = JSONUtil.parseObj(envelopeText.substring(0, separatorIndex));
            Boolean success = header.getBool("success");
            if (success == null) {
                return null;
            }
            String eventSummary = header.getStr("summary");
            String modelContent = envelopeText.substring(
                    separatorIndex + HEADER_BODY_SEPARATOR.length());
            return new ToolResult(success, eventSummary, modelContent);
        } catch (Exception e) {
            return null;
        }
    }
}
