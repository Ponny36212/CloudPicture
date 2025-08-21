package com.pbx.cloudpicbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.model.dto.category.CategoryQueryRequest;
import com.pbx.cloudpicbackend.model.entity.Category;
import com.pbx.cloudpicbackend.model.vo.CategoryVO;
import com.pbx.cloudpicbackend.service.CategoryService;
import com.pbx.cloudpicbackend.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService {

    @Override
    public List<CategoryVO> getVOByList(List<Category> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        return list.stream().map(category ->
                new CategoryVO(category.getId(), category.getName(), category.getDescription(), category.getCreateTime(), category.getUpdateTime())).collect(Collectors.toList());

    }

    @Override
    public Wrapper<Category> getQueryWrapper(CategoryQueryRequest categoryQueryRequest) {
        if (categoryQueryRequest == null) return null;
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(categoryQueryRequest.getName())) {
            queryWrapper.like("name", categoryQueryRequest.getName());
        }
        if (StrUtil.isNotBlank(categoryQueryRequest.getDescription())) {
            queryWrapper.like("description", categoryQueryRequest.getDescription());
        }
        return queryWrapper;
    }
}




