package com.example.team3Project.global.config;

/*
스프링이 캐시 어노테이션을 활성화하도록 설정해야 한다.
@EnableCachig 을 사용하려면 캐시 인프라를 자동 구성한다.
공식문서에서 해당 어노테이션은 @Configuration 클래스에 두는 것을 권장한다.
*/

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration // 해당 클래스가 어플리케이션 설정용 클래스임을 나타냄
@EnableCaching // 어노테이션 기반 캐시 기능을 활성화한다.

// 캐시 활성화만 해주면 되기 때문에 클래스의 내용은 비어 있어도 괜찮다. -> 어노테이션만 이용
public class Cacheconfig {
}

 /*
 configuration 클래스 : 앱이 어떻게 동작할지 미리 세팅해두는 파일
 - 캐시 기능 켜기
 - 보안 설정
 - Swagger 설정
 - 날짜 포맷 설정
 - CORS 설정
 */