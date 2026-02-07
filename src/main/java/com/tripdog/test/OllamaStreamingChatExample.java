package com.tripdog.test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import java.util.concurrent.CompletableFuture;

public class OllamaStreamingChatExample {
    static String MODEL_NAME = "qwen3:8b"; // try other local ollama model names
    static String BASE_URL = "http://localhost:11434"; // local ollama base url


    public static void main(String[] args) {
        StreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .build();
        String userMessage = "用 go 写一个归并排序";

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();
        model.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                futureResponse.completeExceptionally(error);
            }
        });

        futureResponse.join();
    }
}