package com.pagerealm.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * CORS（跨來源資源共享，Cross-Origin Resource Sharing）是一種瀏覽器安全機制，允許網頁從不同來源（domain）請求資源。
 * 預設瀏覽器只允許同源請求，CORS 讓伺服器能設定哪些外部網域可存取 API，避免未授權的跨站請求，提升安全性。
 * 常見用途：前端（如 React、Vue）向後端 API 發送請求時，需設置 CORS 允許前端網域。
 */

//此類別為CORS全域配置，可在對應的controller加上@CrossOrigin註解達成controller級別配置
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${frontend.url}")
    private String frontendUrl;


    @Bean
    public WebMvcConfigurer corsConfigurer(){
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // CORS配置路徑1
                String[] origins = frontendUrl.split(",");
                registry.addMapping("/api/**")
                        .allowedOrigins(frontendUrl)
                        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);

                // CORS配置路徑2
                registry.addMapping("/**")
                        .allowedOrigins(frontendUrl)
                        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

            //User Avatar檔案上傳路徑 (已改用AWS S3儲存)
//            @Override
//            public void addResourceHandlers(ResourceHandlerRegistry registry) {
//
//                String fileLocation = Paths.get(avatarDir).toAbsolutePath().normalize().toUri().toString();
//                registry.addResourceHandler("/images/**")
//                        .addResourceLocations(fileLocation)   // 例：file:/var/app/uploads/avatar/
//                        .setCachePeriod(3600);
//            }
        };
    }


}