package com.alibaba.cloud.ai.common;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;

public class CommonUtils {

    public static final String URL = "http://996code.top:11434";
    public static final String MODEL = "qwen3:1.7b";


    public static ReactAgent getReactAgent(String my_agent) {

        ChatModel chatModel = getChatModel();

        ReactAgent agent = ReactAgent.builder()
                .name(my_agent)
                .model(chatModel)
                // 限制最多调用 5 次
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
                // 使用自定义停止条件
//                .hooks(new CustomStopConditionHook())
                .build();

        return agent;
    }

    @NotNull
    public static ChatModel getChatModel() {
        OllamaApi build = OllamaApi.builder()
                .baseUrl(URL)
                .build();

        ChatModel chatModel = OllamaChatModel.builder()
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model(MODEL)
                                .build()
                )
                .ollamaApi(build)
                .build();
        return chatModel;
    }

}
