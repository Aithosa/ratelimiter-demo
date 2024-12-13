package org.example.ratelimiter.common.constant;

/**
 * 开关枚举
 * 控制限流器是否生效
 * NOTE: 不是所有接口都需要限流器;不过也不是所有接口都需要添加逻辑
 *
 * @author Percy
 * @date 2024/12/10
 */
public enum SwitchEnum {
    /**
     * 开启状态
     */
    ON("1", "ON"),

    /**
     * 关闭状态
     */
    OFF("0", "OFF");

    SwitchEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private final String code;

    private final String desc;

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String getDesc(String code) {
        SwitchEnum[] enums = SwitchEnum.values();
        for (SwitchEnum switchEnum : enums) {
            if (switchEnum.getCode().equals(code)) {
                return switchEnum.getDesc();
            }
        }
        return "";
    }
}
