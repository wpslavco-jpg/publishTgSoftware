package com.geekparser.contentplatform;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContentPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentPlatformApplication.class, args);
	}

}
