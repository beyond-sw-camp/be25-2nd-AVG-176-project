package com.example.team3ProjectEureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class Team3ProjectEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team3ProjectEurekaApplication.class, args);
	}

}
