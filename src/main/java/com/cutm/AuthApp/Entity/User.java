package com.cutm.AuthApp.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id")
    private UUID id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String image;

    private boolean isEnabled;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    // EAGER is required for Spring Security to build the Authentication object
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                // 1. Map the Role entity to the Spring Security object
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    // Runs once right before inserting into the database
//    @PrePersist
//    protected void onCreate() {
//        this.createdAt = Instant.now();
//        this.updatedAt = Instant.now();
//    }
//
//    // Runs every time the entity is updated in the database
//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = Instant.now();
//    }
}


/* When you add @Builder.Default above a field ,you are instructing Lombok:

"If I do not explicitly set this field while using the builder, please use the default value I wrote right here."
Which fields need it?
You only need it on fields where you have used an equals sign (=) to assign a default starting value.*/

/* Hibernate takes over, sees the null timestamps,
 and injects the exact database server time right before running the SQL INSERT command.*/