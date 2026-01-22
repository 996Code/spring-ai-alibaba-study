package com.alibaba.cloud.ai.study;


import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.examples.documentation.framework.tutorials.ToolsExample;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.alibaba.cloud.ai.common.CommonUtils.getChatModel;


/**
 * https://java2ai.com/docs/frameworks/agent-framework/tutorials/tools
 */
@Log4j2
public class ToolsExampleOllama {

    /**
     * @Tool
     * name：tool 的名称。如果未提供，将使用方法名称。AI model 使用此名称在调用时识别 tool。因此，不允许在同一类中有两个同名 tools。名称必须在特定聊天请求中提供给 model 的所有 tools 中唯一。
     *  description：tool 的描述，model 可以使用它来理解何时以及如何调用 tool。如果未提供，方法名称将用作 tool 描述。
     * returnDirect：tool 结果是否应直接返回给客户端或传递回 model
     * resultConverter：用于将 tool call 的结果转换为 String object 以发送回 AI model 的ToolCallResultConverter 实现。
     *
     * @ToolParam
     * description：参数的描述，model 可以使用它来更好地理解如何使用它。
     * required：参数是必需还是可选的
     */

    /**
     * 编程方式构建 MethodToolCallback
     * toolDefinition：定义 tool 名称、描述和输入 schema 的 ToolDefinition 实例
     * toolMetadata：定义附加设置的 ToolMetadata 实例，例如结果是否应直接返回给客户端，以及要使用的结果转换器
     * toolMethod：表示 tool 方法的 Method 实例
     * toolObject：包含 tool 方法的对象实例
     * toolCallResultConverter：用于将 tool call 的结果转换为 String 对象以发送回 AI model 的 ToolCallResultConverter 实例
     */

    // ==================== 基础工具定义 ====================

    /**
     * 示例1：编程方式规范 - FunctionToolCallback
     */
    @Test
    public void programmaticToolSpecification() {
        ToolCallback toolCallback = FunctionToolCallback
                // tool 的名称
                .builder("currentWeather", new WeatherService())
                // tool 的描述
                .description("Get the weather in location")
                // 函数输入的类型
                .inputType(WeatherRequest.class)
                .build();
    }



    /**
     * 示例2：添加工具到 ChatClient（使用编程规范）
     */
    @Test
    public void addToolToChatClient() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        ToolCallback toolCallback = FunctionToolCallback
                // tool 的名称
                .builder("currentWeather", new WeatherService())
                .description("Get the weather in location")
                .inputType(WeatherRequest.class)
                .build();


        // Note: ChatClient usage would be shown here in actual implementation
        // This is a simplified example

        // 使用工具
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(toolCallback)
                .build();

        AssistantMessage call = agent.call("What is the weather like in New York City?");
        System.out.println(call.getText());
    }

    /**
     * 示例3：自定义工具名称
     */
    @Test
    public void customToolName() {
        ToolCallback searchTool = FunctionToolCallback
                .builder("web_search", new SearchFunction())  // 自定义名称
                .description("Search the web for information")
                .inputType(JSONObject.class)
                .build();

        System.out.println(searchTool.getToolDefinition().name());  // web_search
    }


    /**
     * 示例4：自定义工具描述
     */
    @Test
    public void customToolDescription() {
        ToolCallback calculatorTool = FunctionToolCallback
                .builder("calculator", new CalculatorFunction())
                .description("Performs arithmetic calculations. Use this for any math problems.")
                .inputType(JSONObject.class)
                .build();
    }

    /**
     * 示例5：高级模式定义
     */
    @SneakyThrows
    @Test
    public void advancedSchemaDefinition() {

        ChatModel chatModel = getChatModel();

        ToolCallback weatherTool = FunctionToolCallback
                .builder("get_weather", new WeatherFunction())
                .description("Get current weather and optional forecast")
                .inputType(WeatherInput.class)
                .build();

        // 使用工具
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(weatherTool)
                .build();

        AssistantMessage call = agent.call("What is the weather like in New York City?");
        System.out.println(call.getText());

    }


    /**
     * 示例6：访问状态
     */
    @SneakyThrows
    @Test
    public void accessingState() {

        ChatModel chatModel = getChatModel();

        // 创建工具
        ToolCallback summaryTool = FunctionToolCallback
                .builder("summarize_conversation", new ConversationSummaryTool())
                .description("Summarize the conversation so far")
                .inputType(JSONObject.class)
                .build();

        // 使用工具
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .saver(new MemorySaver())
                .tools(summaryTool)
                .build();

        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId("user_123")
                .build();

        AssistantMessage response = agent.call("我叫张三", config);
        log.info(response.getText());
        // 输出: "你叫张三"
        response = agent.call("我叫什么名字？", config);
        log.info(response.getText());

        AssistantMessage call = agent.call("总结一下我刚说了什么？", config);
        log.info(call.getText());

    }


    // ==================== 自定义工具属性 ====================

    /**
     * 示例7：访问上下文
     */
    @Test
    public void accessingContext() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        ToolCallback accountTool = FunctionToolCallback
                .builder("get_account_info", new AccountInfoTool())
                .description("Get the current user's account information")
                .inputType(JSONObject.class)
                .build();

        // 在 ReactAgent 中使用
        ReactAgent agent = ReactAgent.builder()
                .name("financial_assistant")
                .model(chatModel)
                .tools(accountTool)
                .systemPrompt("You are a financial assistant.")
                .build();

        // 调用时传递上下文
        RunnableConfig config = RunnableConfig.builder()
                .addMetadata("user_id", "user123")
                .build();

        AssistantMessage call = agent.call("获取我的财务账户信息", config);
        System.out.println(call.getText());
    }



    /**
     * 示例8：使用存储访问跨对话的持久数据
     */
    @Test
    public void accessingMemoryStore() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 配置持久化存储
        MemorySaver memorySaver = new MemorySaver();

        // 创建工具
        ToolCallback saveUserInfoTool = createSaveUserInfoTool();
        ToolCallback getUserInfoTool = createGetUserInfoTool();

        // 创建带有持久化记忆的 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(saveUserInfoTool, getUserInfoTool)
                .saver(memorySaver)
                .build();

        // 第一个会话：保存用户信息
        RunnableConfig config1 = RunnableConfig.builder()
                .threadId("session_1")
                .build();

        AssistantMessage call1 = agent.call("Save user: userid: abc123, name: Foo, age: 25, email: foo@example.com", config1);
        System.out.println(call1.getText());

        // 第二个会话：获取用户信息，注意这里用的是不同的 threadId
        RunnableConfig config2 = RunnableConfig.builder()
                .threadId("session_2")
                .build();

        AssistantMessage call = agent.call("Get user info for user with id 'abc123'", config2);
        System.out.println(call.getText());
    }

    /**
     * 示例9：在 ReactAgent 中使用工具
     */
    @Test
    public void toolsInReactAgent() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建工具
        ToolCallback weatherTool = FunctionToolCallback
                .builder("get_weather", new WeatherFunction())
                .description("Get weather for a given city")
                .inputType(WeatherInput.class)
                .build();

        ToolCallback searchTool = FunctionToolCallback
                .builder("search", new SearchFunction())
                .description("Search for information")
                .inputType(JSONObject.class)
                .build();

        // 创建带有工具的 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(weatherTool, searchTool)
                .systemPrompt("You are a helpful assistant with access to weather and search tools.")
                .saver(new MemorySaver())
                .build();

        // 使用 Agent
        AssistantMessage response = agent.call("What's the weather like in San Francisco?");
        System.out.println(response.getText());
    }

    /**
     * 示例10：完整的工具使用示例（使用 tools 方法）
     */
    @Test
    public void comprehensiveToolExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 定义多个工具
        ToolCallback weatherTool = FunctionToolCallback
                .builder("get_weather", new WeatherFunction())
                .description("Get current weather and optional forecast for a city")
                .inputType(WeatherInput.class)
                .build();

        ToolCallback calculatorTool = FunctionToolCallback
                .builder("calculator", new CalculatorFunction())
                .description("Perform arithmetic calculations")
                .inputType(JSONObject.class)
                .build();

        ToolCallback searchTool = FunctionToolCallback
                .builder("web_search", new SearchFunction())
                .description("Search the web for information")
                .inputType(JSONObject.class)
                .build();

        // 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("multi_tool_agent")
                .model(chatModel)
                .tools(weatherTool, calculatorTool, searchTool)
                .systemPrompt("""
						You are a helpful AI assistant with access to multiple tools:
						- Weather information
						- Calculator for math operations
						- Web search for general information
						
						Use the appropriate tool based on the user's question.
						""")
                .saver(new MemorySaver())
                .build();

        // 使用不同的工具
        RunnableConfig config = RunnableConfig.builder()
                .threadId("session_1")
                .build();

        AssistantMessage call = agent.call("What's the weather in New York?", config);
        System.out.println(call.getText());
        AssistantMessage call1 = agent.call("Calculate 25 * 4 + 10", config);
        System.out.println(call1.getText());
        AssistantMessage call2 = agent.call("Search for latest AI news", config);
        System.out.println(call2.getText());
    }

    /**
     * 示例11：使用 methodTools - 基于 @Tool 注解的方法工具
     */
    @Test
    public void methodToolsExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建带有 @Tool 注解方法的工具对象
        CalculatorTools calculatorTools = new CalculatorTools();

        // 使用 methodTools 方法，传入带有 @Tool 注解方法的对象
        ReactAgent agent = ReactAgent.builder()
                .name("calculator_agent")
                .model(chatModel)
                .description("An agent that can perform calculations")
                .instruction("You are a helpful calculator assistant. Use the available tools to perform calculations.")
                .methodTools(calculatorTools)  // 传入带有 @Tool 注解方法的对象
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("method_tools_session")
                .build();

        AssistantMessage call = agent.call("What is 15 + 27?", config);
        System.out.println(call.getText());
        AssistantMessage call1 = agent.call("What is 8 * 9?", config);
        System.out.println(call1.getText());
    }

    /**
     * 示例12：使用多个 methodTools 对象
     */
    @Test
    public void multipleMethodToolsExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建多个工具对象
        CalculatorTools calculatorTools = new CalculatorTools();
        WeatherTools weatherTools = new WeatherTools();

        // 可以传入多个 methodTools 对象
        ReactAgent agent = ReactAgent.builder()
                .name("multi_method_tool_agent")
                .model(chatModel)
                .description("An agent with multiple method-based tools")
                .instruction("You are a helpful assistant with calculator and weather tools.")
                .methodTools(calculatorTools, weatherTools)  // 传入多个工具对象
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("multi_method_tools_session")
                .build();

        AssistantMessage call = agent.call("What is 10 * 8 and what's the weather in Beijing?", config);
        System.out.println(call.getText());
    }

    /**
     * 示例13：使用 ToolCallbackProvider
     *
     * 使用 ToolCallbackProvider 接口动态提供工具。这种方式适合需要根据运行时条件动态决定提供哪些工具的场景。
     *
     */
    @Test
    public void toolCallbackProviderExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建工具
        ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchToolWithContext())
                .description("Search for information")
                .inputType(CalculatorFunctionWithContextRequest.class)
                .build();

        // 创建 ToolCallbackProvider
        ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool));

        // 使用 toolCallbackProviders 方法
        ReactAgent agent = ReactAgent.builder()
                .name("search_agent")
                .model(chatModel)
                .description("An agent that can search for information")
                .instruction("You are a helpful assistant with search capabilities.")
                .toolCallbackProviders(toolProvider)  // 使用 ToolCallbackProvider
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("tool_provider_session")
                .build();

        AssistantMessage call = agent.call("Search for information about Spring AI", config);
        System.out.println(call.getText());
    }

    /**
     * 示例14：使用 toolNames 和 resolver（必须配合使用）
     * 使用 toolNames() 方法指定工具名称，配合 resolver() 方法提供的 ToolCallbackResolver 来解析工具。
     * 这种方式适合工具定义和工具使用分离的场景。
     */
    @Test
    public void toolNamesWithResolverExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建工具（使用复合类型）
        ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchFunctionWithRequest())
                .description("Search for information")
                .inputType(SearchRequest.class)
                .build();

        ToolCallback calculatorTool = FunctionToolCallback.builder("calculator", new CalculatorFunctionWithRequest())
                .description("Perform arithmetic calculations")
                .inputType(CalculatorRequest.class)
                .build();

        // 创建 StaticToolCallbackResolver，包含所有工具
        StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
                List.of(calculatorTool, searchTool));

        // 使用 toolNames 指定要使用的工具名称，必须配合 resolver 使用
        ReactAgent agent = ReactAgent.builder()
                .name("multi_tool_agent")
                .model(chatModel)
                .description("An agent with multiple tools")
                .instruction("You are a helpful assistant with access to calculator and search tools.")
                .toolNames("calculator", "search")  // 使用工具名称而不是 ToolCallback 实例
                .resolver(resolver)  // 必须提供 resolver 来解析工具名称
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("tool_names_session")
                .build();

        AssistantMessage call = agent.call("Calculate 25 + 4 and then search for information about the result", config);
        System.out.println(call.getText());
    }

    /**
     * 示例15：使用 resolver 直接解析工具
     */
    @Test
    public void resolverExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // 创建工具
        ToolCallback calculatorTool = FunctionToolCallback.builder("calculator", new CalculatorFunctionWithContext())
                .description("Perform arithmetic calculations")
                .inputType(CalculatorFunctionWithContextRequest.class)
                .build();

        // 创建 resolver
        StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
                List.of(calculatorTool));

        // 使用 resolver，可以直接在 tools 中使用，也可以仅通过 resolver 提供
        ReactAgent agent = ReactAgent.builder()
                .name("resolver_agent")
                .model(chatModel)
                .description("An agent using ToolCallbackResolver")
                .instruction("You are a helpful calculator assistant.")
                .tools(calculatorTool)  // 直接指定工具
                .resolver(resolver)  // 同时设置 resolver 供工具节点使用
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("resolver_session")
                .build();

        AssistantMessage call = agent.call("What is 100 divided by 4?", config);
        System.out.println(call.getText());
    }

    /**
     * 示例16：组合使用多种工具提供方式
     */
    @Test
    public void combinedToolProvisionExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // Method tools
        CalculatorTools calculatorTools = new CalculatorTools();

        //  Multiple tools with the same name (search) found in ToolCallingChatOptions
        //  工具多种方式不能重复，所以这边使用了不同的名称

        // Direct tool
        ToolCallback searchTool1 = FunctionToolCallback.builder("search_info1", new SearchToolWithContext())
                .description("Search for information1")
                .inputType(CalculatorFunctionWithContextRequest.class)
                .build();
        // Direct tool
        ToolCallback searchTool2 = FunctionToolCallback.builder("search_info2", new SearchToolWithContext())
                .description("Search for information2")
                .inputType(CalculatorFunctionWithContextRequest.class)
                .build();

        // ToolCallbackProvider
        ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool1));

        // 组合使用多种方式
        ReactAgent agent = ReactAgent.builder()
                .name("combined_tool_agent")
                .model(chatModel)
                .description("An agent with multiple tool provision methods")
                .instruction("You are a helpful assistant with calculator and search capabilities.")
                .methodTools(calculatorTools)  // Method-based tools
                .toolCallbackProviders(toolProvider)  // Provider-based tools
                .tools(searchTool2)  // Direct tools
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("combined_session")
                .build();

        AssistantMessage call = agent.call("Calculate 50 + 75 and search for information about mathematics", config);
        System.out.println(call.getText());
    }

    // ==================== 高级模式定义 ====================

    /**
     * 创建保存用户信息工具
     */
    private ToolCallback createSaveUserInfoTool() {
        return FunctionToolCallback.builder("save_user_info", (JSONObject input) -> {
                    // 简化的实现
                    return "User info saved: " + input;
                })
                .description("Save user information")
                .inputType(JSONObject.class)
                .build();
    }

    /**
     * 创建获取用户信息工具
     */
    private ToolCallback createGetUserInfoTool() {
        return FunctionToolCallback.builder("get_user_info", (JSONObject userId) -> {
                    // 简化的实现
                    return "User info for: " + userId;
                })
                .description("Get user information by ID")
                .inputType(JSONObject.class)
                .build();
    }


    public enum Unit {C, F}

    // ==================== 访问上下文 ====================

    public enum UnitType {CELSIUS, FAHRENHEIT}

    /**
     * 天气服务
     */
    public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
        @Override
        public WeatherResponse apply(WeatherRequest request) {
            return new WeatherResponse(30.0, Unit.C);
        }
    }

    // ==================== Context（上下文） ====================

    public record WeatherRequest(
            @ToolParam(description = "城市或坐标") String location,
            Unit unit
    ) { }

    public record WeatherResponse(double temp, Unit unit) { }

    // ==================== Memory（存储） ====================

    /**
     * 搜索函数
     */
    public class SearchFunction implements Function<JSONObject, String> {
        @Override
        public String apply(JSONObject query) {
            return "Search results for: " + query.getString("query");
        }
    }

    // ==================== 在 ReactAgent 中使用工具 ====================

    /**
     * 计算器函数
     */
    public class CalculatorFunction implements Function<JSONObject, String> {
        @Override
        public String apply(JSONObject expression) {
            // 简化的计算逻辑
            return "Result: " + expression.getString("expression");
        }
    }

    // ==================== 完整示例 ====================

    /**
     * 天气输入（使用记录类）
     */
    public record WeatherInput(
            @ToolParam(description = "City name or coordinates") String location,
            @ToolParam(description = "Temperature unit preference") Unit units,
            @ToolParam(description = "Include 5-day forecast") boolean includeForecast
    ) { }

    // ==================== 辅助方法 ====================

    /**
     * 天气函数（高级版）
     * WeatherInput 输入参数
     * String 输出参数
     */
    public class WeatherFunction implements Function<WeatherInput, String> {
        @Override
        public String apply(WeatherInput input) {
            double temp = input.units() == Unit.F ? 22 : 72;
            String result = String.format(
                    "Current weather in %s: %.0f degrees %s",
                    input.location(),
                    temp,
                    input.units().toString().substring(0, 1).toUpperCase()
            );

            if (input.includeForecast()) {
                result += "\nNext 5 days: Sunny";
            }

            return result;
        }
    }

    /**
     * 对话摘要工具
     */
    public class ConversationSummaryTool implements BiFunction<JSONObject, ToolContext, String> {

        @Override
        public String apply(JSONObject input, ToolContext toolContext) {
            OverAllState state = (OverAllState) toolContext.getContext().get(ToolContextConstants.AGENT_STATE_CONTEXT_KEY);
            RunnableConfig config = (RunnableConfig) toolContext.getContext().get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY);

            // 从state中获取消息
            Optional<Object> messagesOpt = state.value("messages");
            List<Message> messages = messagesOpt.isPresent()
                    ? (List<Message>) messagesOpt.get()
                    : new ArrayList<>();

            if (messages.isEmpty()) {
                return "No conversation history available";
            }

            long userMsgs = messages.stream()
                    .filter(m -> m.getMessageType().getValue().equals("user"))
                    .count();
            long aiMsgs = messages.stream()
                    .filter(m -> m.getMessageType().getValue().equals("assistant"))
                    .count();
            long toolMsgs = messages.stream()
                    .filter(m -> m.getMessageType().getValue().equals("tool"))
                    .count();

            return String.format(
                    "Conversation has %d user messages, %d AI responses, and %d tool results",
                    userMsgs, aiMsgs, toolMsgs
            );
        }
    }

    // ==================== Main 方法 ====================

    /**
     * 账户信息工具
     */
    public class AccountInfoTool implements BiFunction<JSONObject, ToolContext, String> {

        private final Map<String, Map<String, Object>> USER_DATABASE = Map.of(
                "user123", Map.of(
                        "name", "Alice Johnson",
                        "account_type", "Premium",
                        "balance", 5000,
                        "email", "alice@example.com"
                ),
                "user456", Map.of(
                        "name", "Bob Smith",
                        "account_type", "Standard",
                        "balance", 1200,
                        "email", "bob@example.com"
                )
        );

        @Override
        public String apply(JSONObject query, ToolContext toolContext) {
            RunnableConfig config = (RunnableConfig) toolContext.getContext().get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY);
            String userId = (String) config.metadata("user_id").orElse(null);

            if (userId == null) {
                return "User ID not provided";
            }

            Map<String, Object> user = USER_DATABASE.get(userId);
            if (user != null) {
                return String.format(
                        "Account holder: %s\nType: %s\nBalance: $%d",
                        user.get("name"),
                        user.get("account_type"),
                        user.get("balance")
                );
            }

            return "User not found";
        }
    }

    // ==================== MethodTools 相关类 ====================

    /**
     * 计算器工具类 - 使用 @Tool 注解
     */
    public class CalculatorTools {
        public int callCount = 0;

        @Tool(description = "Add two numbers together")
        public String add(
                @ToolParam(description = "First number") int a,
                @ToolParam(description = "Second number") int b) {
            callCount++;
            return String.valueOf(a + b);
        }

        @Tool(description = "Multiply two numbers together")
        public String multiply(
                @ToolParam(description = "First number") int a,
                @ToolParam(description = "Second number") int b) {
            callCount++;
            return String.valueOf(a * b);
        }

        @Tool(description = "Subtract second number from first number")
        public String subtract(
                @ToolParam(description = "First number") int a,
                @ToolParam(description = "Second number") int b) {
            callCount++;
            return String.valueOf(a - b);
        }
    }

    /**
     * 天气工具类 - 使用 @Tool 注解
     */
    public class WeatherTools {
        @Tool(description = "Get current weather for a location")
        public String getWeather(@ToolParam(description = "City name") String city) {
            return "Sunny, 25°C in " + city;
        }

        @Tool(description = "Get weather forecast for a location")
        public String getForecast(
                @ToolParam(description = "City name") String city,
                @ToolParam(description = "Number of days") int days) {
            return String.format("Weather forecast for %s for next %d days: Mostly sunny", city, days);
        }
    }

    // ==================== ToolCallbackProvider 相关类 ====================

    /**
     * 自定义 ToolCallbackProvider 实现
     */
    public class CustomToolCallbackProvider implements ToolCallbackProvider {
        private final List<ToolCallback> toolCallbacks;

        public CustomToolCallbackProvider(List<ToolCallback> toolCallbacks) {
            this.toolCallbacks = toolCallbacks;
        }

        @Override
        public ToolCallback[] getToolCallbacks() {
            return toolCallbacks.toArray(new ToolCallback[0]);
        }
    }

    /**
     * 带上下文的搜索工具
     */
    public class SearchToolWithContext implements BiFunction<CalculatorFunctionWithContextRequest, ToolContext, String> {
        @Override
        public String apply(CalculatorFunctionWithContextRequest query, ToolContext toolContext) {
            return "Search results for: " + query;
        }
    }

    // ==================== Resolver 相关类 ====================

    /**
     * 搜索请求类（用于复合类型）
     */
    public static class SearchRequest {
        @JsonProperty(required = true)
        @JsonPropertyDescription("The search query string")
        public String query;

        public SearchRequest() {
        }

        public SearchRequest(String query) {
            this.query = query;
        }
    }

    /**
     * 使用复合类型的搜索函数
     */
    public class SearchFunctionWithRequest implements BiFunction<SearchRequest, ToolContext, String> {
        @Override
        public String apply(SearchRequest request, ToolContext toolContext) {
            return "Search results for: " + request.query;
        }
    }

    /**
     * 计算器请求类（用于复合类型）
     */
    public static class CalculatorRequest {  // 添加 static 修饰符
        @JsonProperty(required = true)
        @JsonPropertyDescription("First number for the calculation")
        public int a;

        @JsonProperty(required = true)
        @JsonPropertyDescription("Second number for the calculation")
        public int b;

        public CalculatorRequest() {
        }

        public CalculatorRequest(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }


    /**
     * 计算器请求类（用于复合类型）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CalculatorFunctionWithContextRequest {  // 添加 static 修饰符
        @JsonProperty(required = true)
        public String operation;

        @JsonProperty(required = true)
        public Double denominator;
        @JsonProperty(required = true)
        public Double numerator;

    }

    /**
     * 使用复合类型的计算器函数
     */
    public class CalculatorFunctionWithRequest implements BiFunction<CalculatorRequest, ToolContext, String> {
        @Override
        public String apply(CalculatorRequest request, ToolContext toolContext) {
            return String.valueOf(request.a + request.b);
        }
    }

    /**
     * 带上下文的计算器函数
     */
    public class CalculatorFunctionWithContext implements BiFunction<CalculatorFunctionWithContextRequest, ToolContext, String> {
        @Override
        public String apply(CalculatorFunctionWithContextRequest expression, ToolContext toolContext) {
            // 简单的计算解析（用于演示）
            String operation = expression.getOperation();
            Double denominator = expression.getDenominator();
            Double numerator = expression.getNumerator();
            if (operation.equals("divided")) {
                double result = numerator / denominator;
                return String.valueOf(result);
            }
            if (operation.contains("*")) {
                double result = numerator * denominator;
                return String.valueOf(result);
            }
            return "Calculation result for: " + expression;
        }
    }
}
