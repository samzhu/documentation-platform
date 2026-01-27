package io.github.samzhu.documentation.platform;

import org.springframework.boot.SpringApplication;

public class TestDocumentationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocumentationPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
