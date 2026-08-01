package com.sagant.distributednotification.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRecipientForChannelValidator.class)
public @interface ValidRecipientForChannel {

   String message() default "recipient must be a valid email address when channel is EMAIL";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};
}
