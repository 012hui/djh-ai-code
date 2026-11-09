package org.test.djhaicode.service;

import com.mybatisflex.core.service.IService;
import org.test.djhaicode.model.entity.ChatEvent;

import java.util.List;

/**
 * 工具调用事件 服务层。
 * 只记录已执行完成的工具；请求了但没执行的不记录。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
public interface ChatEventService extends IService<ChatEvent> {

    /**
     * 追加一条工具执行事件（基础校验后插入）
     *
     * @param event 工具事件
     * @return 是否成功
     */
    boolean appendEvent(ChatEvent event);

    /**
     * 按轮次查询工具事件，按 seq 升序
     *
     * @param turnId 轮次ID
     * @return 工具事件列表
     */
    List<ChatEvent> listByTurnId(Long turnId);
}
