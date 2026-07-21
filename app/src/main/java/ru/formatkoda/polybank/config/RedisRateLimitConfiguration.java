package ru.formatkoda.polybank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class RedisRateLimitConfiguration {

	@Bean
	@SuppressWarnings("rawtypes")
	public DefaultRedisScript<List> rateLimitScript() {
		DefaultRedisScript<List> script = new DefaultRedisScript<>();

		script.setLocation(new ClassPathResource("redis/rate-limit.lua"));
		script.setResultType(List.class);

		return script;
	}
}
