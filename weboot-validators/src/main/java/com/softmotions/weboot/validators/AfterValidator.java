package com.softmotions.weboot.validators;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Konstantin Zolotukhin (konstantin.a.zolotukhin@gmail.com)
 */
public class AfterValidator implements ConstraintValidator<After, Date> {
    Date date;

    @Override
    public void initialize(After constraintAnnotation) {
        DateFormat dateFormat = new SimpleDateFormat(constraintAnnotation.dateFormat());
        try {
            this.date = dateFormat.parse(constraintAnnotation.date());
        } catch (ParseException e) {
            throw new ConstraintDefinitionException("Cannot parse date '" + constraintAnnotation.date() + "' with pattern '"
                                                    + constraintAnnotation.dateFormat() + "'");
        }
    }

    @Override
    public boolean isValid(Date value, ConstraintValidatorContext context) {
        return value.after(date);
    }

}
