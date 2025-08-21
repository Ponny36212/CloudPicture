package com.pbx.cloudpicbackend.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends LoginUserVO implements Serializable {

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private Long vipNumber;

    private static final long serialVersionUID = 1L;
}