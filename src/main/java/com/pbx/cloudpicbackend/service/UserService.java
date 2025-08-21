package com.pbx.cloudpicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pbx.cloudpicbackend.model.dto.user.UserQueryRequest;
import com.pbx.cloudpicbackend.model.dto.user.UserUpdateRequest;
import com.pbx.cloudpicbackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pbx.cloudpicbackend.model.vo.LoginUserVO;
import com.pbx.cloudpicbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 从数据库种查找是否为管理员
     * @param id
     * @return
     */
    boolean isAdmin(Long id);

    /**
     * 是否为超级管理 不可删除、修改权限
     * @param id
     * @return
     */
    boolean isSuperAdmin(Long id);

    /**
     * 用户兑换会员（会员码兑换）
     */
    boolean exchangeVip(User user, String vipCode);

    /**
     * 更新用户信息
     * @param userUpdateRequest
     * @param request
     * @return
     */
    boolean updateCommonUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request);

    /**
     * 验证用户名是否合法
     * @param userAccount 用户名
     * @return 合法True 不合法False
     */
    boolean validUserAccount(String userAccount);

    /**
     * 为VO设置用户头像
     * @param user
     * @param target
     */
    void setAvatar2VO(User user, LoginUserVO target);

    /**
     * 获取User信息 - 联查
     * @param id 用户id
     * @return User
     */
    User getFullById(Long id);


    /**
     * 获取完整的user信息 - 联查
     * @param page
     * @param queryWrapper
     * @return
     */
    Page<User> selectFullPage(Page<User> page, QueryWrapper<User> queryWrapper);

    /**
     * 更新用户 - 管理员
     * @param userUpdateRequest
     * @param loginUser
     * @return
     */
    boolean userUpdate(UserUpdateRequest userUpdateRequest, User loginUser);

    /**
     * 删除用户 - 管理员
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteUser(Long id, User loginUser);
}
