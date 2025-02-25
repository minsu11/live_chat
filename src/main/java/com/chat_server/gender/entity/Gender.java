package com.chat_server.gender.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

/**
 * packageName    : com.chat_server.gender.entity
 * fileName       : Gender
 * author         : parkminsu
 * date           : 25. 2. 25.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 25.        parkminsu       최초 생성
 */
@Getter
@Entity
@Table(name = "gender")
@NoArgsConstructor
public class Gender {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gender_id")
    private Integer id;

    @Column(name = "gender_name")
    private String genderName;

    @Column(name = "gender_created_at")
    private Date createdAt;
}
