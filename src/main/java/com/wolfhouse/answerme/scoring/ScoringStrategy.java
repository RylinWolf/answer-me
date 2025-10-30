package com.wolfhouse.answerme.scoring;

import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 *
 * @author Rylin Wolf
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}
