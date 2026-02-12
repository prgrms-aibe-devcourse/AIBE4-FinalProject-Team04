package kr.java.patchnotedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PatchnoteDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatchnoteDemoApplication.class, args);
    }
}
