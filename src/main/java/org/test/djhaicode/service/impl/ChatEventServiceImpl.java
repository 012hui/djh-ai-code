package org.test.djhaicode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.test.djhaicode.exception.ErrorCode;
import org.test.djhaicode.exception.ThrowUtils;
import org.test.djhaicode.mapper.ChatEventMapper;
import org.test.djhaicode.model.entity.ChatEvent;
import org.test.djhaicode.service.ChatEventService;

import java.util.List;

/**
 * 工具调用事件 服务层实现。
 * 一行对应一次已执行完成的工具调用；summary 限长，避免把完整结果又塞回事件表。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Service
public class ChatEventServiceImpl extends ServiceImpl<ChatEventMapper, ChatEvent>
        implements ChatEventService {

    @Override
    public boolean appendEvent(ChatEvent event) {
        // 基础校验：事件必须能关联到应用和轮次，summary 不能为空且不能超过列长度
        ThrowUtils.throwIf(event == null, ErrorCode.PARAMS_ERROR, "事件不能为空");
        ThrowUtils.throwIf(event.getAppId() == null || event.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(event.getTurnId() == null || event.getTurnId() <= 0,
                ErrorCode.PARAMS_ERROR, "轮次ID不能为空");
        ThrowUtils.throwIf(event.getSeq() == null || event.getSeq() <= 0,
                ErrorCode.PARAMS_ERROR, "事件序号必须大于0");
        ThrowUtils.throwIf(StrUtil.isBlank(event.getToolName()),
                ErrorCode.PARAMS_ERROR, "工具名不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(event.getSummary()),
                ErrorCode.PARAMS_ERROR, "事件摘要不能为空");
        ThrowUtils.throwIf(event.getSummary().length() > 1024,
                ErrorCode.PARAMS_ERROR, "事件摘要不能超过1024字符");
        ThrowUtils.throwIf(event.getSuccess() == null
                        || (event.getSuccess() != 0 && event.getSuccess() != 1),
                ErrorCode.PARAMS_ERROR, "成功标记只能是0或1");
        return this.save(event);
    }

    @Override
    public List<ChatEvent> listByTurnId(Long turnId) {
        ThrowUtils.throwIf(turnId == null || turnId <= 0,
                ErrorCode.PARAMS_ERROR, "轮次ID不能为空");
        // seq 是轮内唯一排序依据；不要用 createTime（DATETIME 秒级，同秒多条无法区分）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatEvent::getTurnId, turnId)
                .orderBy(ChatEvent::getSeq, true);
        return this.list(queryWrapper);
    }
}
