package com.wolfhouse.answerme.scoring;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wolfhouse.answerme.ai.AiManager;
import com.wolfhouse.answerme.ai.Prompts;
import com.wolfhouse.answerme.model.dto.question.QuestionAnswerDto;
import com.wolfhouse.answerme.model.dto.question.QuestionContentDto;
import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.Question;
import com.wolfhouse.answerme.model.entity.UserAnswer;
import com.wolfhouse.answerme.service.QuestionService;
import com.wolfhouse.answerme.utils.AiJsonUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ai 测评类应用测评类策略
 *
 * @author Rylin Wolf
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    private static final String AI_ANSWER_LOCK = "ai_answer_lock";
    /**
     * AI 评分结果本地缓存，保存 AI 评分输出
     */
    private final Cache<String, String> answerCache =
        Caffeine.newBuilder()
                .initialCapacity(1024)
                // 五分钟移除
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .build();

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    @NotNull
    private static UserAnswer getUserAnswerFromAiResult(App app, String result, Long appId, String choicesJson) {
        UserAnswer userAnswer = AiJsonUtils.toBean(result, UserAnswer.class);
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setChoices(choicesJson);
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        return userAnswer;
    }

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {

        Long appId = app.getId();
        String choicesJson = JSONUtil.toJsonStr(choices);
        String cacheKey = buildCacheKey(appId, choicesJson);
        String cachedScoreResult = answerCache.getIfPresent(cacheKey);
        if (cachedScoreResult != null) {
            // 构造返回值
            return getUserAnswerFromAiResult(app, cachedScoreResult, appId, choicesJson);
        }

        // 定义并发锁
        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);
        try {
            // 竞争锁
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            // 没抢到锁，从缓存中获取
            if (!res) {
                // 从缓存中获取数据，最大重试五次
                int retryTimes = 1;
                int maxRetryTimes = 5;
                while (retryTimes <= maxRetryTimes) {
                    String cachedResult = answerCache.getIfPresent(cacheKey);
                    if (cachedResult != null) {
                        return getUserAnswerFromAiResult(app, cachedResult, appId, choicesJson);
                    }
                    retryTimes++;
                    Thread.sleep(1000);
                }
                return null;
            }

            // 根据 ID 获取到题目信息
            Question question = questionService.getOne(new LambdaQueryWrapper<Question>().eq(Question::getAppId,
                                                                                             app.getId()));
            // 获取题目列表
            List<QuestionContentDto> questions = JSONUtil.toList(question.getQuestionContent(),
                                                                 QuestionContentDto.class);
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

            // 缓存结果
            answerCache.put(cacheKey, result);

            // 构造返回值
            return getUserAnswerFromAiResult(app, result, appId, choicesJson);
        } finally {
            if (lock != null && lock.isLocked()) {
                // 防止锁超时释放后，释放其他线程的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private String buildCacheKey(Long appId, String choices) {
        return appId + ":" + Arrays.toString(DigestUtil.md5(choices));
    }
}
