package io.heron;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HeronApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeronApplication.class, args);
	}

}
