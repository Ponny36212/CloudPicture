package com.pbx.cloudpicbackend.model.vo;


import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.model.entity.UserAvatar;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@Data
public class UserAvatarVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片 id
     */
    private Long id;

    /**
     * 头像url
     */
    private String avatarUrl;

    /**
     * 头像缩略图
     */
    private String thumbnailUrl;


    /**
     * 将userAvatar转换VO
     * @param userAvatar
     * @return
     */
    public static UserAvatarVO objToVo(UserAvatar userAvatar) {
        ThrowUtils.throwIf(userAvatar == null, ErrorCode.PARAMS_ERROR);
        UserAvatarVO userAvatarVO = new UserAvatarVO();
        BeanUtils.copyProperties(userAvatar, userAvatarVO);
        return userAvatarVO;
    }
}
