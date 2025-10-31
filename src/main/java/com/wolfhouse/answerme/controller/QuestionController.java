package com.wolfhouse.answerme.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wolfhouse.answerme.ai.AiManager;
import com.wolfhouse.answerme.ai.Prompts;
import com.wolfhouse.answerme.ai.dto.AiGenerateQuestionRequest;
import com.wolfhouse.answerme.annotation.AuthCheck;
import com.wolfhouse.answerme.common.BaseResponse;
import com.wolfhouse.answerme.common.DeleteRequest;
import com.wolfhouse.answerme.common.ErrorCode;
import com.wolfhouse.answerme.common.ResultUtils;
import com.wolfhouse.answerme.constant.UserConstant;
import com.wolfhouse.answerme.exception.BusinessException;
import com.wolfhouse.answerme.exception.ThrowUtils;
import com.wolfhouse.answerme.model.dto.question.*;
import com.wolfhouse.answerme.model.entity.App;
import com.wolfhouse.answerme.model.entity.Question;
import com.wolfhouse.answerme.model.entity.User;
import com.wolfhouse.answerme.model.enums.AppTypeEnum;
import com.wolfhouse.answerme.model.vo.QuestionVO;
import com.wolfhouse.answerme.service.AppService;
import com.wolfhouse.answerme.service.QuestionService;
import com.wolfhouse.answerme.service.UserService;
import com.wolfhouse.answerme.utils.AiJsonUtils;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;
    @Resource
    private AppService appService;
    @Autowired
    private AiManager aiManager;

// region 增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse
        <Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        //  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        List<QuestionContentDto> questionContent = questionAddRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContent));
        BeanUtils.copyProperties(questionAddRequest, question);
        // 数据校验
        questionService.validQuestion(question, true);
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse
        <Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId()
                        .equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse
        <Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<QuestionContentDto> questionContent = questionUpdateRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContent));
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse
        <QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                                                           questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                                                           questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                                                           questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse
        <Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest,
                               HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<QuestionContentDto> questionContent = questionEditRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContent));
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId()
                        .equals(loginUser.getId()) &&
            !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion

    // region AI 生成题目功能

    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContentDto>> aiGenerateQuestion(@RequestBody AiGenerateQuestionRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = request.getAppId();
        int questionNumber = request.getQuestionNumber();
        int optionNumber = request.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = Prompts.userQuestionInput(app.getAppName(),
                                                       app.getAppDesc(),
                                                       AppTypeEnum.getEnumByValue(app.getAppType()),
                                                       questionNumber,
                                                       optionNumber);
        // Ai 生成
        String result = aiManager.doSyncRequest(Prompts.systemQuestionPrompt(), userMessage, null);
        List<QuestionContentDto> dtoList = AiJsonUtils.toList(result, QuestionContentDto.class);
        return ResultUtils.success(dtoList);
    }

    @GetMapping("/ai_generate/sse")
    public SseEmitter aiGenerateQuestionSse(AiGenerateQuestionRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = request.getAppId();
        int questionNumber = request.getQuestionNumber();
        int optionNumber = request.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = Prompts.userQuestionInput(app.getAppName(),
                                                       app.getAppDesc(),
                                                       AppTypeEnum.getEnumByValue(app.getAppType()),
                                                       questionNumber,
                                                       optionNumber);
        // 建立 SSE 对象, 0L 永不超时
        SseEmitter emitter = new SseEmitter(0L);
        // Ai 生成
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(Prompts.systemQuestionPrompt(),
                                                                          userMessage,
                                                                          null);

        // 左括号计数器，回归为 0 时表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        // 拼接完整题目
        StringBuilder stringBuilder = new StringBuilder();

        modelDataFlowable
            .observeOn(Schedulers.io())
            .map(modelData -> modelData.getChoices()
                                       .get(0)
                                       .getDelta()
                                       .getContent())
            .map(msg -> msg.replaceAll("\\s", ""))
            .filter(StrUtil::isNotBlank)
            .flatMap(msg -> {
                List<Character> charList = msg.chars()
                                              .mapToObj(c -> (char) c)
                                              .collect(Collectors.toList());
                return Flowable.fromIterable(charList);
            })
            .doOnNext(c -> {
                // 有内容需要拼接
                // 如果是 '{'，计数器 + 1
                if (c.equals('{')) {
                    counter.incrementAndGet();
                    stringBuilder.append(c);
                    return;
                }
                if (counter.get() > 0) {
                    stringBuilder.append(c);
                }
                if (c.equals('}')) {
                    counter.decrementAndGet();
                    if (counter.get() == 0) {
                        // 有一道完整的题目，通过 SSE 返回
                        emitter.send(stringBuilder.toString());
                        // 重置
                        stringBuilder.setLength(0);
                    }
                }

            })
            .doOnError(throwable -> log.error(throwable.getMessage()))
            .doOnComplete(emitter::complete)
            .subscribe();

        return emitter;
    }
    // endregion

}
