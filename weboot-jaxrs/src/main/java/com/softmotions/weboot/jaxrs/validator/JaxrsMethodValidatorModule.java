package com.softmotions.weboot.jaxrs.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.Path;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.not;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.softmotions.commons.lifecycle.Start;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JaxrsMethodValidatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JaxrsMethodValidator.class).in(Singleton.class);
        ValidatorGroupsListener listener = new ValidatorGroupsListener();
        bindListener(any(), listener);
        MethodValidatorService methodValidatorService = new MethodValidatorService(listener);
        requestInjection(methodValidatorService);
        bind(MethodValidatorService.class).toInstance(methodValidatorService);

        // ValidateJsonBodyInterceptor
        ValidateJsonBodyInterceptor interceptor = new ValidateJsonBodyInterceptor();
        requestInjection(interceptor);
        bindInterceptor(any(),
                        annotatedWith(ValidateREST.class),
                        interceptor);
        bindInterceptor(annotatedWith(ValidateREST.class),
                        not(annotatedWith(ValidateREST.class))
                                .and(annotatedWith(Path.class)),
                        interceptor);
    }

    static class ValidatorGroupsListener implements TypeListener {

        final List<ValidatorsGroup> validatorGroups = new ArrayList<>();

        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            Class<? super I> clazz = type.getRawType();
            ValidatorsGroup vg = clazz.getAnnotation(ValidatorsGroup.class);
            if (vg != null) {
                validatorGroups.add(vg);
            }
            ValidatorsGroups vgs = clazz.getAnnotation(ValidatorsGroups.class);
            if (vgs != null) {
                Collections.addAll(validatorGroups, vgs.groups());
            }
        }
    }

    public static class MethodValidatorService {

        @Inject
        JaxrsMethodValidator validator;

        ValidatorGroupsListener listener;

        public MethodValidatorService(ValidatorGroupsListener listener) {
            this.listener = listener;
        }

        @Start
        public void start() {
            Objects.requireNonNull(validator);
            Objects.requireNonNull(listener);
            listener.validatorGroups.forEach(validator::registerValidatorsGroup);
            listener.validatorGroups.clear();
        }
    }
}
