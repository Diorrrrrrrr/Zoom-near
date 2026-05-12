package kr.zoomnear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/// ZOOM NEAR(주니어) API 애플리케이션 부트스트랩 엔트리포인트.
/// kr.zoomnear 패키지를 컴포넌트 스캔 루트로 사용한다.
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ZoomnearApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZoomnearApplication.class, args);
    }
}
