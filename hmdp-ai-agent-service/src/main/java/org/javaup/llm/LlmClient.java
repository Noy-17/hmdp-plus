package org.javaup.llm;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.javaup.config.LlmProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final LlmProperties props;

    public LlmClient(LlmProperties props) {
        this.props = props;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(props.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(props.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public String chat(String systemPrompt, String userMessage) {
        JSONObject body = buildRequestBody(systemPrompt, userMessage);
        String raw = doRequest(body, 0);
        return extractContent(raw);
    }

    public LlmToolResponse chatWithTools(String systemPrompt, String userMessage, List<ToolDefinition> tools) {
        JSONObject body = buildRequestBody(systemPrompt, userMessage);
        body.set("tools", buildToolsArray(tools));
        body.set("tool_choice", "auto");

        String raw = doRequest(body, 0);
        return parseToolResponse(raw);
    }

    private JSONObject buildRequestBody(String systemPrompt, String userMessage) {
        JSONArray messages = new JSONArray();
        messages.add(buildMessage("system", systemPrompt));
        messages.add(buildMessage("user", userMessage));

        JSONObject body = new JSONObject();
        body.set("model", props.getModel());
        body.set("messages", messages);
        body.set("max_tokens", props.getMaxTokens());
        body.set("temperature", props.getTemperature());
        return body;
    }

    private JSONObject buildMessage(String role, String content) {
        JSONObject msg = new JSONObject();
        msg.set("role", role);
        msg.set("content", content);
        return msg;
    }

    private JSONArray buildToolsArray(List<ToolDefinition> tools) {
        JSONArray arr = new JSONArray();
        for (ToolDefinition tool : tools) {
            JSONObject wrapper = new JSONObject();
            wrapper.set("type", "function");

            JSONObject func = new JSONObject();
            func.set("name", tool.getName());
            func.set("description", tool.getDescription());
            func.set("parameters", tool.getParameters());
            wrapper.set("function", func);

            arr.add(wrapper);
        }
        return arr;
    }

    private String doRequest(JSONObject body, int retryCount) {
        Request request = new Request.Builder()
                .url(props.getBaseUrl() + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                if ((code == 429 || code >= 500) && retryCount < 1) {
                    log.warn("LLM API returned {}, retrying...", code);
                    return doRequest(body, retryCount + 1);
                }
                String errBody = response.body() != null ? response.body().string() : "";
                throw new LlmException("LLM API error: " + code + " " + errBody);
            }
            return response.body() != null ? response.body().string() : "{}";
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM API call failed: " + e.getMessage(), e);
        }
    }

    private String extractContent(String raw) {
        JSONObject json = JSONUtil.parseObj(raw);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            return "";
        }
        return message.getStr("content", "");
    }

    private LlmToolResponse parseToolResponse(String raw) {
        JSONObject json = JSONUtil.parseObj(raw);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return LlmToolResponse.ofText("抱歉，AI 服务暂时不可用，请稍后重试。");
        }

        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            return LlmToolResponse.ofText("抱歉，AI 服务暂时不可用，请稍后重试。");
        }

        JSONArray toolCalls = message.getJSONArray("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            JSONObject toolCall = toolCalls.getJSONObject(0);
            String funcName = toolCall.getJSONObject("function").getStr("name");
            String argsStr = toolCall.getJSONObject("function").getStr("arguments");
            Map<String, Object> arguments = JSONUtil.parseObj(argsStr).toBean(HashMap.class);
            return LlmToolResponse.ofToolCall(funcName, arguments);
        }

        String content = message.getStr("content");
        return LlmToolResponse.ofText(content != null ? content : "抱歉，我无法处理这个请求。");
    }
}
