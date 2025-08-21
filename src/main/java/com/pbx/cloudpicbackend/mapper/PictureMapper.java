package com.pbx.cloudpicbackend.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pbx.cloudpicbackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface PictureMapper extends BaseMapper<Picture> {

    Picture getFullById(long id);

    Page<Picture> selectFullPage(@Param("page") Page<Picture> page, @Param("ew") Wrapper<Picture> queryWrapper);
}




