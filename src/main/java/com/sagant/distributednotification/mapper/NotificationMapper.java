package com.sagant.distributednotification.mapper;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "priority", source = "priority", defaultValue = "MEDIUM")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "lastError", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    Notification toEntity(final NotificationRequest request);

    NotificationResponse toResponse(final Notification notification);
}
