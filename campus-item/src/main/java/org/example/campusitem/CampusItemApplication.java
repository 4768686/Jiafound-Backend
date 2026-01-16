package org.example.campusitem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@SpringBootApplication
@MapperScan("org.example.campusitem.mapper")
public class CampusItemApplication {
    @Value("${redis.url}")
    private String redisUrl;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.password}")
    private String redisPassword;

    @Bean
    public JedisPooled jedisPooled() {
        HostAndPort hostAndPort = new HostAndPort(redisUrl, redisPort);

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .connectionTimeoutMillis(5000)
                .socketTimeoutMillis(5000)
                .build();

        return new JedisPooled(hostAndPort, clientConfig);
    }

    public static void main(String[] args) {
        SpringApplication.run(CampusItemApplication.class, args);
    }

}
