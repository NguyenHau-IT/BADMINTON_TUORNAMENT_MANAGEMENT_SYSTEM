package com.example.btms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * 
 * - Static resource handling with caching
 * - Rate limiting for API endpoints
 * - CORS configuration (if needed)
 * 
 * @author BTMS Team
 * @version 1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Autowired
        private RateLimitInterceptor rateLimitInterceptor;

        /**
         * Configure static resource handlers with caching
         */
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Static resources with long cache (1 year for versioned files)
                registry.addResourceHandler("/css/**")
                                .addResourceLocations("classpath:/static/css/")
                                .setCachePeriod(31536000); // 1 year

                registry.addResourceHandler("/js/**")
                                .addResourceLocations("classpath:/static/js/")
                                .setCachePeriod(31536000);

                // Icons and images - shorter cache (1 week)
                registry.addResourceHandler("/icons/**")
                                .addResourceLocations("classpath:/icons/")
                                .setCachePeriod(604800); // 1 week

                registry.addResourceHandler("/images/**")
                                .addResourceLocations("classpath:/static/images/")
                                .setCachePeriod(604800);

                // Sounds - medium cache (1 day)
                registry.addResourceHandler("/sounds/**")
                                .addResourceLocations("classpath:/static/sounds/")
                                .setCachePeriod(86400);

                // Downloads - no cache (always fresh)
                registry.addResourceHandler("/downloads/**")
                                .addResourceLocations("classpath:/downloads/")
                                .setCachePeriod(0); // No cache for download files
        }

        /**
         * Configure interceptors
         */
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                // Apply rate limiting to API endpoints only
                registry.addInterceptor(rateLimitInterceptor)
                                .addPathPatterns("/api/**")
                                .excludePathPatterns(
                                                "/api/sse/**" // Exclude SSE endpoints (they have their own limits)
                                );
        }
}
