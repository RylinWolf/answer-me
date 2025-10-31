package com.wolfhouse.answerme.utils;

import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * @author Rylin Wolf
 */
public class AiJsonUtils {
    public static <T> List<T> toList(String json, Class<T> clazz) {
        int start = json.indexOf("[");
        int end = json.lastIndexOf("]");
        json = json.substring(start, end + 1);
        return JSONUtil.toList(json, clazz);
    }

    public static <T> T toBean(String json, Class<T> clazz) {
        int start = json.indexOf("{");
        int end = json.lastIndexOf("}");
        json = json.substring(start, end + 1);
        return JSONUtil.toBean(json, clazz);
    }
}
