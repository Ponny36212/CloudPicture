package com.pbx.cloudpicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.constant.UserConstant;
import com.pbx.cloudpicbackend.exception.BusinessException;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.manager.auth.StpKit;
import com.pbx.cloudpicbackend.model.dto.user.UserQueryRequest;
import com.pbx.cloudpicbackend.model.dto.user.UserUpdateRequest;
import com.pbx.cloudpicbackend.model.dto.user.VipCode;
import com.pbx.cloudpicbackend.model.entity.User;
import com.pbx.cloudpicbackend.model.entity.UserAvatar;
import com.pbx.cloudpicbackend.model.enums.UserRoleEnum;
import com.pbx.cloudpicbackend.model.vo.LoginUserVO;
import com.pbx.cloudpicbackend.model.vo.UserVO;
import com.pbx.cloudpicbackend.service.UserService;
import com.pbx.cloudpicbackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户注册id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword),ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        ThrowUtils.throwIf(!this.validUserAccount(userAccount), ErrorCode.PARAMS_ERROR, "用户名不合法");
        // 2. 检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("未命名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }
        // 2. 对用户传递的密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 不存在，抛异常
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        }
        // 4. 保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        final String SALT = "lin";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中查询
        Long userId = currentUser.getId();
        // currentUser = this.getById(userId);
        currentUser = userMapper.getFullById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);

        try {
            this.setAvatar2VO(user, loginUserVO);
        } catch (Exception e) {
            log.error("获取用户头像失败: userId={}", user.getId(), e);
        }

        return loginUserVO;
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);

        try {
            this.setAvatar2VO(user, userVO);
        } catch (Exception e) {
            log.error("获取用户头像失败: userId={}", user.getId(), e);
        }

        return userVO;
    }


    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.eq("user.isDelete", 0);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public boolean isAdmin(Long id) {
        ThrowUtils.throwIf(id == null || id < 0, ErrorCode.PARAMS_ERROR);
        User user = this.getById(id);
        return this.isAdmin(user);
    }

    @Override
    public boolean isSuperAdmin(Long id) {
        ThrowUtils.throwIf(id == null || id < 0, ErrorCode.PARAMS_ERROR);
        User earliestUser = this.getOne(new QueryWrapper<User>().orderByAsc("createTime").last("LIMIT 1"));
        return earliestUser.getId().equals(id);
    }

    // region ------- 以下代码为用户兑换会员功能 --------

    // 新增依赖注入
    @Autowired
    private ResourceLoader resourceLoader;

    // 文件读写锁（确保并发安全）
    private final ReentrantLock fileLock = new ReentrantLock();

    // VIP 角色常量（根据你的需求自定义）
    private static final String VIP_ROLE = "vip";

    /**
     * 兑换会员
     *
     * @param user
     * @param vipCode
     * @return
     */
    @Override
    public boolean exchangeVip(User user, String vipCode) {
        // 1. 参数校验
        if (user == null || StrUtil.isBlank(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 读取并校验兑换码
        VipCode targetCode = validateAndMarkVipCode(vipCode);
        // 3. 更新用户信息
        updateUserVipInfo(user, targetCode.getCode());
        return true;
    }

    @Override
    public boolean updateCommonUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        // 校验参数
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User loginUser = this.getLoginUser(request);
        User user = new User();
        user.setId(userUpdateRequest.getId())
                .setUserProfile(loginUser.getUserProfile())
                .setUserName(loginUser.getUserName());
        // 此处为更新密码 优先处理更新密码
        boolean updatePwd = false;
        if (userUpdateRequest.getOldPassword() != null && userUpdateRequest.getNewPassword() != null) {
            // 获取加密后的密码
            String encryptPassword = this.getEncryptPassword(userUpdateRequest.getOldPassword());
            // 密码错误
            ThrowUtils.throwIf(!loginUser.getUserPassword().equals(encryptPassword), ErrorCode.PARAMS_ERROR, "密码错误或新密码不匹配");
            // 密码正确，修改密码
            user.setUserPassword(this.getEncryptPassword(userUpdateRequest.getNewPassword()));
            updatePwd = true;
        }
        // 拿到更新条件 更新 username profile
        if (!updatePwd) {
            user.setUserName(userUpdateRequest.getUserName())
                    .setUserProfile(userUpdateRequest.getUserProfile());
        }
        boolean result = this.updateById(user);
        if (updatePwd) this.userLogout(request);
        return result;
    }

    /**
     * 验证用户名是否合法
     * @param userAccount 用户名
     * @return 是否合法
     */
    @Override
    public boolean validUserAccount(String userAccount) {
        String reg = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$";
        return ReUtil.isMatch(reg, userAccount);
    }

    /**
     * 校验兑换码并标记为已使用
     */
    private VipCode validateAndMarkVipCode(String vipCode) {
        fileLock.lock(); // 加锁保证文件操作原子性
        try {
            // 读取 JSON 文件
            JSONArray jsonArray = readVipCodeFile();

            // 查找匹配的未使用兑换码
            List<VipCode> codes = JSONUtil.toList(jsonArray, VipCode.class);
            VipCode target = codes.stream()
                    .filter(code -> code.getCode().equals(vipCode) && !code.isHasUsed())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "无效的兑换码"));

            // 标记为已使用
            target.setHasUsed(true);

            // 写回文件
            writeVipCodeFile(JSONUtil.parseArray(codes));
            return target;
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 读取兑换码文件
     */
    private JSONArray readVipCodeFile() {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            String content = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
            return JSONUtil.parseArray(content);
        } catch (IOException e) {
            log.error("读取兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 写入兑换码文件
     */
    private void writeVipCodeFile(JSONArray jsonArray) {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            FileUtil.writeString(jsonArray.toStringPretty(), resource.getFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("更新兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 更新用户会员信息
     */
    private void updateUserVipInfo(User user, String usedVipCode) {
        // 计算过期时间（当前时间 + 1 年）
        Date expireTime = DateUtil.offsetMonth(new Date(), 12); // 计算当前时间加 1 年后的时间

        // 构建更新对象
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setVipExpireTime(expireTime); // 设置过期时间
        updateUser.setVipCode(usedVipCode);     // 记录使用的兑换码
        updateUser.setUserRole(VIP_ROLE);       // 修改用户角色

        // 执行更新
        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "开通会员失败，操作数据库失败");
        }
    }

    // endregion

    @Override
    public void setAvatar2VO(User user, LoginUserVO target) {
            List<UserAvatar> avatars = user.getAvatar();
            if (avatars != null && !avatars.isEmpty()) {
                Optional<UserAvatar> activeAvatar = avatars.stream()
                        .filter(a -> a.getIsActive() > 0)
                        .findFirst();

                if (activeAvatar.isPresent()) {
                    target.setUserAvatar(activeAvatar.get().getAvatarUrl());
                } else {
                    // 没有活跃头像时使用默认值
                    target.setUserAvatar(null);
                }
            }
    }

    @Override
    public User getFullById(Long id) {
        ThrowUtils.throwIf(id == null || id < 0, ErrorCode.PARAMS_ERROR);
        return userMapper.getFullById(id);
    }

    @Override
    public Page<User> selectFullPage(Page<User> page, QueryWrapper<User> queryWrapper) {
        return userMapper.selectFullPage(page, queryWrapper);
    }

    @Override
    public boolean userUpdate(UserUpdateRequest userUpdateRequest, User loginUser) {
        Long userId = userUpdateRequest.getId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 判断是否为超级管理员
        boolean isSuperAdmin = this.isSuperAdmin(userId);
        boolean isCurrentUserSuperAdmin = this.isSuperAdmin(loginUser.getId());
        boolean isCurrentUserAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isModifyingRole = StrUtil.isNotBlank(userUpdateRequest.getUserRole());

        // 如果要修改的用户是超级管理员
        if (isSuperAdmin) {
            // 不允许降级超级管理员权限
            if (isModifyingRole && !UserConstant.ADMIN_ROLE.equals(userUpdateRequest.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能降级超级管理员权限");
            }

            // 只有自己可以修改自己的信息
            if (!Objects.equals(loginUser.getId(), userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您无权修改超级管理员信息");
            }

            // 超管不能修改自己的权限
            if (isModifyingRole && Objects.equals(loginUser.getId(), userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "超级管理员不能修改自己的权限");
            }
        }
        // 修改的用户是普通管理员，且当前用户不是超级管理员，且不是修改自己
        else if (this.isAdmin(userId) && !isCurrentUserSuperAdmin && !Objects.equals(loginUser.getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您无权修改其他管理员信息");
        }

        // 普通管理员不能修改他人权限
        if (isCurrentUserAdmin && !isCurrentUserSuperAdmin && !Objects.equals(loginUser.getId(), userId)
                && isModifyingRole) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "普通管理员不能修改他人权限");
        }

        // 普通管理员不能修改自己的权限
        if (isCurrentUserAdmin && !isCurrentUserSuperAdmin && Objects.equals(loginUser.getId(), userId)
                && isModifyingRole) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "普通管理员不能修改自己的权限");
        }

        // 执行更新
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);

        // 填入密码
        if (StrUtil.isNotBlank(userUpdateRequest.getNewPassword())) {
            user.setUserPassword(this.getEncryptPassword(userUpdateRequest.getNewPassword()));
        }

        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return result;
    }

    @Override
    public boolean deleteUser(Long userId, User loginUser) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 查询要删除的用户
        User userToDelete = getById(userId);
        if (userToDelete == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 判断权限
        boolean isUserAdmin = UserConstant.ADMIN_ROLE.equals(userToDelete.getUserRole());
        boolean isSuperAdmin = this.isSuperAdmin(userId);
        boolean isCurrentUserSuperAdmin = this.isSuperAdmin(loginUser.getId());
        boolean isSelfOperation = Objects.equals(loginUser.getId(), userId);

        // 不能删除超级管理员
        if (isSuperAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能删除超级管理员");
        }

        // 普通管理员不能删除自己
        if (isUserAdmin && isSelfOperation) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员不能删除自己的账号");
        }

        // 只有超级管理员可以删除普通管理员
        if (isUserAdmin && !isCurrentUserSuperAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您无权删除管理员账号");
        }

        // 执行删除
        boolean result = removeById(userId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return true;
    }



}