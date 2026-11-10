package org.test.djhaicode.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.test.djhaicode.constant.AppConstant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件目录读取工具
 * 使用 Hutool 简化文件操作
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool{

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    /**
     * 读取目录结构，支持限制深度和条目数。
     * 超过上限时返回提示，引导模型指定子目录继续读取。
     *
     * @param relativeDirPath 目录相对路径，可为空
     * @param maxDepth        最大深度，可为空，默认 5
     * @param maxEntries      最大条目数，可为空，默认 200
     * @param appId           应用ID
     * @return 目录结构或错误/截断提示
     */
    @Tool("读取目录结构，可限制深度和条目数")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @P("最大深度，可为空，默认5")
            Integer maxDepth,
            @P("最大条目数，可为空，默认200")
            Integer maxEntries,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeDirPath == null ? "" : relativeDirPath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeDirPath == null ? "" : relativeDirPath);
            }
            File targetDir = path.toFile();
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                String error = "错误：目录不存在或不是目录 - " + relativeDirPath;
                String summary = String.format("readDir %s 失败：%s",
                        StrUtil.blankToDefault(relativeDirPath, "根目录"),
                        abbreviate(error, 200));
                return ToolResult.failure(summary, error).toEnvelopeText();
            }
            // 默认最多 5 层、200 个条目，避免一次返回整个大目录
            int depthLimit = maxDepth == null || maxDepth < 1 ? 5 : maxDepth;
            int entryLimit = maxEntries == null || maxEntries < 1 ? 200 : maxEntries;

            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            // 使用 Hutool 递归获取所有文件
            List<File> allFiles = FileUtil.loopFiles(targetDir, file -> !shouldIgnore(file.getName()));

            // 超过深度上限的条目不再展示，但保留提示，引导模型指定子目录
            boolean depthTruncated = allFiles.stream()
                    .anyMatch(file -> getRelativeDepth(targetDir, file) > depthLimit);

            // 按路径深度和名称排序，只取前 entryLimit + 1 条用于判断是否截断
            List<File> visibleFiles = allFiles.stream()
                    .filter(file -> getRelativeDepth(targetDir, file) <= depthLimit)
                    .sorted((f1, f2) -> {
                        int depth1 = getRelativeDepth(targetDir, f1);
                        int depth2 = getRelativeDepth(targetDir, f2);
                        if (depth1 != depth2) {
                            return Integer.compare(depth1, depth2);
                        }
                        return f1.getPath().compareTo(f2.getPath());
                    })
                    .limit((long) entryLimit + 1)
                    .collect(Collectors.toList());

            // 多取一条用于判断是否还有被截断的条目
            boolean entryTruncated = visibleFiles.size() > entryLimit;
            if (entryTruncated) {
                visibleFiles = visibleFiles.subList(0, entryLimit);
            }
            for (File file : visibleFiles) {
                int depth = getRelativeDepth(targetDir, file);
                // 每个条目单独一行，便于模型阅读，也便于统计条目数
                structure.append("  ".repeat(depth)).append(file.getName()).append("\n");
            }
            if (entryTruncated) {
                structure.append("[目录条目过多，已返回前 ").append(entryLimit)
                        .append(" 个，请指定子目录继续读取]\n");
            }
            if (depthTruncated) {
                structure.append("[已达到深度上限 ").append(depthLimit)
                        .append("，请指定子目录继续读取]\n");
            }
            // 摘要记录实际展示的条目数和是否截断，正文保持目录树原样
            int entryCount = visibleFiles.size();
            boolean truncated = entryTruncated || depthTruncated;
            String summary = String.format("readDir %s（%d 个条目%s）成功",
                    StrUtil.blankToDefault(relativeDirPath, "根目录"),
                    entryCount, truncated ? "，已截断" : "");
            return ToolResult.success(summary, structure.toString()).toEnvelopeText();

        } catch (Exception e) {
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            String summary = String.format("readDir %s 失败：%s",
                    StrUtil.blankToDefault(relativeDirPath, "根目录"),
                    abbreviate(errorMessage, 200));
            return ToolResult.failure(summary, errorMessage).toEnvelopeText();
        }
    }

    /**
     * 计算文件相对于根目录的深度
     */
    private int getRelativeDepth(File root, File file) {
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(String fileName) {
        // 检查是否在忽略名称列表中
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateDisplayText(JSONObject arguments) {
        // 工具入参名是 relativeDirPath，原实现取成了 relativeFilePath
        String relativeDirPath = arguments.getStr("relativeDirPath");
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }

}
