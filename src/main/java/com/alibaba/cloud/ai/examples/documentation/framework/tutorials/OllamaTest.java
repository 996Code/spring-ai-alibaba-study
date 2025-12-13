package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.util.List;

public class OllamaTest {


    public static void basicModelConfiguration() throws GraphRunnerException {
        OllamaApi build = OllamaApi.builder()
                .baseUrl("http://996code.top:11434")
                .build();

        ChatModel chatModel = OllamaChatModel.builder()
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3")
                                .build()
                )
                .ollamaApi(build)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .build();

        // 字符串输入
        AssistantMessage response = agent.call("杭州的天气怎么样？");
        System.out.println(response.getText());

        // UserMessage 输入
        UserMessage userMessage = new UserMessage("帮我分析这个问题");
        AssistantMessage response2 = agent.call(userMessage);
        System.out.println(response2.getText());

        // 多个消息
        List<Message> messages = List.of(
                new UserMessage("我想了解 Java 多线程"),
                new UserMessage("特别是线程池的使用")
        );
        AssistantMessage response3 = agent.call(messages);
        System.out.println(response3.getText());
    }


    public static void main(String[] args) {
        try {
            basicModelConfiguration();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

}
