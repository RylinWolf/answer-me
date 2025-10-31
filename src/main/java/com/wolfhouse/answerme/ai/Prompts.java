package com.wolfhouse.answerme.ai;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.json.JSONUtil;
import com.wolfhouse.answerme.model.dto.question.QuestionAnswerDto;
import com.wolfhouse.answerme.model.enums.AppTypeEnum;

import java.net.URL;
import java.util.List;

/**
 * @author Rylin Wolf
 */
public class Prompts {
    public static String userQuestionInput(String appName,
                                           String appDesc,
                                           AppTypeEnum appType,
                                           Integer questionCount,
                                           Integer optionCount) {
        appType = appType == null ? AppTypeEnum.SCORE : appType;
        return String.format(readPrompt("prompt/user-question-input.txt"),
                             appName,
                             appDesc,
                             appType.getText(),
                             questionCount,
                             optionCount);

    }

    public static String userScoringInput(String appName,
                                          String appDesc,
                                          List<QuestionAnswerDto> answers) {
        return String.format(readPrompt("prompt/user-scoring-input.txt"),
                             appName,
                             appDesc,
                             JSONUtil.toJsonStr(answers));
    }

    public static String readPrompt(String promptPath) {
        return readFile(promptPath);
    }

    public static String systemScoringPrompt() {
        return readFile("prompt/system-scoring-prompt.txt");
    }

    public static String systemQuestionPrompt() {
        return readFile("prompt/system-question-prompt.txt");
    }

    private static String readFile(String filePath) {
        URL url = Prompts.class.getClassLoader()
                               .getResource(filePath);
        if (url == null) {
            return "";
        }
        FileReader reader = new FileReader(url.getFile());
        return reader.readLines()
                     .stream()
                     .reduce("", (s1, s2) -> s1 + s2 + "\n")
                     .strip();
    }
}
