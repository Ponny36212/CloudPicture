package com.pbx.cloudpicbackend.mapper;

import com.pbx.cloudpicbackend.model.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;


public interface TagMapper extends BaseMapper<Tag> {

    List<Tag> selectTagsByPictureId(Long pictureId);
}




