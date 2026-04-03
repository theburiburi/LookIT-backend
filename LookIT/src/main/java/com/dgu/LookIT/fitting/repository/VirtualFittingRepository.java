package com.dgu.LookIT.fitting.repository;

import com.dgu.LookIT.fitting.domain.VirtualFitting;
import com.dgu.LookIT.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VirtualFittingRepository extends JpaRepository<VirtualFitting, Long> {

    // 특정 사용자 ID로 모든 결과 조회 (최신순 정렬)
    List<VirtualFitting> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<VirtualFitting> findAllByUserId(Long userId);

    Optional<VirtualFitting> findByUserId(Long userId);
}
