package com.wolfhouse.answerme.scoring;

import com.wolfhouse.answerme.common.ErrorCode;
import com.wolfhouse.answerme.exception.BusinessException;
import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.UserAnswer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Rylin Wolf
 */
@Service
public class ScoringStrategyExecutor {

    /** 策略列表 */
    @Resource
    private List<ScoringStrategy> scoringStrategyList;


    /**
     * 评分
     *
     * @param choiceList
     * @param app
     * @return
     * @throws Exception
     */
    public UserAnswer doScore(List<String> choiceList, App app) throws Exception {
        Integer appType = app.getAppType();
        Integer appScoringStrategy = app.getScoringStrategy();
        if (appType == null || appScoringStrategy == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 根据注解获取策略
        for (ScoringStrategy strategy : scoringStrategyList) {
            if (strategy.getClass()
                        .isAnnotationPresent(ScoringStrategyConfig.class)) {
                ScoringStrategyConfig scoringStrategyConfig = strategy.getClass()
                                                                      .getAnnotation(ScoringStrategyConfig.class);
                if (scoringStrategyConfig.appType() == appType &&
                    scoringStrategyConfig.scoringStrategy() == appScoringStrategy) {
                    return strategy.doScore(choiceList, app);
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }
}
