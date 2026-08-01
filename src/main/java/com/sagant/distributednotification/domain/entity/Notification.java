package com.sagant.distributednotification.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(nullable = false)
   private String recipient;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private NotificationChannel channel;

   @Column
   private String subject;

   @Column(nullable = false, columnDefinition = "TEXT")
   private String body;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 10)
   private NotificationPriority priority = NotificationPriority.MEDIUM;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(columnDefinition = "jsonb")
   private Map<String, String> metadata;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private NotificationStatus status = NotificationStatus.PENDING;

   @Column(name = "retry_count", nullable = false)
   private int retryCount = 0;

   @Column(name = "last_error", columnDefinition = "TEXT")
   private String lastError;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private Instant updatedAt;

   @Column(name = "sent_at")
   private Instant sentAt;
}
