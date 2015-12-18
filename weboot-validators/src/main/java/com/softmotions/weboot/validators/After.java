package com.softmotions.weboot.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Created by Konstantin Zolotukhin on 17.12.15.
 */

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AfterValidator.class)
@Documented
public @interface After {
    String date();
    String dateFormat() default "dd.MM.yyyy";


    String message() default "{com.softmotions.commons.validator.After.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
