package com.wolfhouse.answerme.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wolfhouse.answerme.model.dto.question.QuestionContentDto;
import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.Question;
import com.wolfhouse.answerme.model.entity.ScoringResult;
import com.wolfhouse.answerme.model.entity.UserAnswer;
import com.wolfhouse.answerme.model.vo.QuestionVO;
import com.wolfhouse.answerme.service.QuestionService;
import com.wolfhouse.answerme.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 自定义测评类应用评分策略
 *
 * @author Rylin Wolf
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;


    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 根据 ID 查询题目和题目结果信息
        Long appId = app.getId();
        Question question = questionService.getOne(
            Wrappers.lambdaQuery(Question.class)
                    .eq(Question::getAppId, appId));

        List<ScoringResult> scoringResults = scoringResultService.list(
            new LambdaQueryWrapper<>(ScoringResult.class)
                .eq(ScoringResult::getAppId, appId)
                // 根据分数范围从高到低排序
                .orderByDesc(ScoringResult::getResultScoreRange));

        // 2. 统计用户总得分
        int totalScore = 0;
        List<QuestionContentDto> questionContent = QuestionVO.objToVo(question)
                                                             .getQuestionContent();
        // 遍历题目列表
        // 确保答案列表和题目列表数量一致
        int size = Math.min(choices.size(), questionContent.size());

        // 遍历每道题目和对应的答案
        for (int i = 0; i < size; i++) {
            String answer = choices.get(i);
            QuestionContentDto questionDto = questionContent.get(i);

            // 遍历当前题目的选项
            for (QuestionContentDto.Option option : questionDto.getOptions()) {
                // 如果答案和选项的 key 匹配
                if (option.getKey()
                          .equals(answer)) {
                    // 累加题目选项分数
                    totalScore += Optional.ofNullable(option.getScore())
                                          .orElse(0);

                    // 找到匹配的选项后，跳出当前题目的选项循环
                    break;
                }
            }
        }
        // 3. 遍历得分结果，找到第一个用户大于此分数的结果
        ScoringResult maxScoringResult = scoringResults.get(0);

        for (ScoringResult result : scoringResults) {
            if (totalScore >= result.getResultScoreRange()) {
                maxScoringResult = result;
                break;
            }
        }

        // 4. 构造返回值，填充
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);

        return userAnswer;
    }
}
