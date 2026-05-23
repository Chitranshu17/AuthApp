package com.cutm.AuthApp.DTO;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleDTO {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private String name;
}