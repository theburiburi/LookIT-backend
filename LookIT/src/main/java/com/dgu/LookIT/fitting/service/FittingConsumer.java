package com.dgu.LookIT.fitting.service;

import com.dgu.LookIT.fitting.dto.request.FittingRequestMessage;
import com.dgu.LookIT.global.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FittingConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final S3FileService s3FileService;

    @PostConstruct
    public void startConsumer() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    String json = redisTemplate.opsForList()
                            .rightPop(RedisKeyConstants.FITTING_QUEUE, 60, TimeUnit.SECONDS);

                    if (json != null) {
                        FittingRequestMessage message = objectMapper.readValue(json, FittingRequestMessage.class);
                        s3FileService.processFittingFromQueue(message);
                    }
                } catch (Exception e) {
                    log.error("Consumer 처리 중 예외 발생", e);
                }
            }
        });
        thread.setName("fitting-queue-consumer");
        thread.setDaemon(true);
        thread.start();
    }

}

