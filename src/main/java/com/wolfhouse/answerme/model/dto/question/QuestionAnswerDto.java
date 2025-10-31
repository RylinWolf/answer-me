package com.wolfhouse.answerme.model.dto.question;

import lombok.Data;

/**
 * 题目答案封装类（用于 AI 评分）
 *
 * @author Rylin Wolf
 */
@Data
public class QuestionAnswerDto {
    private String title;
    private String userAnswer;
}
