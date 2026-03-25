package org.finos.fluxnova.example.ai.mcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient.Builder chatClientBuilder;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    public ChatController(            ChatClient.Builder chatClientBuilder, SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClientBuilder = chatClientBuilder;
        this.toolCallbackProvider = toolCallbackProvider;
        Arrays.stream(toolCallbackProvider.getToolCallbacks()).forEach(t -> {
            log.info("Tool callback: {}", t.getToolDefinition());
        });
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "what are dan vegas last three videos?") String prompt) {
        try {
            // Refresh the tools. NB NOT SUITABLE FOR PRODUCTION
            toolCallbackProvider.invalidateCache();
            ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
            Arrays.stream(toolCallbackProvider.getToolCallbacks()).forEach(t -> {
                log.info("Tool callback: {}", t.getToolDefinition());
            });
            return chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .content();
        } catch (NonTransientAiException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("model") && ex.getMessage().contains("not found")) {
                log.warn("Ollama model not ready: {}", ex.getMessage());
                return "The Ollama model is still downloading or unavailable. Please try again in a short while.";
            }
            log.error("Non-transient AI error while processing prompt", ex);
            return "Unable to complete the request due to an upstream AI error.";
        } catch (Exception ex) {
            log.error("Unexpected error while processing prompt", ex);
            return "An unexpected error occurred while handling your request.";
        }
    }
}
