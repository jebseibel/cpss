package com.seibel.cpss.common.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
public class PasswordResetToken extends BaseDomain {
    private String userExtid;
    private String token;
    private LocalDateTime expiresAt;
    private boolean used;
}
