package com.pbx.cloudpicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片编辑请求
 */
@Data
public class PictureEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类 id
     */
    private Long category;

    /**
     * 标签
     * 以便 上传/编辑 时新增 tag, 把tags的泛型设置为 String 以便接收各种类型
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}