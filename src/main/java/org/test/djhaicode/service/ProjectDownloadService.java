package org.test.djhaicode.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 下载项目压缩包
     * @param projectPath
     * @param downloadFileName
     * @param response
     * @return
     */
    String downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
