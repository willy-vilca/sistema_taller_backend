package com.tallermecanico.api;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TallerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TallerApiApplication.class, args);
	}

}
