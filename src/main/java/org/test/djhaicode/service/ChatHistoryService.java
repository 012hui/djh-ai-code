package org.test.djhaicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.test.djhaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import org.test.djhaicode.model.entity.ChatHistory;
import org.test.djhaicode.model.entity.User;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     *
     * @param appId         应用ID
     * @param message       对话消息
     * @param messageType   消息类型，用户还是AI回复
     * @param userId        用户ID
     * @return   消息是否添加到对话历史成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据 appID 删除对话历史
     *
     * @param appId     应用ID
     * @return  是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 加载对话历史到内存
     *
     * @param appId
     * @param chatMemory
     * @param maxCount  最多加载多少条
     * @return  加载成功的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 分页查询某APP对话记录
     *
     * @param appId            应用ID
     * @param pageSize         分页大小
     * @param lastCreateTime   最后一条记录创建时间，游标查询此前的记录
     * @param loginUser        当前用户
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
