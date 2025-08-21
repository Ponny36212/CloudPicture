package com.pbx.cloudpicbackend.service;

import com.pbx.cloudpicbackend.model.entity.PictureTag;
import com.baomidou.mybatisplus.extension.service.IService;


public interface PictureTagService extends IService<PictureTag> {

    void deleteByPictureId(Long id);
}
