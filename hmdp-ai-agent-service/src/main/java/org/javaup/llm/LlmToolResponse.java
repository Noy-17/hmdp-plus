package org.javaup.llm;

import lombok.Data;

import java.util.Map;

@Data
public class LlmToolResponse {
    private boolean toolCalled;
    private String toolName;
    private Map<String, Object> arguments;
    private String textResponse;

    public static LlmToolResponse ofToolCall(String toolName, Map<String, Object> arguments) {
        LlmToolResponse r = new LlmToolResponse();
        r.toolCalled = true;
        r.toolName = toolName;
        r.arguments = arguments;
        return r;
    }

    public static LlmToolResponse ofText(String text) {
        LlmToolResponse r = new LlmToolResponse();
        r.toolCalled = false;
        r.textResponse = text;
        return r;
    }
}
