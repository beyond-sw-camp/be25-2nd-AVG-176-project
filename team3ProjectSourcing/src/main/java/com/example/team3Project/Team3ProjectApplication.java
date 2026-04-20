package com.example.team3Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class Team3ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team3ProjectApplication.class, args);
	}
}