package io.github.samzhu.documentation.mcp;

import org.springframework.boot.SpringApplication;

/**
 * 測試用啟動類
 * <p>
 * 整合 TestcontainersConfiguration 提供 PostgreSQL 測試容器，
 * 可用 ./gradlew bootTestRun 啟動本地開發環境。
 * </p>
 */
public class TestDocumentationMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocumentationMcpServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
