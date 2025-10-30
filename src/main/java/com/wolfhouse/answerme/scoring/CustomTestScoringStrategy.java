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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义测评类应用测评类策略
 *
 * @author Rylin Wolf
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {
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
                .eq(ScoringResult::getAppId, appId));

        // 2. 统计用户每个选择对应的属性个数

        // 初始化一个 Map，用于存储每个选项的计数
        Map<String, Integer> optionCount = new HashMap<>();
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDto> questions = questionVO.getQuestionContent();

        // 用户选择 A, B, C
        // 对应 result：I, I, J
        // optionCount[I] = 2; optionCount[J] = 1

        // 遍历题目列表
        // 确保答案列表和题目列表数量一致
        int size = Math.min(choices.size(), questions.size());

        // 遍历每道题目和对应的答案
        for (int i = 0; i < size; i++) {
            String answer = choices.get(i);
            QuestionContentDto questionDto = questions.get(i);

            // 遍历当前题目的选项
            for (QuestionContentDto.Option option : questionDto.getOptions()) {
                // 如果答案和选项的 key 匹配
                if (option.getKey()
                          .equals(answer)) {
                    // 获取选项的 result 属性
                    String result = option.getResult();

                    // 在 optionCount 中增加计数
                    optionCount.put(result, optionCount.getOrDefault(result, 0) + 1);

                    // 找到匹配的选项后，跳出当前题目的选项循环
                    break;
                }
            }
        }
        // 3. 计算每个属性对应的分数
        // 初始化最高分数和最高分数对应的评分结果
        int maxScore = 0;
        ScoringResult maxScoringResult = scoringResults.get(0);

        // 遍历评分结果列表
        for (ScoringResult result : scoringResults) {
            // 计算当前评分结果的分数
            List<String> resultPropList = JSONUtil.toList(result.getResultProp(), String.class);
            int score = resultPropList.stream()
                                      .mapToInt(prop -> optionCount.getOrDefault(prop, 0))
                                      .sum();

            // 如果分数高于当前最高分数，更新最高分数和最高分数对应的评分结果
            if (score > maxScore) {
                // 最高分数和最高分数对应的评分结果
                maxScore = score;
                maxScoringResult = result;
            }
        }

        // 4. 构造返回值，返回答案对象
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        return userAnswer;
    }
}
