package com.softmotions.weboot.jaxrs.validator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.softmotions.commons.ClassUtils;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class ValidateJsonBodyInterceptor implements MethodInterceptor {

    private final Logger log = LoggerFactory.getLogger(ValidateJsonBodyInterceptor.class);

    @Inject
    private JaxrsMethodValidator methodValidator;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();
        ValidateREST vm = ClassUtils.getAnnotation(method, ValidateREST.class);
        if (vm == null) {
            vm = method.getDeclaringClass().getAnnotation(ValidateREST.class);
        }
        if (vm == null) {
            log.error("Possible annotation misconfiguration. Method/Class: {} is not annotated by {}",
                      method, ValidateREST.class.getName());
            return invocation.proceed();
        }
        validate(vm, method, arguments);
        return invocation.proceed();
    }

    @Nonnull
    private ValidationContext validate(ValidateREST vm, Method method, Object[] arguments) {
        String[] groups = vm.includeGroups();
        String[] rules = vm.validators();
        AtomicReference<ValidationContext> vref = new AtomicReference<>();
        List<JaxrsMethodValidationError> errors =
                methodValidator.validateMethod(groups, rules, method, arguments, vref::set);
        if (!errors.isEmpty()) {
            throw new JaxrsMethodValidationException(errors);
        }
        return Objects.requireNonNull(vref.get());
    }
}
