-- 创建库
create database if not exists pic_cloud;

-- 切换库
use pic_cloud;

create table category
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(64)                        not null comment '分类名称',
    description varchar(256)                       null comment '分类描述',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    constraint category_index_unique_name
        unique (name)
)
    comment '分类' collate = utf8mb4_unicode_ci;

create table picture
(
    id            bigint auto_increment comment 'id'
        primary key,
    url           varchar(512)                       not null comment '图片 url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    category      bigint                             null comment '分类id',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(32)                        null comment '图片格式',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    reviewStatus  int      default 0                 not null comment '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    reviewerId    bigint                             null comment '审核人 ID',
    reviewTime    datetime                           null comment '审核时间',
    thumbnailUrl  varchar(512)                       null comment '缩略图 url',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）',
    picColor      varchar(16)                        null comment '图片主色调'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_category
    on picture (category);

create index idx_introduction
    on picture (introduction);

create index idx_name
    on picture (name);

create index idx_reviewStatus
    on picture (reviewStatus);

create index idx_spaceId
    on picture (spaceId);

create index idx_userId
    on picture (userId);

create table picture_tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    pictureId  bigint                             not null comment '图片id',
    tagid      bigint                             not null comment '标签id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_picture_tag
        unique (pictureId, tagid)
)
    comment '图片-标签关联' collate = utf8mb4_unicode_ci;

create index idx_picture_id
    on picture_tag (pictureId);

create index idx_tag_id
    on picture_tag (tagid);

create table space
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    spaceType  int      default 0                 not null comment '空间类型：0-私有 1-团队'
)
    comment '空间' collate = utf8mb4_unicode_ci;

create index idx_spaceLevel
    on space (spaceLevel);

create index idx_spaceName
    on space (spaceName);

create index idx_spaceType
    on space (spaceType);

create index idx_userId
    on space (userId);

create table space_user
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_spaceId_userId
        unique (spaceId, userId)
)
    comment '空间用户关联' collate = utf8mb4_unicode_ci;

create index idx_spaceId
    on space_user (spaceId);

create index idx_userId
    on space_user (userId);

create table tag
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(64)                                                not null comment '标签名称',
    description varchar(256)                                               null comment '标签描述',
    usageCount  bigint                           default 0                 null comment '使用次数',
    type        enum ('normal', 'hot', 'system') default 'normal'          null comment '标签类型：normal/hot/system',
    createTime  datetime                         default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime                         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint                          default 0                 not null comment '是否删除',
    constraint uk_tag_name
        unique (name)
)
    comment '标签' collate = utf8mb4_unicode_ci;

create index idx_tag_type
    on tag (type);

create index idx_tag_usage
    on tag (usageCount);

create table user
(
    id            bigint auto_increment comment 'id'
        primary key,
    userAccount   varchar(256)                           not null comment '账号',
    userPassword  varchar(512)                           not null comment '密码',
    userName      varchar(256)                           null comment '用户昵称',
    userAvatar    bigint                                 null comment '用户头像 外键',
    userProfile   varchar(512)                           null comment '用户简介',
    userRole      varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime      datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    vipExpireTime datetime                               null comment '会员过期时间',
    vipCode       varchar(128)                           null comment '会员兑换码',
    vipNumber     bigint                                 null comment '会员编号',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userName
    on user (userName);

create table user_avatar
(
    id           bigint auto_increment comment 'id'
        primary key,
    avatarUrl    varchar(512)                       null comment '头像url',
    thumbnailUrl varchar(512)                       null comment '头像缩略图',
    userId       bigint                             not null comment '用户id',
    isActive     tinyint  default 0                 not null comment '是否为当前使用的头像：0-否，1-是',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_user_active
        unique (userId, isActive, isDelete)
);

create index idx_user_id
    on user_avatar (userId);


