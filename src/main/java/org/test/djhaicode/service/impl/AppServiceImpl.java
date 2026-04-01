package org.test.djhaicode.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.test.djhaicode.model.entity.App;
import org.test.djhaicode.mapper.AppMapper;
import org.test.djhaicode.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
