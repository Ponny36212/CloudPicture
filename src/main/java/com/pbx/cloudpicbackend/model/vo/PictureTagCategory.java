package com.pbx.cloudpicbackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签分类列表视图
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<TagVO> tagList;

    /**
     * 分类列表
     */
    private List<CategoryVO> categoryList;
}
