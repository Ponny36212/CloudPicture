package com.pbx.cloudpicbackend.model.dto.tag;


import com.pbx.cloudpicbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TagQueryRequest extends PageRequest {
    /**
     * tag Id
     */
    private Long tagId;

    /**
     * tag 名称
     */
    private String name;

    /**
     * tag 描述
     */
    private String description;

    /**
     * 是否删除
     */
    private boolean isDelete = false;
}
