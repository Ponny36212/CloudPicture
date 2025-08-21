package com.pbx.cloudpicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pbx.cloudpicbackend.model.dto.tag.TagAddRequest;
import com.pbx.cloudpicbackend.model.dto.tag.TagQueryRequest;
import com.pbx.cloudpicbackend.model.entity.Tag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pbx.cloudpicbackend.model.vo.TagVO;

import java.util.ArrayList;
import java.util.List;

public interface TagService extends IService<Tag> {

    /**
     * 获取QueryWrapper
     * @param tagQueryRequest tag请求
     * @return QueryWrapper
     */
    QueryWrapper<Tag> getQueryWrapper(TagQueryRequest tagQueryRequest);

    /**
     * 快速转换VO
     * @param records page的记录
     * @return TagVO
     */
    List<TagVO> getPagesVoList(List<Tag> records);

    /**
     * 获取VO
     * @param tag
     * @return TagVO
     */
    TagVO getOneVo(Tag tag);

    /**
     * 获取TagEntity
     * @param tagAddRequest
     * @return TagEntity
     */
    Tag getTagEntity(TagAddRequest tagAddRequest);

    /**
     * 减少使用数量
     * @param tagId
     */
    void decrementUsageCount(Long tagId);

    /**
     * 增加使用数量
     * @param tagId
     */
    void incrementUsageCount(Long tagId);

    /**
     * 批量增加使用数量
     * @param longs
     */
    void batchIncrementUsageCount(ArrayList<Long> longs);

    /**
     * 批量减少使用数量
     * @param longs
     */
    void batchDecrementUsageCount(ArrayList<Long> longs);

    /**
     * 热点 tag
     */
    List<Tag> listHotTags(int limit);

    /**
     * 根据图片id获取标签列表
     */
    List<Tag> listTagsByPictureId(Long pictureId);
}
