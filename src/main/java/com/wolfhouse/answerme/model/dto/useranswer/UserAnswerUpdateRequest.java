package com.wolfhouse.answerme.model.dto.useranswer;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新用户答案请求
 */
@Data
public class UserAnswerUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 用户答案（JSON 数组）
     */
    private List<String> choices;
}