package org.test.djhaicode.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.test.djhaicode.model.entity.ChatEvent;

/**
 * 工具调用事件 映射层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Mapper
public interface ChatEventMapper extends BaseMapper<ChatEvent> {

}
