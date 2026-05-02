package org.test.djhaicode.service.impl;

import djhaicode.innerservice.InnerScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.test.djhaicode.service.ScreenshotService;

@DubboService
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}
