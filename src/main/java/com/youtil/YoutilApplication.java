package com.youtil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YoutilApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoutilApplication.class, args);
	}

}
