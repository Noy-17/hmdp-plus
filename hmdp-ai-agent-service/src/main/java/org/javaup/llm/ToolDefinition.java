package org.javaup.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, Object> parameters;
}
