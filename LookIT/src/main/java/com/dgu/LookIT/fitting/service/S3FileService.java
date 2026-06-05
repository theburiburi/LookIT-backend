package com.dgu.LookIT.fitting.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dgu.LookIT.exception.CommonException;
import com.dgu.LookIT.exception.ErrorCode;
import com.dgu.LookIT.fitting.domain.VirtualFitting;
import com.dgu.LookIT.fitting.dto.response.FittingResultResponse;
import com.dgu.LookIT.fitting.repository.VirtualFittingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final AmazonS3 s3Client;
    private final VirtualFittingRepository virtualFittingRepository;
    private final FittingProcessor fittingProcessor;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // 파일 업로드
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        s3Client.putObject(bucketName, fileName, file.getInputStream(), metadata);
        return s3Client.getUrl(bucketName, fileName).toString();
    }

    // 동기 요청 처리
    public String processFitting(Long userId, MultipartFile clothesImage, MultipartFile bodyImage) {
        return fittingProcessor.processSync(userId, clothesImage, bodyImage);
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
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_VIRTUAL_FITTING));
        if (!vf.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ROLE);
        }
        virtualFittingRepository.delete(vf);
        return "삭제 완료: " + fittingId;
    }
}
