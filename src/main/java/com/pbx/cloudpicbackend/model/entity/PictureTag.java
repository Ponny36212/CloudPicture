package com.pbx.cloudpicbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;


@TableName(value ="picture_tag")
@Data
public class PictureTag {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 标签id
     */
    private Long tagid;

    /**
     * 创建时间
     */
    private Date createTime;

}