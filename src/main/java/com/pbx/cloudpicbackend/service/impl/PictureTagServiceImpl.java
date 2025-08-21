package com.pbx.cloudpicbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.model.entity.PictureTag;
import com.pbx.cloudpicbackend.service.PictureTagService;
import com.pbx.cloudpicbackend.mapper.PictureTagMapper;
import org.springframework.stereotype.Service;


@Service
public class PictureTagServiceImpl extends ServiceImpl<PictureTagMapper, PictureTag>
    implements PictureTagService {

    @Override
    public void deleteByPictureId(Long pictureId) {
        LambdaQueryWrapper<PictureTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureTag::getPictureId, pictureId);
        this.remove(wrapper);
    }
}




