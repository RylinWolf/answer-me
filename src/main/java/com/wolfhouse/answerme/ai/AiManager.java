package com.wolfhouse.answerme.ai;

import com.wolfhouse.answerme.common.ErrorCode;
import com.wolfhouse.answerme.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rylin Wolf
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiManager {
    private static final float STABLE_TEMPERATURE = 0.05f;
    private static final float UNSTABLE_TEMPERATURE = 0.99f;
    private final ClientV4 client;

    /**
     * 执行对话请求的方法。(同步 答案较不稳定)
     *
     * @param systemMessage 系统消息内容，用于设定对话的上下文或指令。
     * @param userMessage   用户消息内容，表示用户的输入或问题。
     * @return 模型返回的聊天内容字符串。
     */
    public String doSyncUnstableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, UNSTABLE_TEMPERATURE);
    }

    /**
     * 执行对话请求的方法。(同步 答案较稳定)
     *
     * @param systemMessage 系统消息内容，用于设定对话的上下文或指令。
     * @param userMessage   用户消息内容，表示用户的输入或问题。
     * @return 模型返回的聊天内容字符串。
     */
    public String doSyncStableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, STABLE_TEMPERATURE);
    }

    /**
     * 执行对话请求的方法。(同步)
     *
     * @param systemMessage 系统消息内容，用于设定对话的上下文或指令。
     * @param userMessage   用户消息内容，表示用户的输入或问题。
     * @param temperature   控制生成内容的随机性，值越高越随机，越低越倾向于确定性。
     * @return 模型返回的聊天内容字符串。
     */
    public String doSyncRequest(String systemMessage, String userMessage, Float temperature) {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, temperature);
    }

    /**
     * 执行对话请求的方法。
     *
     * @param systemMessage 系统消息内容，用于设定对话的上下文或指令。
     * @param userMessage   用户消息内容，表示用户的输入或问题。
     * @param stream        指定是否启用流式输出，设置为 true 表示启用，为 false 表示禁用。
     * @param temperature   控制生成内容的随机性，值越高越随机，越低越倾向于确定性。
     * @return 模型返回的聊天内容字符串。
     */
    public String doRequest(String systemMessage, String userMessage, Boolean stream, Float temperature) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(systemChatMessage);
        messages.add(userChatMessage);
        return doRequest(messages, stream, temperature);
    }

    /**
     * 执行对话请求的方法。(通用)
     *
     * @param messages    包含聊天消息的列表，每条消息作为模型的输入。
     * @param stream      指定是否启用流式输出，设置为 true 表示启用，为 false 表示禁用。
     * @param temperature 控制生成内容的随机性，值越高越随机，越低越倾向于确定性。
     * @return 模型返回的聊天内容字符串。
     */
    public String doRequest(List<ChatMessage> messages, Boolean stream, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                                                                           .model(Constants.ModelChatGLM4)
                                                                           .temperature(temperature)
                                                                           .stream(stream)
                                                                           .invokeMethod(Constants.invokeMethod)
                                                                           .messages(messages)
                                                                           .build();
        // 调用
        try {
            ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
            return invokeModelApiResp.getData()
                                     .getChoices()
                                     .get(0)
                                     .getMessage()
                                     .getContent()
                                     .toString();
        } catch (Exception e) {
            log.error("AI 生成失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }
}
