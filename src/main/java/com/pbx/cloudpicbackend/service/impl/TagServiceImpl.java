package com.pbx.cloudpicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.exception.BusinessException;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.model.dto.tag.TagAddRequest;
import com.pbx.cloudpicbackend.model.dto.tag.TagQueryRequest;
import com.pbx.cloudpicbackend.model.entity.Tag;
import com.pbx.cloudpicbackend.model.vo.TagVO;
import com.pbx.cloudpicbackend.service.TagService;
import com.pbx.cloudpicbackend.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

    @Override
    public QueryWrapper<Tag> getQueryWrapper(TagQueryRequest tagQueryRequest) {
        if (tagQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = tagQueryRequest.getTagId() == 0 ? null : tagQueryRequest.getTagId();
        String name = tagQueryRequest.getName();
        String description = tagQueryRequest.getDescription();
        String sortField = tagQueryRequest.getSortField();
        String sortOrder = tagQueryRequest.getSortOrder();
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public List<TagVO> getPagesVoList(List<Tag> records) {
        if (CollUtil.isEmpty(records)) {
            return new ArrayList<>();
        }
        return records.stream()
                .map(this::getOneVo)
                .collect(Collectors.toList());
    }

    @Override
    public TagVO getOneVo(Tag tag) {
        TagVO tagVO = new TagVO();
        if (tag == null) return tagVO;
        BeanUtil.copyProperties(tag,tagVO);
        return tagVO;
    }

    @Override
    public Tag getTagEntity(TagAddRequest tagAddRequest) {
        Tag tag = new Tag();
        if (tagAddRequest == null) return tag;
        BeanUtil.copyProperties(tagAddRequest,tag);
        return tag;
    }

    @Override
    public void decrementUsageCount(Long tagId) {
        ThrowUtils.throwIf(tagId == null || tagId < 0, ErrorCode.PARAMS_ERROR);
        Tag byId = this.getById(tagId);
        if (byId == null || byId.getUsageCount() <= 0) return;
        byId.setUsageCount(byId.getUsageCount() - 1);
        this.updateById(byId);
    }

    @Override
    public void incrementUsageCount(Long tagId) {
        ThrowUtils.throwIf(tagId == null || tagId < 0, ErrorCode.PARAMS_ERROR);
        Tag byId = this.getById(tagId);
        if (byId == null || byId.getUsageCount() < 0) return;
        byId.setUsageCount(byId.getUsageCount() + 1);
        this.updateById(byId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchIncrementUsageCount(ArrayList<Long> longs) {
        if (CollUtil.isEmpty(longs)) {
            return;
        }

        // 逐个更新标签使用计数
        for (Long tagId : longs) {
            this.update(new UpdateWrapper<Tag>()
                    .eq("id", tagId)
                    .setSql("usage_count = usage_count + 1"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDecrementUsageCount(ArrayList<Long> longs) {
        if (CollUtil.isEmpty(longs)) {
            return;
        }
        // 逐个更新标签使用计数，确保不会变为负数
        for (Long tagId : longs) {
            this.update(new UpdateWrapper<Tag>()
                    .eq("id", tagId)
                    .setSql("usage_count = GREATEST(usage_count - 1, 0)"));
        }
    }

    @Override
    public List<Tag> listHotTags(int limit) {
        ThrowUtils.throwIf(limit < 0 || limit > 10, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.gt("usageCount", 0)
                .orderByDesc("usageCount").last("limit " + limit);
        return this.list(wrapper);
    }

    @Override
    public List<Tag> listTagsByPictureId(Long pictureId) {
        return this.baseMapper.selectTagsByPictureId(pictureId);
    }
}




