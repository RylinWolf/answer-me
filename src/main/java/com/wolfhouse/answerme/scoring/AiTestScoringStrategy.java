package com.wolfhouse.answerme.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wolfhouse.answerme.ai.AiManager;
import com.wolfhouse.answerme.ai.Prompts;
import com.wolfhouse.answerme.model.dto.question.QuestionAnswerDto;
import com.wolfhouse.answerme.model.dto.question.QuestionContentDto;
import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.Question;
import com.wolfhouse.answerme.model.entity.UserAnswer;
import com.wolfhouse.answerme.service.QuestionService;
import com.wolfhouse.answerme.utils.AiJsonUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Ai 测评类应用测评类策略
 *
 * @author Rylin Wolf
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 根据 ID 获取到题目信息
        Question question = questionService.getOne(new LambdaQueryWrapper<Question>().eq(Question::getAppId,
                                                                                         app.getId()));
        // 获取题目列表
        List<QuestionContentDto> questions = JSONUtil.toList(question.getQuestionContent(), QuestionContentDto.class);
        List<QuestionAnswerDto> answers = new ArrayList<>();
        // 构建题目 - 答案列表
        for (int i = 0; i < questions.size(); i++) {
            // 获取问题 dto 及用户选项
            QuestionContentDto questionDto = questions.get(i);
            String choice = choices.get(i);
            // 构建问题答案 dto
            QuestionAnswerDto answerDto = new QuestionAnswerDto();
            String userAnswer = questionDto.getOptions()
                                           .stream()
                                           .filter(o -> o.getKey()
                                                         .equals(choice))
                                           .findFirst()
                                           .orElseThrow()
                                           .getResult();
            answerDto.setTitle(questionDto.getTitle());
            answerDto.setUserAnswer(userAnswer);
            answers.add(answerDto);
        }
        // 调用 AI 获取结果
        String userScoringInput = Prompts.userScoringInput(app.getAppName(),
                                                           app.getAppDesc(),
                                                           answers);
        String systemPrompt = Prompts.systemScoringPrompt();
        String result = aiManager.doSyncStableRequest(systemPrompt, userScoringInput);
        // 构造返回值
        return AiJsonUtils.toBean(result, UserAnswer.class);
    }
}
