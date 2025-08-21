package com.pbx.cloudpicbackend.model.dto.user;


import lombok.Data;
import java.io.Serializable;

/**
 * 用户修改密码请求
 */
@Data
public class UserPasswordUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;



}
