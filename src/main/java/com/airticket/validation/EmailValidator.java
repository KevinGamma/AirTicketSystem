package com.airticket.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    
    @Autowired
    private MessageSource messageSource;
    
    @Override
    public void initialize(ValidEmail constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }

        String allowedCharsPattern = "^[a-zA-Z0-9@._-]+$";
        if (!email.matches(allowedCharsPattern)) {
            context.disableDefaultConstraintViolation();
            String message = messageSource.getMessage("validation.email.invalidChars", null, LocaleContextHolder.getLocale());
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
            return false;
        }

        String basicEmailPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(basicEmailPattern)) {
            context.disableDefaultConstraintViolation();
            String message = messageSource.getMessage("validation.email.invalidFormat", null, LocaleContextHolder.getLocale());
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}