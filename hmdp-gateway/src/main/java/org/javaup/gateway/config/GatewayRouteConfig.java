package org.javaup.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator shopRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("shop-service", r -> r
                        .path("/api/shop-type/**", "/api/shop/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://hmdp-shop-service"))
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://hmdp-user-service"))
                .route("voucher-service", r -> r
                        .path("/api/voucher/**", "/api/voucher-order/**", "/api/upload/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://hmdp-voucher-service"))
                .route("blog-service", r -> r
                        .path("/api/blog/**", "/api/blog-comments/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://hmdp-blog-service"))
                .route("follow-service", r -> r
                        .path("/api/follow/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://hmdp-follow-service"))
                .build();
    }
}
