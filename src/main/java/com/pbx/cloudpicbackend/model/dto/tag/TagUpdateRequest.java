package com.pbx.cloudpicbackend.model.dto.tag;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * tag更新请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TagUpdateRequest extends TagAddRequest{
    /**
     * tag Id
     */
    private Long id;

    /**
     * 是否删除
     */
    private boolean isDelete = false;
}
