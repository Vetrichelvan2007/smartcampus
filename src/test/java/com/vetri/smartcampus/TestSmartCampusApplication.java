package com.vetri.smartcampus;

import org.springframework.boot.SpringApplication;

public class TestSmartCampusApplication {

	public static void main(String[] args) {
		SpringApplication.from(SmartCampusApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
