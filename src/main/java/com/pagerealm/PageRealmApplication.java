package com.pagerealm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.pagerealm", "com.coupons_points"})
@EntityScan(basePackages = {"com.pagerealm", "com.coupons_points"})
@SpringBootApplication(scanBasePackages = {"com.pagerealm", "com.coupons_points"})
public class PageRealmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PageRealmApplication.class, args);
    }

}
