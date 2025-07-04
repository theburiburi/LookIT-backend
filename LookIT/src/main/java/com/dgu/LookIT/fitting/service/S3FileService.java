package com.dgu.LookIT.fitting.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dgu.LookIT.exception.CommonException;
import com.dgu.LookIT.exception.ErrorCode;
import com.dgu.LookIT.fitting.domain.VirtualFitting;
import com.dgu.LookIT.fitting.dto.request.FittingRequestMessage;
import com.dgu.LookIT.fitting.dto.response.FittingResultResponse;
import com.dgu.LookIT.fitting.repository.VirtualFittingRepository;
import com.dgu.LookIT.user.domain.User;
import com.dgu.LookIT.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final AmazonS3 s3Client;
    private final UserRepository userRepository;
    private final VirtualFittingRepository virtualFittingRepository;
    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final String FITTING_QUEUE = "virtual_fitting_queue";

    // 파일 업로드
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(bucketName, fileName, file.getInputStream(), metadata);
        return s3Client.getUrl(bucketName, fileName).toString();
    }

    // 비동기 요청: Redis 큐에 저장
    public void processFittingAsync(Long userId, MultipartFile clothesImage, MultipartFile bodyImage) {
        try {
            String clothesBase64 = Base64.getEncoder().encodeToString(clothesImage.getBytes());
            String bodyBase64 = Base64.getEncoder().encodeToString(bodyImage.getBytes());

            FittingRequestMessage message = FittingRequestMessage.builder()
                    .userId(userId)
                    .clothesImageBase64(clothesBase64)
                    .bodyImageBase64(bodyBase64)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(FITTING_QUEUE, json);

            log.info("가상 피팅 요청이 Redis 큐에 저장되었습니다");
        } catch (Exception e) {
            log.error("가상 피팅 요청 큐 저장 실패", e);
        }
    }

    // 동기 요청 처리
    public String processFitting(Long userId, MultipartFile clothesImage, MultipartFile bodyImage) throws IOException {
        byte[] clothesBytes = clothesImage.getBytes();
        byte[] bodyBytes = bodyImage.getBytes();
        try {
            return processFittingInternal(userId, clothesBytes, bodyBytes);
        } catch (Exception e) {
            log.error("동기 가상 피팅 실패", e);
            return null;
        }
    }

    // Redis Consumer가 호출하는 처리 로직
    public void processFittingFromQueue(FittingRequestMessage message) {
        try {
            byte[] clothesBytes = Base64.getDecoder().decode(message.getClothesImageBase64());
            byte[] bodyBytes = Base64.getDecoder().decode(message.getBodyImageBase64());

            String resultUrl = processFittingInternal(message.getUserId(), clothesBytes, bodyBytes);
            log.info("가상 피팅 처리 성공: {}", resultUrl);

        } catch (Exception e) {
            log.error("Queue 기반 가상 피팅 처리 실패", e);
        }
    }

    // 실제 WebClient 호출 및 결과 저장
    private String processFittingInternal(Long userId, byte[] clothes, byte[] body) throws Exception {
        ByteArrayResource bodyRes = new ByteArrayResource(body) {
            @Override public String getFilename() { return "body.png"; }
        };
        ByteArrayResource clothesRes = new ByteArrayResource(clothes) {
            @Override public String getFilename() { return "clothes.png"; }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("body", bodyRes).filename("body.png").contentType(MediaType.IMAGE_PNG);
        builder.part("clothes", clothesRes).filename("clothes.png").contentType(MediaType.IMAGE_PNG);

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

    // 결과 조회
    public List<FittingResultResponse> getFittingResults(Long userId) {
        return virtualFittingRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FittingResultResponse::from)
                .toList();
    }

    // 결과 삭제
    public String deleteVirtualFitting(Long userId, Long fittingId) {
        VirtualFitting vf = virtualFittingRepository.findById(fittingId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUNT_VIRTUAL_FITTING));
        virtualFittingRepository.delete(vf);
        return "삭제 완료: " + fittingId;
    }
}