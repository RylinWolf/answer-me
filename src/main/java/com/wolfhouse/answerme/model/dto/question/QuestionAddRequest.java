package com.wolfhouse.answerme.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建题目请求
 *
 * @author Rylin Wolf
 */
@Data
public class QuestionAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 题目内容（json格式）
     */
    private List<QuestionContentDto> questionContent;
    /**
     * 应用 id
     */
    private Long appId;
}