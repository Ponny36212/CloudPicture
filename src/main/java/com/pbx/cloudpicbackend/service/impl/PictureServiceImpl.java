package com.pbx.cloudpicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pbx.cloudpicbackend.api.aliyunai.AliYunAiApi;
import com.pbx.cloudpicbackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.pbx.cloudpicbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.pbx.cloudpicbackend.exception.BusinessException;
import com.pbx.cloudpicbackend.exception.ErrorCode;
import com.pbx.cloudpicbackend.exception.ThrowUtils;
import com.pbx.cloudpicbackend.manager.CosManager;
import com.pbx.cloudpicbackend.manager.upload.FilePictureUpload;
import com.pbx.cloudpicbackend.manager.upload.PictureUploadTemplate;
import com.pbx.cloudpicbackend.manager.upload.UrlPictureUpload;
import com.pbx.cloudpicbackend.mapper.PictureMapper;
import com.pbx.cloudpicbackend.model.dto.file.UploadPictureResult;
import com.pbx.cloudpicbackend.model.enums.PictureReviewStatusEnum;
import com.pbx.cloudpicbackend.model.vo.PictureVO;
import com.pbx.cloudpicbackend.model.vo.TagVO;
import com.pbx.cloudpicbackend.model.vo.UserVO;
import com.pbx.cloudpicbackend.utils.ColorSimilarUtils;
import com.pbx.cloudpicbackend.utils.ColorTransformUtils;
import com.pbx.cloudpicbackend.model.dto.picture.*;
import com.pbx.cloudpicbackend.model.entity.*;
import com.pbx.cloudpicbackend.service.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService { // 继承 MyBatis-Plus 的通用实现，减少样板代码：无需为每个实体类重复编写基础的 CRUD 实现。

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Autowired
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private TagService tagService;

    @Resource
    private CategoryService categoryService;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return this.uploadPictureByCategory(inputSource, pictureUploadRequest, loginUser, null);
    }

    @Override
    public PictureVO uploadPictureByCategory(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser, Category category) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 改为使用统一的权限校验
//            // 校验是否有空间的权限，仅空间管理员才能上传
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 改为使用统一的权限校验
//            // 仅本人或管理员可编辑图片
//            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId（这样也兼容了公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原图片的空间 id 一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId); // 指定空间 id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setUserId(loginUser.getId());
        // 检查是否有分类
        if (category != null && category.getId() != null) {
            picture.setCategory(category.getId());
        }
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO uploadAvatar(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 上传头像，得到图片信息
        // 按照用户 id 分配地址
        String uploadPathPrefix = String.format("public/avatar/%s", loginUser.getId());


        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置标签为 头像
        picture.setCategory(1L);
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 开启事务
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getFullById(userId);
            Category category = categoryService.getById(picture.getCategory());
            if (category == null) {
                category = new Category();
                category.setName("未分类");
                category.setId(0L);
            }
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setTags(tagService.getPagesVoList(picture.getTagInfo()));
            if (StrUtil.isBlankIfStr(category.getName())) category.setName("未分类");
            pictureVO.setCategoryName(category.getName());
            pictureVO.setCategory(category.getId());
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> {
                    PictureVO pictureVO = new PictureVO();
                    BeanUtils.copyProperties(picture, pictureVO);

                    // 处理标签信息
                    if (picture.getTagInfo() != null) {
                        List<TagVO> tagVOList = picture.getTagInfo().stream()
                                .map(TagVO::objToVo)
                                .collect(Collectors.toList());
                        pictureVO.setTags(tagVOList);
                    }

                    // 处理分类信息
                    if (picture.getCategoryInfo() != null) {
                        pictureVO.setCategory(picture.getCategoryInfo().getId());
                        pictureVO.setCategoryName(picture.getCategoryInfo().getName());
                    }

                    return pictureVO;
                })
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        Long categoryId = pictureQueryRequest.getCategory();
        List<Object> tagList = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("p.name", searchText)
                            .or()
                            .like("p.introduction", searchText)
            );
        }
        // 基本条件查询
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "p.id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "p.userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "p.spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "p.spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "p.name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "p.introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "p.picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "p.reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "p.picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "p.picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "p.picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "p.picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "p.reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "p.reviewerId", reviewerId);
        queryWrapper.eq("p.isDelete", 0);   // 避免查询已删除的图片

        // 时间范围查询
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "p.editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "p.editTime", endEditTime);

        // 标签查询 - 匹配任一标签 tagList 3种可能 1.TagVO(Map) 2.name 3.tagId
        if (CollUtil.isNotEmpty(tagList)) {
            List<Long> tagIds = new ArrayList<>();
            List<String> tagNames = new ArrayList<>();

            for (Object tagObj : tagList) {
                if (tagObj == null) continue;
                try {
                    // 尝试获取id属性值
                    Long idd = null;
                    if (String.class.isInstance(tagObj)) {
                        // 尝试转为 Long，如果无法强转为 Long，则为name
                        idd = Long.parseLong(tagObj.toString());
                    }
                    if (Map.class.isInstance(tagObj)) {
                        idd = Long.parseLong((String) ((Map<?, ?>) tagObj).get("id"));
                    }
                    // 如果成功获取ID，添加到tagIds列表
                    if (idd != null) {
                        tagIds.add(idd);
                        continue;
                    }
                } catch (Exception ignored) {
                    // 如果获取ID失败，当作name处理
                }

                // 处理为字符串name
                tagNames.add(tagObj.toString());
            }

            // 处理ID查询
            if (CollUtil.isNotEmpty(tagIds)) {
                queryWrapper.exists(
                        String.format(
                                "SELECT 1 FROM picture_tag pt WHERE pt.pictureId = p.id AND pt.tagId IN (%s)",
                                tagIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                        )
                );
            }
            // 处理名称模糊查询
            if (CollUtil.isNotEmpty(tagNames)) {
                StringBuilder likeCondition = new StringBuilder();
                for (int i = 0; i < tagNames.size(); i++) {
                    if (i > 0) likeCondition.append(" OR ");
                    String escapedTag = tagNames.get(i).replace("'", "''");
                    likeCondition.append("t.name LIKE '%").append(escapedTag).append("%'");
                }
                queryWrapper.exists(
                        "SELECT 1 FROM picture_tag pt JOIN tag t ON pt.tagId = t.id " +
                                "WHERE pt.pictureId = p.id AND (" + likeCondition + ")"
                );
            }
        }

        // 分类查询
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq("c.id", categoryId);
        }
        // 排序
        if (StrUtil.isNotEmpty(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(true, isAsc, "p." + sortField);
        } else {
            // 默认按更新时间降序排列
            queryWrapper.orderByDesc("p.updateTime");
        }

        return queryWrapper;
    }


    // 审查
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在
        Picture oldPicture = this.getFullById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复，已是改状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 4. 数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        /*
        * key  upload:用户id
        * value 0
        * */
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        Category category = new Category();
        category.setId(pictureUploadByBatchRequest.getCategory());
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // https://tse4-mm.cn.bing.net/th/id/OIP-C.HmUk9vtCQyAWmNusIQt4bQAAAA?w=129&h=162
            // https://tse4-mm.cn.bing.net/th/id/OIP-C.HmUk9vtCQyAWmNusIQt4bQAAAA
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPictureByCategory(fileUrl, pictureUploadRequest, loginUser, category);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        // 修改value
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);   // 只会查询当前的表没法级联查询
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作picture数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
            if (oldPicture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            // 先更新tag表中对应字段
            List<Tag> tagInfo = tagService.listTagsByPictureId(oldPicture.getId());
            for(Tag tag : tagInfo){
                tagService.decrementUsageCount(tag.getId());
            }
            // 再操作picture_tag数据表
            pictureTagService.deleteByPictureId(oldPicture.getId());
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    // 编辑图片
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        ThrowUtils.throwIf(pictureEditRequest == null, ErrorCode.PARAMS_ERROR);
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 拿到图片的分类id
        Long category = pictureEditRequest.getCategory();
        if (category == null || category < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片未分类");
        }

        // 拿到编辑图片的所有tags
        List<String> tagInputs = pictureEditRequest.getTags();
        if (tagInputs == null) tagInputs = new ArrayList<>();

        // 处理标签：将输入的标签转换为实际的标签ID
        List<Long> processedTagIds = new ArrayList<>();

        for (String tagInput : tagInputs) {
            // 尝试将输入解析为Long (tagId)
            try {
                Long tagId = Long.parseLong(tagInput);
                // 检查数据库是否存在此ID的标签
                Tag tag = tagService.getById(tagId);
                if (tag != null) {
                    // 标签存在，使用此ID
                    processedTagIds.add(tagId);
                    continue;
                }
                // ID不存在，按照字符串处理
            } catch (NumberFormatException e) {
                // 解析失败，按字符串处理
            }

            // 以字符串处理：查找是否有同名标签
            QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
            tagQueryWrapper.eq("name", tagInput);
            Tag existingTag = tagService.getOne(tagQueryWrapper);

            if (existingTag != null) {
                // 已存在同名标签，使用其ID
                processedTagIds.add(existingTag.getId());
            } else {
                // 创建新标签
                Tag newTag = new Tag();
                newTag.setName(tagInput);
                newTag.setCreateTime(new Date());
                newTag.setUsageCount(1L); // 首次使用
                tagService.save(newTag);
                processedTagIds.add(newTag.getId());
            }
        }

        // 获取数据库中已有的标签关联
        QueryWrapper<PictureTag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureEditRequest.getId());
        List<PictureTag> existingPictureTags = pictureTagService.list(queryWrapper);

        // 获取已有的标签ID列表 existingPictureTags -> existingTagIds
        List<Long> existingTagIds = existingPictureTags.stream()
                .map(PictureTag::getTagid)
                .collect(Collectors.toList());

        // 计算需要删除的标签关联
        List<Long> toRemoveIds = existingTagIds.stream()
                .filter(tagId -> !processedTagIds.contains(tagId))
                .collect(Collectors.toList());

        // 删除不再需要的标签关联
        if (!toRemoveIds.isEmpty()) {
            QueryWrapper<PictureTag> removeWrapper = new QueryWrapper<>();
            removeWrapper.eq("pictureId", pictureEditRequest.getId())
                    .in("tagId", toRemoveIds);
            pictureTagService.remove(removeWrapper);
        }

        // 计算需要新增的标签关联
        List<Long> toAddIds = processedTagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .collect(Collectors.toList());

        // 新增标签关联 插入数据库
        if (!toAddIds.isEmpty()) {
            List<PictureTag> newPictureTags = toAddIds.stream()
                    .map(tagId -> {
                        PictureTag pictureTag = new PictureTag();
                        pictureTag.setPictureId(pictureEditRequest.getId());
                        pictureTag.setTagid(tagId);
                        return pictureTag;
                    })
                    .collect(Collectors.toList());

            pictureTagService.saveBatch(newPictureTags);
        }

        // 更新标签使用计数
        // 减少计数
        if (!toRemoveIds.isEmpty()) {
            toRemoveIds.forEach(tagId -> tagService.decrementUsageCount(tagId));
        }

        // 增加计数 - 对于新创建的标签已在前面设置了初始使用计数为1
        if (!toAddIds.isEmpty()) {
            toAddIds.forEach(tagId -> tagService.incrementUsageCount(tagId));
        }

        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());
        // 5. 返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        Long categoryId = pictureEditByBatchRequest.getCategory();
        List<Long> tagIds = pictureEditByBatchRequest.getTags();

        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }

        // 3. 查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return;
        }

        // 4. 更新分类和标签 更新图片基本信息
        Date now = new Date();
        List<Picture> updatedPictureList = pictureList.stream().map(picture -> {
            // 仅更新需要更新的字段
            Picture updatedPicture = new Picture();
            updatedPicture.setId(picture.getId());

            // 只有在请求中指定了分类时才更新
            if (categoryId != null) {
                updatedPicture.setCategory(categoryId);
            }

            // 设置编辑时间
            updatedPicture.setEditTime(now);
            updatedPicture.setUpdateTime(now);

            // 补充审核参数
            this.fillReviewParams(updatedPicture, loginUser);

            return updatedPicture;
        }).collect(Collectors.toList());

        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        if (StrUtil.isNotBlank(nameRule)) {
            fillPictureWithNameRule(updatedPictureList, nameRule);
        }

        // 5. 操作数据库进行批量更新
        boolean result = this.updateBatchById(updatedPictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量更新图片信息失败");

        // 6. 处理标签更新
        if (CollUtil.isNotEmpty(tagIds)) {
            // 为每张图片更新标签
            List<PictureTag> toAddList = new ArrayList<>();
            Set<Long> toRemoveTagIds = new HashSet<>(); // 使用Set避免重复
            Set<Long> toAddTagIds = new HashSet<>(); // 使用Set避免重复

            for (Long pictureId : pictureIdList) {
                // 6.1 获取数据库中已有的标签关联
                QueryWrapper<PictureTag> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("pictureId", pictureId);
                List<PictureTag> existingPictureTags = pictureTagService.list(queryWrapper);
                // 6.2 获取已有的标签ID列表
                List<Long> existingTagIds = existingPictureTags.stream()
                        .map(PictureTag::getTagid)
                        .collect(Collectors.toList());
                // 6.3 计算需要删除的标签关联
                List<Long> toRemoveIds = existingTagIds.stream()
                        .filter(tagId -> !tagIds.contains(tagId))
                        .collect(Collectors.toList());
                // 6.4 删除不再需要的标签关联
                if (!toRemoveIds.isEmpty()) {
                    QueryWrapper<PictureTag> removeWrapper = new QueryWrapper<>();
                    removeWrapper.eq("pictureId", pictureId)
                            .in("tagId", toRemoveIds);
                    pictureTagService.remove(removeWrapper);
                }
                // 6.5 计算需要新增的标签关联
                List<Long> toAddIds = tagIds.stream()
                        .filter(tagId -> !existingTagIds.contains(tagId))
                        .collect(Collectors.toList());
                // 6.6 新增标签关联
                if (!toAddIds.isEmpty()) {
                    List<PictureTag> newPictureTags = toAddIds.stream()
                            .map(tagId -> {
                                PictureTag pictureTag = new PictureTag();
                                pictureTag.setPictureId(pictureId);
                                pictureTag.setTagid(tagId);
                                return pictureTag;
                            })
                            .collect(Collectors.toList());
                    toAddList.addAll(newPictureTags);
                }

                toRemoveTagIds.addAll(toRemoveIds);
                toAddTagIds.addAll(toAddIds);
            }

            // 批量操作
            if (!toAddList.isEmpty()) {
                pictureTagService.saveBatch(toAddList);
            }

            // 批量更新使用计数
            if (!toAddTagIds.isEmpty()) {
                tagService.batchIncrementUsageCount(new ArrayList<>(toAddTagIds));
            }
            if (!toRemoveTagIds.isEmpty()) {
                tagService.batchDecrementUsageCount(new ArrayList<>(toRemoveTagIds));
            }
        }
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) throws MalformedURLException {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限
        // 创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    /**
     * 查询完整的图片信息 - 联查
     *
     * @param id 图片 id
     * @return 完整的图片信息
     */
    @Override
    public Picture getFullById(long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        return pictureMapper.getFullById(id);
    }

    /**
     * 分页查询图片并包含分类和标签信息
     *
     * @param page
     * @param queryWrapper
     * @return
     */
    @Override
    public Page<Picture> selectFullPage(Page<Picture> page, Wrapper<Picture> queryWrapper) {
        return pictureMapper.selectFullPage(page, queryWrapper);
    }

    /**
     * 检查是否存在指定分类ID的图片
     *
     * @param categoryIds 分类ID列表
     * @return 如果存在则返回true，否则返回false
     */
    @Override
    public boolean existsByCategoryIds(List<Long> categoryIds) {
        if (CollUtil.isEmpty(categoryIds)) {
            return false;
        }
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("category", categoryIds);

        return this.count(queryWrapper) > 0;
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }
}