package com.alibaba.cloud.ai.study;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.common.CommonUtils.getChatModel;

/**
 * Hooks & Interceptors Tutorial - hooks.md
 *
 * https://java2ai.com/docs/frameworks/agent-framework/tutorials/hooks/
 *
 */
@Log4j2
public class HooksExampleOllama {


    // ==================== 基础 Hook 和 Interceptor 配置 ====================

    /**
     * 示例1：添加 Hooks 和 Interceptors
     */

    @SneakyThrows
    @Test
    public void basicHooksAndInterceptors() {
        ChatModel chatModel = getChatModel();

        // 创建工具（示例）
        ToolCallback[] tools = new ToolCallback[0];

        // 创建 Hooks 和 Interceptors
        ModelHook loggingHook = new LoggingModelHook();
        MessagesModelHook messageTrimmingHook = new MessageTrimmingHook();
        ModelInterceptor guardrailInterceptor = new GuardrailInterceptor();
        ToolInterceptor retryInterceptor = new RetryToolInterceptor();

        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(tools)
                .hooks(loggingHook, messageTrimmingHook)
                .interceptors(guardrailInterceptor)
                .interceptors(retryInterceptor)
                .build();
    }


    // ==================== 消息压缩（Summarization） ====================

    /**
     * 示例2：消息压缩 Hook
     */
    @SneakyThrows
    @Test
    public void messageSummarization() {
        ChatModel chatModel = getChatModel();
        // 创建消息压缩 Hook
        SummarizationHook summarizationHook = SummarizationHook.builder()
                // model: 用于生成摘要的 ChatModel
                .model(chatModel)
                // maxTokensBeforeSummary: 触发摘要之前的最大 token 数
                .maxTokensBeforeSummary(4000)
                // messagesToKeep: 摘要后保留的最新消息数
                .messagesToKeep(20)
                .build();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .hooks(summarizationHook)
                .build();

    }


    // ==================== Human-in-the-Loop ====================

    /**
     * 示例3：Human-in-the-Loop Hook
     */
    @SneakyThrows
    @Test
    public void humanInTheLoop() {
        ChatModel chatModel = getChatModel();

        // 创建工具（示例）
        ToolCallback sendEmailTool = createSendEmailTool();
        ToolCallback deleteDataTool = createDeleteDataTool();

        // 创建 Human-in-the-Loop Hook
        HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
                .approvalOn("sendEmailTool", ToolConfig.builder()
                        .description("Please confirm sending the email.")
                        .build())
                .approvalOn("deleteDataTool", ToolConfig.builder()
                        .description("Please confirm deleting the data.")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("supervised_agent")
                .model(chatModel)
                .tools(sendEmailTool, deleteDataTool)
                .hooks(humanReviewHook)
                .saver(new MemorySaver())
//                .saver(new RedisSaver())
                .build();
    }


    // ==================== 模型调用限制 ====================

    /**
     * 示例4：模型调用限制
     */
    @SneakyThrows
    @Test
    public void modelCallLimit() {
        ChatModel chatModel = getChatModel();

        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())  // 限制模型调用次数为5次
                .saver(new MemorySaver())
                .build();
    }



    // ==================== PII 检测 ====================

    /**
     * 示例6：PII 检测
     */
    @SneakyThrows
    @Test
    public void piiDetection() {
        ChatModel chatModel = getChatModel();

        PIIDetectionHook pii = PIIDetectionHook.builder()
                .piiType(PIIType.EMAIL)
                .strategy(RedactionStrategy.REDACT)
                .applyToInput(true)
                .build();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("secure_agent")
                .model(chatModel)
                .hooks(pii)
                .build();
    }


    // ==================== 工具重试 ====================

    /**
     * 示例7：工具重试
     */
    @SneakyThrows
    @Test
    public void toolRetry() {
        ChatModel chatModel = getChatModel();

        // 创建工具（示例）
        ToolCallback searchTool = createSearchTool();
        ToolCallback databaseTool = createDatabaseTool();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("resilient_agent")
                .model(chatModel)
                .tools(searchTool, databaseTool)
                .interceptors(ToolRetryInterceptor.builder().maxRetries(2)
                        .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE).build())
                .build();
    }



    // ==================== Planning ====================

    /**
     * 示例8：Planning Hook
     */
    @SneakyThrows
    @Test
    public void planning() {
        ChatModel chatModel = getChatModel();

        ToolCallback myTool = createSampleTool();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("planning_agent")
                .model(chatModel)
                .tools(myTool)
                .interceptors(TodoListInterceptor.builder().build())
                .build();

        AssistantMessage response = agent.call("帮我写一首适合秋高气爽的诗词");
        // 输出会遵循 TextAnalysisResult 的结构
        log.info(response.getText());
    }



    // ==================== LLM Tool Selector ====================

    /**
     * 示例9：LLM 工具选择器
     */
    @SneakyThrows
    @Test
    public void llmToolSelector() {
        ChatModel chatModel = getChatModel();

        ChatModel selectorModel = chatModel; // 用于选择的另一个ChatModel

        ToolCallback tool1 = createSampleTool();
        ToolCallback tool2 = createSampleTool();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("smart_selector_agent")
                .model(chatModel)
                .tools(tool1, tool2)
                .interceptors(ToolSelectionInterceptor.builder().selectionModel(selectorModel).build())
                .build();
    }


    // ==================== LLM Tool Emulator ====================

    /**
     * 示例10：LLM 工具模拟器
     */
    @SneakyThrows
    @Test
    public void llmToolEmulator() {
        ChatModel chatModel = getChatModel();

        ToolCallback simulatedTool = createSampleTool();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("emulator_agent")
                .model(chatModel)
                .tools(simulatedTool)
                .interceptors(ToolEmulatorInterceptor.builder().model(chatModel).build())
                .build();
    }


    // ==================== Context Editing ====================

    /**
     * 示例11：上下文编辑
     */
    public static void contextEditing() {
        ChatModel chatModel = getChatModel();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("context_aware_agent")
                .model(chatModel)
                .interceptors(ContextEditingInterceptor.builder().trigger(120000).clearAtLeast(60000).build())
                .build();
    }





    // ==================== 自定义 Hooks ====================

    // 创建示例工具的辅助方法
    private static ToolCallback createSendEmailTool() {
        return FunctionToolCallback.builder("sendEmailTool", (String input) -> "Email sent")
                .description("Send an email")
                .inputType(String.class)
                .build();
    }

    private static ToolCallback createDeleteDataTool() {
        return FunctionToolCallback.builder("deleteDataTool", (String input) -> "Data deleted")
                .description("Delete data")
                .inputType(String.class)
                .build();
    }

    // ==================== 自定义 Interceptors ====================

    private static ToolCallback createSearchTool() {
        return FunctionToolCallback.builder("searchTool", (String input) -> "Search results")
                .description("Search the web")
                .inputType(String.class)
                .build();
    }

    private static ToolCallback createDatabaseTool() {
        return FunctionToolCallback.builder("databaseTool", (String input) -> "Database query results")
                .description("Query database")
                .inputType(String.class)
                .build();
    }

    // ==================== 辅助类和方法 ====================

    private static ToolCallback createSampleTool() {
        return FunctionToolCallback.builder("sampleTool", (String input) -> "Sample result")
                .description("A sample tool")
                .inputType(String.class)
                .build();
    }


    /**
     * 示例12：自定义 ModelHook
     */
    @HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
    public static class CustomModelHook extends ModelHook {

        @Override
        public String getName() {
            return "custom_model_hook";
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
            // 在模型调用前执行
            System.out.println("准备调用模型...");

            // 可以修改状态
            // 例如：添加额外的上下文
            return CompletableFuture.completedFuture(Map.of("extra_context", "某些额外信息"));
        }

        @Override
        public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
            // 在模型调用后执行
            System.out.println("模型调用完成");

            // 可以记录响应信息
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    /**
     * 示例13：自定义 AgentHook
     */
    @HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
    public static class CustomAgentHook extends AgentHook {

        @Override
        public String getName() {
            return "custom_agent_hook";
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
            System.out.println("Agent 开始执行");
            // 可以初始化资源、记录开始时间等
            return CompletableFuture.completedFuture(Map.of("start_time", System.currentTimeMillis()));
        }

        @Override
        public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
            System.out.println("Agent 执行完成");
            // 可以清理资源、计算执行时间等
            Optional<Object> startTime = state.value("start_time");
            if (startTime.isPresent()) {
                long duration = System.currentTimeMillis() - (Long) startTime.get();
                System.out.println("执行耗时: " + duration + "ms");
            }
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    /**
     * 示例14：自定义 ModelInterceptor
     */
    public static class LoggingInterceptor extends ModelInterceptor {

        @Override
        public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
            // 请求前记录
            System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

            long startTime = System.currentTimeMillis();

            // 执行实际调用
            ModelResponse response = handler.call(request);

            // 响应后记录
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("模型响应耗时: " + duration + "ms");

            return response;
        }

        @Override
        public String getName() {
            return "LoggingInterceptor";
        }
    }

    /**
     * 示例15：自定义 ToolInterceptor
     */
    public static class ToolMonitoringInterceptor extends ToolInterceptor {

        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            String toolName = request.getToolName();
            long startTime = System.currentTimeMillis();

            System.out.println("执行工具: " + toolName);

            try {
                ToolCallResponse response = handler.call(request);

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("工具 " + toolName + " 执行成功 (耗时: " + duration + "ms)");

                return response;
            }
            catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

                return ToolCallResponse.of(
                        request.getToolCallId(),
                        request.getToolName(),
                        "工具执行失败: " + e.getMessage()
                );
            }
        }

        @Override
        public String getName() {
            return "ToolMonitoringInterceptor";
        }
    }


    /**
     * 日志记录 ModelHook
     */
    @HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
    private static class LoggingModelHook extends ModelHook {
        @Override
        public String getName() {
            return "logging_model_hook";
        }

        @Override
        public HookPosition[] getHookPositions() {
            return new HookPosition[] {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
            log.info("Before model call");
            return CompletableFuture.completedFuture(Map.of());
        }

        @Override
        public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
            log.info("After model call");
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    /**
     * 消息修剪 Hook
     * 使用 MessagesModelHook 实现，在模型调用前修剪消息列表，只保留最后 10 条消息
     */
    @HookPositions({HookPosition.BEFORE_MODEL})
    private static class MessageTrimmingHook extends MessagesModelHook {
        private static final int MAX_MESSAGES = 10;

        @Override
        public String getName() {
            return "message_trimming";
        }

        @Override
        public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
            // 如果消息数量超过限制，只保留最后 MAX_MESSAGES 条消息
            if (previousMessages.size() > MAX_MESSAGES) {
                List<Message> trimmedMessages = previousMessages.subList(
                        previousMessages.size() - MAX_MESSAGES,
                        previousMessages.size()
                );
                // 使用 REPLACE 策略替换所有消息
                return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
            }
            // 如果消息数量未超过限制，返回原始消息（不进行修改）
            return new AgentCommand(previousMessages);
        }
    }

    /**
     * 护栏拦截器
     */
    private static class GuardrailInterceptor extends ModelInterceptor {
        @Override
        public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
            // 简化的实现
            return handler.call(request);
        }

        @Override
        public String getName() {
            return "GuardrailInterceptor";
        }
    }

    // ==================== Main 方法 ====================

    /**
     * 重试工具拦截器
     */
    private static class RetryToolInterceptor extends ToolInterceptor {
        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            // 简化的实现
            return handler.call(request);
        }

        @Override
        public String getName() {
            return "RetryToolInterceptor";
        }
    }
}
