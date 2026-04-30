package org.test.djhaicodeuser.innerservice;

import jakarta.servlet.http.HttpServletRequest;
import org.test.djhaicodeuser.exception.BusinessException;
import org.test.djhaicodeuser.exception.ErrorCode;
import org.test.djhaicodeuser.model.entity.User;
import org.test.djhaicodeuser.model.vo.UserVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static org.test.djhaicodeuser.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 内部使用的用户服务
 */
public interface InnerUserService {

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);

    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}
