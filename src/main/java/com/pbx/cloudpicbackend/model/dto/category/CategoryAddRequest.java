package com.pbx.cloudpicbackend.model.dto.category;


import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryAddRequest implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;
}
