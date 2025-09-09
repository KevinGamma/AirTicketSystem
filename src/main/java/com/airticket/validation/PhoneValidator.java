package com.airticket.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    
    @Autowired
    private MessageSource messageSource;
    
    @Override
    public void initialize(ValidPhone constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.trim().isEmpty()) {
            return true;
        }

        String digitsOnlyPattern = "^\\d+$";
        if (!phone.matches(digitsOnlyPattern)) {
            context.disableDefaultConstraintViolation();
            String message = messageSource.getMessage("validation.phone.numbersOnly", null, LocaleContextHolder.getLocale());
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}