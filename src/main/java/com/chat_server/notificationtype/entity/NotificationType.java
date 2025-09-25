package com.chat_server.notificationtype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "notification_type")
public class NotificationType {

    @Id
    @Column(name = "notification_type_id")
    private int notificationTypeId;

    @Column(name = "notification_type_name")
    private String notificationTypeName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_popup")
    private boolean isPopup;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
