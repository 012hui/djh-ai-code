package org.test.djhaicodeuser.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.test.djhaicode.model.entity.User;

/**
 * 用户 映射层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
