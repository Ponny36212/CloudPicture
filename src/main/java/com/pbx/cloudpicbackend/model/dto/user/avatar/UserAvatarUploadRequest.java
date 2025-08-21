package com.pbx.cloudpicbackend.model.dto.user.avatar;


import lombok.Data;

import java.io.Serializable;

@Data
public class UserAvatarUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;


}
