package com.pagerealm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.pagerealm", "com.coupons_points", "com.books", "com.admin_log"})
@EntityScan(basePackages = {"com.pagerealm", "com.coupons_points", "com.books", "com.admin_log"})
@SpringBootApplication(scanBasePackages = {"com.pagerealm", "com.coupons_points", "com.books", "com.admin_log"})
public class PageRealmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PageRealmApplication.class, args);
    }

}
