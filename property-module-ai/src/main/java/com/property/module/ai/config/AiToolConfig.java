package com.property.module.ai.config;

import com.property.module.ai.tool.CommunityInfoTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiToolConfig {
    @Bean
    public ToolCallbackProvider communityInfoToolCallback(CommunityInfoTool communityInfoTool)
    {
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(communityInfoTool)
                .build();
    }
}
