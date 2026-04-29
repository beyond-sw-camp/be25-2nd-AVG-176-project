package com.example.team3Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Team3ProjectApplication {

	// 애플리케이션 시작점
	// 여기서 스프링 컨테이너가 올라오고, Security/Controller/Service 빈이 초기화된다.
	public static void main(String[] args) {
		SpringApplication.run(Team3ProjectApplication.class, args);
	}

}
