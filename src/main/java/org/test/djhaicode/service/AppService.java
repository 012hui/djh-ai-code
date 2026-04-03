package org.test.djhaicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.test.djhaicode.model.dto.app.AppQueryRequest;
import org.test.djhaicode.model.entity.App;
import org.test.djhaicode.model.entity.User;
import org.test.djhaicode.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
public interface AppService extends IService<App> {

    /**
     *部署应用
     * 1.参数校验：是否App存在，是否有权限部署该应用（仅本人可以）
     * 2.生成deployKey：有则不生成，使用Hutool随机生成6位大小写字母+数字，不能重复
     * 3.部署操作：将code_output下的生成文件，复制到code_deploy部署目录下，deployKey作为文件名
     *
     * @param appId 应用ID
     * @param loginUser 当前登录用户
     * @return 返回可以直接访问的URL：部署域名 / deployKey /
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 通过对话生成应用代码（SSE）
     * 调用generateAndSaveCodeStream生成代码，并且流式返回前端。
     * 前端不需要区分用户是否是第一次和该应用对话，始终调用这个接口即可，需要怎么做都交给后端来判断。
     * @param appId     应用ID
     * @param message   用户消息
     * @param loginUser 当前登录用户
     * @return   生成结果流
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     *获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造应用查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用封装类列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);
}
