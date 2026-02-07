package io.github.samzhu.documentation.mcp;

import io.github.samzhu.documentation.mcp.config.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SearchProperties.class)
public class DocumentationMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentationMcpServerApplication.class, args);
	}

}
