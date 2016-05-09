package com.softmotions.weboot.jaxrs;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.Human;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ValidationException extends MessageException {

    private static final Logger log = LoggerFactory.getLogger(ValidationException.class);

    public ValidationException() {
    }

    public ValidationException(Set cvlist) {
        this(cvlist, true);
    }

    public ValidationException(Set cvlist, boolean useHumanNames) {
        for (Object o : cvlist) {
            ConstraintViolation cv = (ConstraintViolation) o;
            String entity = cv.getRootBeanClass().getSimpleName();
            Human human = null;
            if (useHumanNames) {
                if (cv.getLeafBean() != null) {
                    human = cv.getLeafBean().getClass().getAnnotation(Human.class);
                }
                if (human == null) {
                    human = (Human) cv.getRootBeanClass().getAnnotation(Human.class);
                }
            }
            if (human != null) {
                entity = human.value();
            }
            String path = cv.getPropertyPath().toString();
            String field = path;
            PropertyDescriptor pd;
            if (cv.getRootBean() != null) {
                try {
                    pd = PropertyUtils.getPropertyDescriptor(cv.getRootBean(), path);
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        human = useHumanNames ? readMethod.getAnnotation(Human.class) : null;
                        if (human != null) {
                            field = human.value();
                        }
                    }
                } catch (Exception e) {
                    log.warn("", e);
                }
            }
            addMessage(field + " (" + entity + "): " + cv.getMessage(), true);
        }
    }

    public MessageException addMessage(String message) {
        return super.addMessage(message, true);
    }

    public ValidationException(String message) {
        super(message, true);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
