package org.test.djhaicode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 单文件代码结果
 * Description把描述也发给模型
 */
@Description("生成HTML代码文件结果")
@Data
public class HtmlCodeResult {

    /**
     * html代码
     */
    @Description("HTML代码")
    private String htmlCode;

    /**
     * 描述
     */
    @Description("生成代码的描述")
    private String description;
}
