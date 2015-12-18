package com.softmotions.weboot.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Checks if a date is after given one
 *
 * @author Konstantin Zolotukhin (konstantin.a.zolotukhin)
 */

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AfterValidator.class)
@Documented
public @interface After {
    /*
     * Date to compare with
     */
    String date();

    /*
     * Date format. It is be used to initialize SimpleDateFormat to parse date string.
     */
    String dateFormat() default "dd.MM.yyyy";


    String message() default "{com.softmotions.commons.validator.After.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
