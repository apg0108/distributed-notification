package com.sagant.distributednotification.api.validation;

import java.util.regex.Pattern;

import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRecipientForChannelValidator implements ConstraintValidator<ValidRecipientForChannel, NotificationRequest> {

   private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

   @Override
   public boolean isValid(final NotificationRequest request, final ConstraintValidatorContext context) {
      if (request == null || request.channel() != NotificationChannel.EMAIL) {
         return true;
      }

      final String recipient = request.recipient();
      if (recipient == null || recipient.isBlank() || EMAIL_PATTERN.matcher(recipient).matches()) {
         return true;
      }

      context.disableDefaultConstraintViolation();
      context
            .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode("recipient")
            .addConstraintViolation();
      return false;
   }
}
