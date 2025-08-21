package com.pbx.cloudpicbackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pbx.cloudpicbackend.annotation.AuthCheck;
import com.pbx.cloudpicbackend.common.BaseResponse;
import com.pbx.cloudpicbackend.common.ResultUtils;
import com.pbx.cloudpicbackend.constant.UserConstant;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.model.dto.tag.TagAddRequest;
import com.pbx.cloudpicbackend.model.dto.tag.TagQueryRequest;
import com.pbx.cloudpicbackend.model.dto.tag.TagUpdateRequest;
import com.pbx.cloudpicbackend.model.entity.Tag;
import com.pbx.cloudpicbackend.model.enums.TagTypeEnum;
import com.pbx.cloudpicbackend.model.vo.TagVO;
import com.pbx.cloudpicbackend.service.TagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private TagService tagService;

    /**
     * 新增 tag
     * @param tagAddRequest
     * @return 新增标签id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Long> addTag(@RequestBody TagAddRequest tagAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(tagAddRequest == null ||
                StrUtil.isBlankIfStr(tagAddRequest.getName()), ErrorCode.PARAMS_ERROR);

        // 创建标签实体
        Tag tagEntity = tagService.getTagEntity(tagAddRequest);

        // 设置标签类型
        tagEntity.setType(TagTypeEnum.getByNameOrDefault(
                tagAddRequest.getType(),
                TagTypeEnum.NORMAL).getName());

        // 保存并返回结果
        boolean save = tagService.save(tagEntity);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(tagEntity.getId());
    }


    /**
     * 更新 tag
     * @param tagUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateTag(@RequestBody TagUpdateRequest tagUpdateRequest) {
        // 参数校验
        ThrowUtils.throwIf(tagUpdateRequest == null ||
                        StrUtil.isBlankIfStr(tagUpdateRequest.getId()), ErrorCode.PARAMS_ERROR);
        Tag tagEntity = tagService.getTagEntity(tagUpdateRequest);
        ThrowUtils.throwIf(StrUtil.isBlankIfStr(tagUpdateRequest.getName()), ErrorCode.PARAMS_ERROR);
        TagTypeEnum tagTypeEnum = TagTypeEnum.getByName(tagUpdateRequest.getType());
        ThrowUtils.throwIf(tagTypeEnum == null || StrUtil.isBlank(tagTypeEnum.getName()), ErrorCode.PARAMS_ERROR);
        tagEntity.setType(tagTypeEnum.getName());
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.eq("id", tagEntity.getId());
        boolean save = tagService.update(tagEntity,wrapper);
        return ResultUtils.success(save);
    }

    /**
     * 删除 tag
     * @param id
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTag(@RequestParam Long id) {
        return ResultUtils.success(tagService.removeById(id));
    }

    /**
     * 分页获取 tag
     * @param tagQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<TagVO>> listTags(@RequestBody TagQueryRequest tagQueryRequest) {
        long current = tagQueryRequest.getCurrent();
        long pageSize = tagQueryRequest.getPageSize();
        Page<Tag> page = tagService.page(new Page<>(current, pageSize), tagService.getQueryWrapper(tagQueryRequest));
        Page<TagVO> tagVoPage = new Page<>(current, pageSize, page.getTotal());
        List<TagVO> pageVoList = tagService.getPagesVoList(page.getRecords());
        tagVoPage.setRecords(pageVoList);
        return ResultUtils.success(tagVoPage);
    }
}
