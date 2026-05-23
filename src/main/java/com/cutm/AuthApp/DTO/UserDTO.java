package com.cutm.AuthApp.DTO;

import com.cutm.AuthApp.Entity.Provider;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    private UUID id;
    private String name;
    private String email;

    // Note: You usually don't send the password back to the frontend in a DTO
    // But if this is used for Registration (incoming data), you keep it.
    private String password;

    private String image;
    private boolean isEnabled;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Builder.Default
    private Provider provider = Provider.LOCAL;

    @Builder.Default
    private Set<RoleDTO> roles = new HashSet<>();
}