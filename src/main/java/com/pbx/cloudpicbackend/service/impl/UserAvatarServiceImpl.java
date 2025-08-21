package com.pbx.cloudpicbackend.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.manager.upload.FilePictureUpload;
import com.pbx.cloudpicbackend.manager.upload.PictureUploadTemplate;
import com.pbx.cloudpicbackend.model.dto.file.UploadPictureResult;
import com.pbx.cloudpicbackend.model.dto.user.avatar.UserAvatarUploadRequest;
import com.pbx.cloudpicbackend.model.entity.User;
import com.pbx.cloudpicbackend.model.entity.UserAvatar;
import com.pbx.cloudpicbackend.model.vo.UserAvatarVO;
import com.pbx.cloudpicbackend.service.UserAvatarService;
import com.pbx.cloudpicbackend.service.UserService;
import com.pbx.cloudpicbackend.mapper.UserAvatarMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@Service
@Slf4j
public class UserAvatarServiceImpl extends ServiceImpl<UserAvatarMapper, UserAvatar>
    implements UserAvatarService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public UserAvatarVO uploadAvatar(MultipartFile multipartFile, UserAvatarUploadRequest userAvatarUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 上传图片，得到图片信息
        // 构造上传信息
        String uploadPathPrefix = String.format("public/avatar/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(multipartFile, uploadPathPrefix);

        // 构造要入库的图片信息
        // 0. 把userAvatar表种其它的active状态设置为0
        UpdateWrapper<UserAvatar> wrapper = new UpdateWrapper<>();
        wrapper
                .eq("userId", loginUser.getId())
                .set("isActive", false);
        // 1. 插入UserAvatar
        UserAvatar userAvatar = new UserAvatar();
        userAvatar.setUserId(loginUser.getId());
        userAvatar.setIsActive(1);
        userAvatar.setAvatarUrl(uploadPictureResult.getUrl());
        userAvatar.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 开启事务
        transactionTemplate.execute(status -> {
            // 更新旧头像
            if (this.count(new QueryWrapper<UserAvatar>().eq("userId", loginUser.getId())) > 0) {
                boolean update = this.update(wrapper);
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
            }
            // 插入新头像
            boolean save = this.save(userAvatar);
            ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
            // 2. 更新User
            User user = new User();
            user.setId(loginUser.getId());
            user.setUserAvatar(userAvatar.getUserId());
            boolean updUserAvatar = userService.updateById(user);
            ThrowUtils.throwIf(!updUserAvatar, ErrorCode.OPERATION_ERROR);
            return userAvatar;
        });

        return UserAvatarVO.objToVo(userAvatar);
    }
}




