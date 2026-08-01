package com.sagant.distributednotification.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "priority", source = "priority", defaultValue = "MEDIUM")
   @Mapping(target = "status", ignore = true)
   Notification toEntity(final NotificationRequest request);

   NotificationResponse toResponse(final Notification notification);
}
