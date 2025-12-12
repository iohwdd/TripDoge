package com.tripdog.ai;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Configuration;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;

/**
 * @author: iohw
 * @date: 2025/9/26 19:05
 * @description:
 */
@Configuration
@RequiredArgsConstructor
public class CustomerChatMemoryProvider implements ChatMemoryProvider {
    private final ChatMemoryStore memoryStore;
    private final Integer MAX_MESSAGES = 50;
    private final Map<String, ChatMemory> map = new HashMap<>();

    @Override
    public ChatMemory get(Object id) {
        String key = id.toString();
        if (!map.containsKey(key)) {
            map.put(key, createMemory(key));
        }
        return map.get(id.toString());
    }

    public Map<String, ChatMemory> getChatMemoryMap() {
        return map;
    }

    public ChatMemory createMemory(Object id) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(memoryStore)
                .build();
        map.put(id.toString(), memory);
        return memory;
    }
}
