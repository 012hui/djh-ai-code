package org.test.djhaicode.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.test.djhaicode.constant.AppConstant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件读取工具
 * 支持 AI 通过工具调用的方式读取文件内容
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool{

    /**
     * 单次最多返回 2000 行
     */
    private static final int MAX_LINES_PER_READ = 2000;

    /**
     * 单次最多返回 32KB
     */
    private static final int MAX_BYTES_PER_READ = 32 * 1024;

    /**
     * 读取文件内容，支持按行范围分段。
     * 默认从第 1 行开始，单次最多 2000 行或 32KB；超出时返回续读提示。
     *
     * @param relativeFilePath 文件相对路径
     * @param startLine        起始行号，从 1 开始，可为空
     * @param endLine          结束行号，可为空
     * @param appId            应用ID
     * @return 文件内容或错误/续读提示
     */
    @Tool("读取指定路径的文件内容，支持按行范围分段读取")
    public String readFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("起始行号，从1开始，可为空")
            Integer startLine,
            @P("结束行号，可为空")
            Integer endLine,
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
                String error = "错误：文件不存在或不是文件 - " + relativeFilePath;
                String summary = String.format("readFile %s 失败：%s",
                        relativeFilePath, abbreviate(error, 200));
                return ToolResult.failure(summary, error).toEnvelopeText();
            }

            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int totalLines = allLines.size();
            if (totalLines == 0) {
                String summary = String.format("readFile %s（空文件）成功", relativeFilePath);
                return ToolResult.success(summary, "").toEnvelopeText();
            }

            // 默认从第 1 行开始，单次最多 2000 行
            int fromLine = startLine == null || startLine < 1 ? 1 : startLine;
            if (fromLine > totalLines) {
                String error = "错误：起始行超出文件范围 - " + relativeFilePath
                        + "（共 " + totalLines + " 行）";
                String summary = String.format("readFile %s 失败：起始行超出范围", relativeFilePath);
                return ToolResult.failure(summary, error).toEnvelopeText();
            }
            int requestedEnd = endLine == null
                    ? fromLine + MAX_LINES_PER_READ - 1
                    : Math.min(endLine, fromLine + MAX_LINES_PER_READ - 1);
            if (requestedEnd < fromLine) {
                String error = "错误：结束行不能小于起始行 - " + relativeFilePath;
                String summary = String.format("readFile %s 失败：结束行小于起始行", relativeFilePath);
                return ToolResult.failure(summary, error).toEnvelopeText();
            }
            int toLine = Math.min(requestedEnd, totalLines);

            StringBuilder content = new StringBuilder();
            int actualEnd = fromLine - 1;
            int usedBytes = 0;
            for (int i = fromLine - 1; i < toLine; i++) {
                String line = allLines.get(i);
                int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + 1;
                // 至少返回一行；之后超过 32KB 就停止
                if (usedBytes + lineBytes > MAX_BYTES_PER_READ && actualEnd >= fromLine) {
                    break;
                }
                content.append(line);
                if (i < toLine - 1) {
                    content.append("\n");
                }
                usedBytes += lineBytes;
                actualEnd = i + 1;
            }

            // 还有后续内容时给出明确的续读提示，引导模型继续分段读取
            if (actualEnd < totalLines) {
                content.append("\n\n[文件共 ").append(totalLines)
                        .append(" 行，已返回第 ").append(fromLine).append("-").append(actualEnd)
                        .append(" 行。如需继续读取，请调用 readFile(relativeFilePath, ")
                        .append(actualEnd + 1).append(", ")
                        .append(Math.min(actualEnd + MAX_LINES_PER_READ, totalLines))
                        .append(")]");
            }
            // 摘要记录实际返回的行范围，正文保持原始内容
            String summary = String.format("readFile %s（第 %d-%d 行）成功",
                    relativeFilePath, fromLine, actualEnd);
            return ToolResult.success(summary, content.toString()).toEnvelopeText();
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            String summary = String.format("readFile %s 失败：%s",
                    relativeFilePath, abbreviate(errorMessage, 200));
            return ToolResult.failure(summary, errorMessage).toEnvelopeText();
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateDisplayText(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s %s", getDisplayName(), relativeFilePath);
    }

}
