package com.tyme.payment;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class BaseRedisIntegrationTest {

    protected static final RedisContainer REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    }
}