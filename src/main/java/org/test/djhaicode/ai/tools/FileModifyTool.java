package org.test.djhaicode.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.test.djhaicode.constant.AppConstant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具
 * 支持 AI 通过工具调用的方式修改文件内容
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool{

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要替换的旧内容")
            String oldContent,
            @P("替换后的新内容")
            String newContent,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                String errorMessage = "错误：文件不存在或不是文件 - " + relativeFilePath;
                String summary = String.format("modifyFile %s 失败：%s",
                        relativeFilePath, abbreviate(errorMessage, 200));
                return ToolResult.failure(summary, errorMessage).toEnvelopeText();
            }
            String originalContent = Files.readString(path);
            if (!originalContent.contains(oldContent)) {
                String warning = "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
                String summary = String.format("modifyFile %s 失败：未找到待替换内容", relativeFilePath);
                return ToolResult.failure(summary, warning).toEnvelopeText();
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                String info = "信息：替换后文件内容未发生变化 - " + relativeFilePath;
                String summary = String.format("modifyFile %s 失败：替换后内容未变化", relativeFilePath);
                return ToolResult.failure(summary, info).toEnvelopeText();
            }
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功修改文件: {}", path.toAbsolutePath());
            // 只保留替换前后短预览，完整内容不进入事件摘要
            String summary = String.format("modifyFile %s（%s → %s）成功",
                    relativeFilePath, abbreviate(oldContent, 50), abbreviate(newContent, 50));
            return ToolResult.success(summary, "文件修改成功: " + relativeFilePath).toEnvelopeText();
        } catch (IOException e) {
            String errorMessage = "修改文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            String summary = String.format("modifyFile %s 失败：%s",
                    relativeFilePath, abbreviate(errorMessage, 200));
            return ToolResult.failure(summary, errorMessage).toEnvelopeText();
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateDisplayText(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        // 修复原实现 old/new 都取路径的问题，展示真正的替换前后内容
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        //显示对比内容
        return String.format("""
                [工具调用] %s %s
                
                替换前：
                ```
                %s
                ```
                
                替换后：
                ```
                %s
                ```
                """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }

}
