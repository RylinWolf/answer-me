package com.wolfhouse.answerme.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 题目内容 DTO
 *
 * @author Rylin Wolf
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionContentDto {
    /**
     * 题目标题
     */
    private String title;

    /**
     * 选项列表
     */
    private List<Option> options;

    /**
     * 题目选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        /**
         * 选项 key
         */
        private String key;

        /**
         * 选项内容
         */
        private String value;

        /**
         * 如果是测评类，则用 result 来保存答案属性
         */
        private String result;

        /**
         * 如果是得分类，则用 score 来设置本题分数
         */
        private Integer score;
    }
}