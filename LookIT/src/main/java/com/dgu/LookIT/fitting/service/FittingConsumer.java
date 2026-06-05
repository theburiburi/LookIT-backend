package com.dgu.LookIT.fitting.service;

import com.dgu.LookIT.fitting.dto.request.FittingRequestMessage;
import com.dgu.LookIT.global.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FittingConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final FittingProcessor fittingProcessor;

    @Value("${fitting.queue.consumer-timeout-seconds:60}")
    private long consumerTimeoutSeconds;

    private final String workerId = UUID.randomUUID().toString();

    @PostConstruct
    public void startConsumer() {
        Thread thread = new Thread(() -> {
            log.info("가상 피팅 Redis 큐 Consumer 시작. queue={}, workerId={}",
                    RedisKeyConstants.FITTING_QUEUE, workerId);
            while (true) {
                try {
                    String json = redisTemplate.opsForList()
                            .rightPop(RedisKeyConstants.FITTING_QUEUE, consumerTimeoutSeconds, TimeUnit.SECONDS);

                    if (json != null) {
                        FittingRequestMessage message = objectMapper.readValue(json, FittingRequestMessage.class);
                        fittingProcessor.processFromQueue(message);
                    }
                } catch (Exception e) {
                    log.error("Redis 큐 Consumer 처리 중 예외 발생. workerId={}", workerId, e);
                }
            }
        });
        thread.setName("fitting-queue-consumer");
        thread.setDaemon(true);
        thread.start();
    }

}
