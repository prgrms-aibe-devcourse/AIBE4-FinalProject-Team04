package kr.java.patchnotedemo.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.base-url}")
    private String geminiBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:gemini-1.5-flash}")
    private String chatModelName;

    @Value("${spring.ai.ollama.embedding.options.model:mxbai-embed-large}")
    private String embeddingModelName;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    // ChatModel: Google Gemini (OpenAI 호환 모드)
    @Bean
    @Qualifier("geminiFlashModel")
    public ChatModel geminiFlashModel() {
        OpenAiApi geminiApi =
                OpenAiApi.builder()
                        .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
                        .apiKey(geminiApiKey)
                        .build();

        OpenAiChatOptions options =
                OpenAiChatOptions.builder().model(chatModelName).temperature(0.0).build();

        return OpenAiChatModel.builder().openAiApi(geminiApi).defaultOptions(options).build();
    }

    @Bean
    public ChatClient chatClient(@Qualifier("geminiFlashModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(ollamaBaseUrl).build();

        var ollamaOptions = OllamaEmbeddingOptions.builder().model(embeddingModelName).build();

        var managementOptions = ModelManagementOptions.defaults();

        return new OllamaEmbeddingModel(
                ollamaApi, ollamaOptions, ObservationRegistry.NOOP, managementOptions // 모델 관리 옵션
                );
    }

    // EmbeddingModel: Real OpenAI (text-embedding-3-small)
    //    @Bean
    //    public EmbeddingModel embeddingModel() {
    //        // [OpenAI 설정] URL 없이 키만 넣으면 기본값(api.openai.com) 사용
    //        OpenAiApi realOpenAiApi = OpenAiApi.builder().apiKey(openAiApiKey).build();
    //
    //        OpenAiEmbeddingOptions options =
    //                OpenAiEmbeddingOptions.builder().model("text-embedding-3-small").build();
    //
    //        return new OpenAiEmbeddingModel(
    //                realOpenAiApi, MetadataMode.EMBED, options, RetryTemplate.builder().build());
    //    }
}
