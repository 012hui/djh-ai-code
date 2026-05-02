package org.test.djhaicode.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.test.djhaicode.model.entity.App;

/**
 * 应用 映射层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Mapper
public interface AppMapper extends BaseMapper<App> {

}
