package com.pbx.cloudpicbackend.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pbx.cloudpicbackend.annotation.AuthCheck;
import com.pbx.cloudpicbackend.common.BaseResponse;
import com.pbx.cloudpicbackend.common.ResultUtils;
import com.pbx.cloudpicbackend.constant.UserConstant;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.model.dto.category.CategoryAddRequest;
import com.pbx.cloudpicbackend.model.dto.category.CategoryQueryRequest;
import com.pbx.cloudpicbackend.model.dto.category.CategoryUpdateRequest;
import com.pbx.cloudpicbackend.model.entity.Category;
import com.pbx.cloudpicbackend.model.vo.CategoryVO;
import com.pbx.cloudpicbackend.service.CategoryService;
import com.pbx.cloudpicbackend.service.PictureService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Resource
    private PictureService pictureService;

    /**
     * 分页获取分类
     * @param categoryQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CategoryVO>> listCategories(@RequestBody CategoryQueryRequest categoryQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(categoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = categoryQueryRequest.getCurrent();
        long pageSize = categoryQueryRequest.getPageSize();
        Page<Category> page = categoryService.page(new Page<>(current, pageSize), categoryService.getQueryWrapper(categoryQueryRequest));
        Page<CategoryVO> tagVoPage = new Page<>(current, pageSize, page.getTotal());
        List<CategoryVO> pageVoList = categoryService.getVOByList(page.getRecords());
        tagVoPage.setRecords(pageVoList);
        return ResultUtils.success(tagVoPage);
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addCategory(@RequestBody CategoryAddRequest categoryAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(categoryAddRequest == null || StrUtil.isBlank(categoryAddRequest.getName()), ErrorCode.PARAMS_ERROR);
        Category category = new Category();
        category.setName(categoryAddRequest.getName());
        category.setDescription(categoryAddRequest.getDescription());
        boolean save = categoryService.save(category);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(category.getId());
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@RequestBody CategoryUpdateRequest categoryUpdateRequest) {
        // 参数校验
        ThrowUtils.throwIf(categoryUpdateRequest == null ||
                categoryUpdateRequest.getId() == null ||
                categoryUpdateRequest.getId() < 0 ||
                StrUtil.isBlank(categoryUpdateRequest.getName()), ErrorCode.PARAMS_ERROR);
        Category category = new Category();
        category.setId(categoryUpdateRequest.getId());
        category.setName(categoryUpdateRequest.getName());
        category.setDescription(categoryUpdateRequest.getDescription());
        boolean b = categoryService.updateById(category);
        return ResultUtils.success(b);
    }

    @PostMapping("/deleteBatch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        // 参数校验
        if (ids == null || ids.isEmpty()) return ResultUtils.success(true);
        List<Long> validIds = ids.stream()
                .filter(id -> id > 0)
                .collect(Collectors.toList());
        ThrowUtils.throwIf(validIds.isEmpty(), ErrorCode.PARAMS_ERROR, "无有效的分类ID");
        // 检查是否有图片关联到这些分类
        boolean hasPictures = pictureService.existsByCategoryIds(validIds);
        ThrowUtils.throwIf(hasPictures, ErrorCode.PARAMS_ERROR, "分类下存在图片，无法删除");

        boolean b = categoryService.removeByIds(validIds);
        return ResultUtils.success(b);
    }

}
