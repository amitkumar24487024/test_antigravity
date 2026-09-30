package com.example.weather.config;

import com.example.weather.tools.WeatherTools;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider weatherToolCallbackProvider(WeatherTools weatherTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)
                .build();
    }

    @Bean
    public McpServerFeatures.SyncResourceRegistration serviceInfoResourceRegistration() {
        var resource = new McpSchema.Resource(
                "weather://service-info",
                "Weather Service Info",
                "Metadata resource for this MCP weather service",
                "text/plain",
                null
        );
        return new McpServerFeatures.SyncResourceRegistration(resource, request -> {
            String info = """
                    Weather MCP Server v1.0.0
                    Powered by Open-Meteo API (https://open-meteo.com/)
                    Features: Real-time weather, 14-day forecasts, air quality indices, and location geocoding.
                    No API key required.""";
            var contents = new McpSchema.TextResourceContents("weather://service-info", "text/plain", info);
            return new McpSchema.ReadResourceResult(List.of(contents));
        });
    }
}

