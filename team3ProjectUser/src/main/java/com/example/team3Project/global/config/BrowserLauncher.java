package com.example.team3Project.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("!test")
public class BrowserLauncher implements ApplicationRunner {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 개발 환경에서만 사용 - 로컬 개발 시 브라우저 자동 오픈
     * 프로덕션(Gateway 기반) 환경에서는 사용되지 않음
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 로컬 개발 환경에서만 사용 (Gateway 프록시 구조와 무관)
        String url = "http://localhost:" + serverPort;
        
        // 서버가 완전히 시작될 시간을 주기 위해 2초 대기
        Thread.sleep(2000);
        
        String os = System.getProperty("os.name").toLowerCase();
        
        try {
            if (os.contains("win")) {
                // Windows: PowerShell로 브라우저 실행
                new ProcessBuilder("powershell.exe", "-c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
            System.out.println("========================================");
            System.out.println("브라우저 자동 오픈: " + url);
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("브라우저 오픈 실패: " + e.getMessage());
        }
    }
}
