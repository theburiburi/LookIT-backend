package com.dgu.LookIT.fitting.service;

import com.dgu.LookIT.exception.CommonException;
import com.dgu.LookIT.exception.ErrorCode;
import com.dgu.LookIT.fitting.dto.request.FittingRequestMessage;
import com.dgu.LookIT.global.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingQueueProducer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(Long userId, MultipartFile clothesImage, MultipartFile bodyImage) {
        try {
            FittingRequestMessage message = FittingRequestMessage.builder()
                    .userId(userId)
                    .clothesImageBase64(Base64.getEncoder().encodeToString(clothesImage.getBytes()))
                    .bodyImageBase64(Base64.getEncoder().encodeToString(bodyImage.getBytes()))
                    .build();

            String json = objectMapper.writeValueAsString(message);
            Long queueSize = redisTemplate.opsForList().leftPush(RedisKeyConstants.FITTING_QUEUE, json);

            log.info("가상 피팅 요청이 Redis 큐에 저장되었습니다. queue={}, size={}",
                    RedisKeyConstants.FITTING_QUEUE, queueSize);
        } catch (Exception e) {
            log.error("가상 피팅 요청 큐 저장 실패", e);
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
