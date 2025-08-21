package com.pbx.cloudpicbackend.model.enums;


import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * tag 类型枚举
 */
@AllArgsConstructor
@Getter
public enum TagTypeEnum {
    NORMAL("normal"),
    HOT("hot"),
    SYSTEM("system");

    private final String name;

    /**
     * 根据名称获取枚举值
     * @param name 标签类型名称
     * @return 对应的枚举值，未找到则返回null
     */
    public static TagTypeEnum getByName(String name) {
        if (ObjUtil.isEmpty(name)) {
            return null;
        }
        for (TagTypeEnum tagTypeEnum : TagTypeEnum.values()) {
            if (tagTypeEnum.getName().equals(name)) {
                return tagTypeEnum;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举值，如果未找到则返回默认值
     * @param name 标签类型名称
     * @param defaultType 默认类型
     * @return 对应的枚举值或默认值
     */
    public static TagTypeEnum getByNameOrDefault(String name, TagTypeEnum defaultType) {
        TagTypeEnum type = getByName(name);
        return type != null ? type : defaultType;
    }
}
