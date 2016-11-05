package com.softmotions.commons;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jetbrains.annotations.Nullable;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ClassUtils {

    private ClassUtils() {
    }

    public static @Nullable <T extends Annotation> T getAnnotation(
            Method method, Class<T> annotationClass) {
        T ret = method.getAnnotation(annotationClass);
        if (ret != null) {
            return ret;
        }
        Class<?> clazz = method.getDeclaringClass();
        int mods = method.getModifiers();
        if (!Modifier.isPrivate(mods) && !Modifier.isStatic(mods)) {
            for (Field f : clazz.getDeclaredFields()) {
                if ("CGLIB$BOUND".equals(f.getName())) {
                    try {
                        Class<?> superClass = clazz.getSuperclass();
                        //noinspection ObjectEquality
                        if (superClass != null && clazz != superClass) {
                            Method m = superClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                            ret = getAnnotation(m, annotationClass);
                        }
                    } catch (NoSuchMethodException ignored) {
                    }
                    break;
                }
            }
        }
        return ret;
    }
}
