package com.dgu.LookIT.fitting.service;

import com.dgu.LookIT.exception.CommonException;
import com.dgu.LookIT.exception.ErrorCode;
import com.dgu.LookIT.fitting.domain.VirtualFitting;
import com.dgu.LookIT.fitting.dto.request.FittingRequestMessage;
import com.dgu.LookIT.fitting.repository.VirtualFittingRepository;
import com.dgu.LookIT.user.domain.User;
import com.dgu.LookIT.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingProcessor {

    private final UserRepository userRepository;
    private final VirtualFittingRepository virtualFittingRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public String processSync(Long userId, MultipartFile clothesImage, MultipartFile bodyImage) {
        try {
            return processFittingInternal(userId, clothesImage.getBytes(), bodyImage.getBytes());
        } catch (Exception e) {
            log.error("동기 가상 피팅 실패", e);
            throw new CommonException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public void processFromQueue(FittingRequestMessage message) {
        try {
            byte[] clothesBytes = Base64.getDecoder().decode(message.getClothesImageBase64());
            byte[] bodyBytes = Base64.getDecoder().decode(message.getBodyImageBase64());

            String resultUrl = processFittingInternal(message.getUserId(), clothesBytes, bodyBytes);
            log.info("Redis 큐 기반 가상 피팅 처리 성공. userId={}, resultUrl={}",
                    message.getUserId(), resultUrl);
        } catch (Exception e) {
            log.error("Redis 큐 기반 가상 피팅 처리 실패. userId={}", message.getUserId(), e);
        }
    }

    private String processFittingInternal(Long userId, byte[] clothes, byte[] body) throws Exception {
        ByteArrayResource bodyResource = new ByteArrayResource(body) {
            @Override
            public String getFilename() {
                return "body.png";
            }
        };
        ByteArrayResource clothesResource = new ByteArrayResource(clothes) {
            @Override
            public String getFilename() {
                return "clothes.png";
            }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("body", bodyResource).filename("body.png").contentType(MediaType.IMAGE_PNG);
        builder.part("clothes", clothesResource).filename("clothes.png").contentType(MediaType.IMAGE_PNG);

        String result = webClient.post()
                .uri("/fitting")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(result);
        String imageUrl = root.path("image").path("url").asText();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER));

        VirtualFitting fitting = VirtualFitting.builder()
                .user(user)
                .resultImageUrl(imageUrl)
                .build();

        virtualFittingRepository.save(fitting);
        return imageUrl;
    }
}
