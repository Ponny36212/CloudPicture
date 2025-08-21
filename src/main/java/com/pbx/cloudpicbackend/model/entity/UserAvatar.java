package com.pbx.cloudpicbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;


@TableName(value ="user_avatar")
@Data
public class UserAvatar implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 用户id
     */
    private Long userId;

    /**
     * 是否为当前使用的头像：0-否，1-是
     */
    private Integer isActive;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;
}