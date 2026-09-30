package com.example.weather;

import com.example.weather.config.McpConfig;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpResourceTest {

    @Autowired
    private McpServerFeatures.SyncResourceRegistration serviceInfoResourceRegistration;

    @Test
    void testServiceInfoResource() {
        assertNotNull(serviceInfoResourceRegistration);
        McpSchema.Resource resource = serviceInfoResourceRegistration.resource();
        assertEquals("weather://service-info", resource.uri());
        assertEquals("Weather Service Info", resource.name());

        McpSchema.ReadResourceResult result = serviceInfoResourceRegistration.readHandler().apply(null);
        assertNotNull(result);
        assertFalse(result.contents().isEmpty());

        McpSchema.TextResourceContents textContent = (McpSchema.TextResourceContents) result.contents().get(0);
        assertTrue(textContent.text().contains("Weather MCP Server v1.0.0"));
    }
}
