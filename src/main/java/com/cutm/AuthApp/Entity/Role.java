package com.cutm.AuthApp.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id = UUID.randomUUID();
    @Column(unique = true, nullable = false)
    private String name;
}
