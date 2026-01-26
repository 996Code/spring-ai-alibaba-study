package com.alibaba.cloud.ai.study;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import static com.alibaba.cloud.ai.common.CommonUtils.getChatModel;
import static com.alibaba.cloud.ai.common.CommonUtils.redisCli;

public class MemoryExampleOllama {


    // ==================== 基础使用 ====================

    /**
     * 示例1：基础记忆配置
     */
    @Test
    public void basicMemoryConfiguration() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建示例工具
        ToolCallback getUserInfoTool = createGetUserInfoTool();

        // 配置 checkpointer
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(getUserInfoTool)
                .saver(new MemorySaver())
                .build();

        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId("1") // threadId 指定会话 ID
                .build();

        AssistantMessage call = agent.call("你好！我叫 Bob。", config);
        System.out.println(call.getText());
    }


    /**
     * 创建示例工具
     */
    private ToolCallback createGetUserInfoTool() {
        return FunctionToolCallback.builder("get_user_info", (String query) -> {
                    return "User info: " + query;
                })
                .description("Get user information")
                .inputType(String.class)
                .build();
    }


    /**
     * 示例2：生产环境使用 Redis Checkpointer
     */
    @Test
	public void productionMemoryConfiguration() throws GraphRunnerException {

        ChatModel chatModel = getChatModel();

        RedissonClient redissonClient = redisCli();

        ToolCallback getUserInfoTool = createGetUserInfoTool();

		// 配置 Redis checkpointer
		RedisSaver redisSaver = RedisSaver.builder().redisson(redissonClient).build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
                .saver(redisSaver)
				.build();


        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId("user_123") // threadId 指定会话 ID
                .build();

        AssistantMessage call = agent.call("你好！我叫 张三。", config);
        System.out.println(call.getText());

        AssistantMessage call1= agent.call("我叫什么名字", config);
        System.out.println(call1.getText());

	}


}
