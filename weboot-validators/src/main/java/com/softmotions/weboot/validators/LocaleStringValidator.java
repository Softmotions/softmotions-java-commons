package com.softmotions.weboot.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * Validator for @LocaleString constraint.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class LocaleStringValidator implements ConstraintValidator<LocaleString, String> {

    private LocaleStringLang lang;

    private String extra;

    @Override
    public void initialize(LocaleString constraintAnnotation) {
        this.lang = constraintAnnotation.lang();
        this.extra = constraintAnnotation.extra();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        boolean isValid = false;
        switch (lang) {
            case RU:
                isValid = validateRU(value);
        }
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "{com.softmotions.commons.validator.LocaleStringValidator.message}")
                    .addConstraintViolation();
        }

        return isValid;
    }


    private boolean validateRU(String s) {
        for (int i = 0, l = s.length(); i < l; ++i) {
            char c = s.charAt(i);
            if (isExtra(c)) {
                continue;
            }
            if (!isCyrillic(c)) {
                return false;
            }
        }
        return true;
    }

    boolean isExtra(char c) {
        return (extra.indexOf(c) != -1);
    }

    boolean isCyrillic(char c) {
        return Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(c));
    }

}
