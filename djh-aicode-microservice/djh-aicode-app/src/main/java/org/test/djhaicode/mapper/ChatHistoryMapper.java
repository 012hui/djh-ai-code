package org.test.djhaicode.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.test.djhaicode.model.entity.ChatHistory;

/**
 * 对话历史 映射层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

}
