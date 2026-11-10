package org.test.djhaicode.ai.tools;

import cn.hutool.json.JSONObject;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成“工具请求”的展示文本，用于前端显示。
     * 这是给人看的提示，不是工具真实返回值。
     *
     * @return 工具请求展示文本
     */
    public String generateRequestDisplayText() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成“工具执行完成后”的展示文本，用于前端和 chat_history。
     * 注意：这不是工具真实返回值；模型看到的是 ToolResult.modelContent。
     * 例如：writeFile 返回完整代码块，modifyFile 返回 old/new，readFile 只返回路径。
     *
     * @param arguments 工具执行参数
     * @return 前端/历史展示文本
     */
    public abstract String generateDisplayText(JSONObject arguments);

    /**
     * 截断过长文本，避免事件摘要反向膨胀。
     *
     * @param text      原始文本
     * @param maxLength 最大保留字符数
     * @return 截断后的文本
     */
    protected String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
