package org.example.ratelimiter.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * 对象转换工具类
 *
 * @author Percy
 * @date 2024/12/13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanUtils {
    /**
     * String 转 Object
     *
     * @param str String内容
     * @param clazz Object的类
     * @return 转换的Object对象
     * @param <T> Object的泛型
     */
    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.isEmpty() || clazz == null) {
            return null;
        }

        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else {
            return JSON.parseObject(str, clazz);
        }
    }

    /**
     * Object 转 String
     *
     * @param value Object对象
     * @return 转换后的String
     * @param <T> Object的泛型
     */
    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }

        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == long.class || clazz == Long.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        } else {
            return JSON.toJSONString(value);
        }
    }

    /**
     * Object 转化成 Map
     *
     * @param value Object
     * @param <T>   Object的泛型
     * @return 得到的 Map
     */
    public static <T> Map<String, String> beanToMap(T value) {
        if (value == null) {
            return Collections.emptyMap();
        }

        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class
                || clazz == long.class || clazz == Long.class
                || clazz == String.class) {
            return Collections.emptyMap();
        } else {
            return JSON.parseObject(JSON.toJSONString(value), new TypeReference<>() {
            });
        }
    }

    /**
     * Map 转 Object
     *
     * @param map   Map
     * @param clazz object 类型
     * @param <T>   Object的泛型
     * @return object
     */
    public static <T> T mapToBean(Map<String, String> map, Class<T> clazz) {
        if (map == null || map.isEmpty() || clazz == null) {
            return null;
        }

        if (clazz == int.class || clazz == Integer.class
                || clazz == long.class || clazz == Long.class
                || clazz == String.class) {
            return null;
        } else {
            return JSON.parseObject(JSON.toJSONString(map), clazz);
        }
    }
}
