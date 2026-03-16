package com.chat_server.gender.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "gender",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gender_name", columnNames = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 10)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}