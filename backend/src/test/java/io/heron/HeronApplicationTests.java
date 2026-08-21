package io.heron;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import io.heron.market.LatestPriceRepository;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest
@ActiveProfiles("test")
class HeronApplicationTests {

	@MockBean
	LatestPriceRepository latestPriceRepository;

	@MockBean
	RedisConnectionFactory redisConnectionFactory;

	@MockBean
	org.springframework.data.redis.connection.ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

	@Test
	void contextLoads() {
	}

}
