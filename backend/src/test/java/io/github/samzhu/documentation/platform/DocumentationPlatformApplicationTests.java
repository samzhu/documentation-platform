package io.github.samzhu.documentation.platform;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 應用程式 Context 載入測試
 * <p>
 * 排除 Spring AI 自動配置，使用 MockitoBean 模擬 EmbeddingModel，避免測試時要求 API Key。
 * Spring Boot 4.0 已將 @MockBean 改為 @MockitoBean。
 * </p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class DocumentationPlatformApplicationTests {

	// 模擬 EmbeddingModel，避免需要真實 API Key
	@MockitoBean
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
		// Spring Context 成功載入即可
	}

}
