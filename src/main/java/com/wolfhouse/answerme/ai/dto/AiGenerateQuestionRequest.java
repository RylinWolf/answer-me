package com.wolfhouse.answerme.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Rylin Wolf
 */
@Data
public class AiGenerateQuestionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 应用 ID */
    private Long appId;

    /** 题目数 */
    private int questionNumber = 10;

    /** 选项数 */
    private int optionNumber = 2;
}
