package com.dgu.LookIT.fitting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FittingRequestMessage {
    private Long userId;
    private String clothesImageBase64;
    private String bodyImageBase64;
}

