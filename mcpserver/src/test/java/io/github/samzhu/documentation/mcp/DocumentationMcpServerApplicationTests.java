package io.github.samzhu.documentation.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 應用程式 Context 載入測試
 * <p>
 * 使用 Testcontainers 提供 PostgreSQL，Mock EmbeddingModel 避免需要 Google API Key。
 * 透過假的 api-key 讓 Google GenAI auto-configuration 走 Gemini Developer API 模式，
 * 再由 @MockitoBean 替換真實的 EmbeddingModel。
 * </p>
 *
 * @see <a href="https://github.com/spring-projects/spring-ai">Spring AI 官方測試模式：提供假 property 值</a>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
	// 提供假的 API Key，讓 GoogleGenAiEmbeddingConnectionAutoConfiguration
	// 走 Gemini Developer API 模式，避免進入 Vertex AI 模式要求 project-id
	"spring.ai.google.genai.embedding.api-key=test-key"
})
class DocumentationMcpServerApplicationTests {

	// 模擬 EmbeddingModel，避免測試時發起真實的嵌入 API 呼叫
	@MockitoBean
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
		// Spring Context 成功載入即可
	}

}
