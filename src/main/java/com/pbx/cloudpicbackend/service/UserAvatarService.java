package com.pbx.cloudpicbackend.service;

import com.pbx.cloudpicbackend.model.dto.user.avatar.UserAvatarUploadRequest;
import com.pbx.cloudpicbackend.model.entity.User;
import com.pbx.cloudpicbackend.model.entity.UserAvatar;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pbx.cloudpicbackend.model.vo.UserAvatarVO;
import org.springframework.web.multipart.MultipartFile;


public interface UserAvatarService extends IService<UserAvatar> {
    /**
     * 用户头像上传
     */
    UserAvatarVO uploadAvatar(MultipartFile multipartFile, UserAvatarUploadRequest userAvatarUploadRequest, User loginUser);
}
