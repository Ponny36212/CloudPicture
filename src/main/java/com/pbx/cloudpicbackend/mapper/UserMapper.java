package com.pbx.cloudpicbackend.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pbx.cloudpicbackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    User getFullById(Long userId);

    Page<User> selectFullPage(@Param("page") Page<User> page, @Param("ew") QueryWrapper<User> queryWrapper);
}




