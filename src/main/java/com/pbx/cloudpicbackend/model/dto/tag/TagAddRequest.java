package com.pbx.cloudpicbackend.model.dto.tag;

import lombok.Data;

/**
 * tag新增请求
 */
@Data
public class TagAddRequest {

    /**
     * tag 名称
     */
    private String name;

    /**
     * tag 描述
     */
    private String description;

    /**
     * tag 类型
     */
    private String type;

}
