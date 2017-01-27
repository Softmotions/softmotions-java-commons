package com.softmotions.weboot.jaxrs.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("CodeBlock2Expr")
@ThreadSafe
@Singleton
public class JaxrsMethodValidator {

    private static final Logger log = LoggerFactory.getLogger(JaxrsMethodValidator.class);

    private Map<String, Validator> validators = new ConcurrentHashMap<>();

    private Map<String, ValidatorsGroup> validatorsGroups = new ConcurrentHashMap<>();

    @Inject
    public JaxrsMethodValidator() {

        ///////////////////////////////////////////////////////////
        //                   Default validators                  //
        ///////////////////////////////////////////////////////////

        registerValidator("notNull", (value, args) -> {
            return (value != null);
        });
        registerValidator("notBlank", (value, args) -> {
            return value != null && !StringUtils.isBlank(value.toString());
        });
        registerValidator("regexp", ((value, args) -> {
            return value == null || value.toString().matches(args[0]);
        }));
        registerValidator("email", (value, args) -> {
            return value == null || EmailValidator.getInstance().isValid(value.toString());
        });
        registerValidator("cyrillic", new CyrillicValidator());
        registerValidator("date", new DateValidator());
        registerValidator("oneOf", new OneOfValidator());
        registerValidator("httpUrl", new HttpUrlValidator());
    }


    public void registerValidatorsGroup(ValidatorsGroup g) {
        if (validatorsGroups.containsKey(g.name())) {
            throw new RuntimeException("Attempting to define already defined validation group: " + g.name());
        }
        log.info("Validators group '{}' rules: \n\t{}", g.name(), StringUtils.join(g.validators(), "\n\t"));
        validatorsGroups.put(g.name(), g);
    }

    public void registerValidator(String name, Validator validator) {
        log.info("Validator '{}' registered", name);
        validators.put(name, validator);
    }

    private Iterable<String> collectAllGroups(@Nullable LinkedHashSet<String> ctx, String[] groups) {
        if (ctx == null) {
            ctx = new LinkedHashSet<>();
        }
        for (String g : groups) {
            ValidatorsGroup vg = validatorsGroups.get(g);
            if (vg == null) {
                throw new RuntimeException("Missing validators group: " + g);
            }
            if (vg.includeGroups().length > 0) {
                collectAllGroups(ctx, vg.includeGroups());
            }
            ctx.add(g);
        }
        return ctx;
    }


    public List<JaxrsMethodValidationError> validateMethod(String[] groups,
                                                           String[] rules,
                                                           Method method,
                                                           Object[] arguments,
                                                           @Nullable Consumer<ValidationContext> contextConsumer) {
        List<String> resolvedRules = new ArrayList<>(1 << 6);
        for (String g : collectAllGroups(null, groups)) {
            ValidatorsGroup vg = validatorsGroups.get(g);
            Collections.addAll(resolvedRules, vg.validators());
        }
        Collections.addAll(resolvedRules, rules);
        ValidationContextImpl vctx = new ValidationContextImpl(method, arguments);
        resolvedRules.forEach(vctx::ruleToken);
        vctx.flush();
        if (contextConsumer != null) {
            contextConsumer.accept(vctx);
        }
        return vctx.errors;
    }


    @SuppressWarnings("SwitchStatementWithoutDefaultBranch")
    private class ValidationContextImpl implements ValidationContext {

        private final int FTYPE_BODY = 1;

        private final int FTYPE_REQ_PARAM = 2;

        private final int FTYPE_PATH_PARAM = 3;

        private final Method method;

        private final Object[] arguments;

        private final List<JaxrsMethodValidationError> errors = new ArrayList<>(8);

        private final List<String> validatorArgs = new ArrayList<>(4);

        private final Map<String, Object> validatedValues = new HashMap<>();

        private JsonNode jsonBody;

        private Object bean;

        private boolean beanFetched;

        private String field;

        private String rawField;

        private Validator validator;

        private String validatorName;

        private String message;

        private String prevTranslatedMessage;

        private int ftype;


        private ValidationContextImpl(Method method,
                                      Object[] arguments) {
            this.method = method;
            this.arguments = arguments;
        }

        @Override
        @Nullable
        public JsonNode getJsonBody() {
            return jsonBody;
        }

        @Override
        @Nonnull
        public Map<String, Object> getValidatedValues() {
            return validatedValues;
        }

        void ruleToken(String token) {

            // Field type/name
            if (token.length() > 1 && token.charAt(0) == '@') {
                switch (token.charAt(1)) {
                    case '@':
                        flush();
                        ftype = FTYPE_BODY;
                        field = token.substring(2);
                        this.rawField = token;
                        return;
                    case '&':
                        flush();
                        ftype = FTYPE_REQ_PARAM;
                        field = token.substring(2);
                        this.rawField = token;
                        return;
                    case '^':
                        flush();
                        ftype = FTYPE_PATH_PARAM;
                        field = token.substring(2);
                        this.rawField = token;
                        return;
                }
            }

            // Validator
            boolean isValidator = validators.containsKey(token);
            if (isValidator) {
                if (validator != null && field != null) {
                    validateBean();
                    validatorArgs.clear();
                }
                validatorName = token;
                validator = validators.get(validatorName);
                return;
            }
            if (!token.isEmpty() && token.charAt(0) == '!') {
                message = token.substring(1);
                if (message.isEmpty()) { // reset message on '!'
                    message = null;
                }
            } else if (validator != null) {
                validatorArgs.add(token);
            }
        }

        @Nullable
        @SuppressWarnings("OverlyNestedMethod") // It's a design (~chopper~) baby..
        private Object getBean() {
            if (beanFetched) {
                return bean;
            }

            int aind = -1;
            beanFetched = true;

            switch (ftype) {
                case FTYPE_BODY:
                    for (int i = 0; i < arguments.length; ++i) {
                        //noinspection ObjectEquality
                        if (arguments[i] != null
                            && JsonNode.class.isAssignableFrom(arguments[i].getClass())) {
                            aind = i;
                            break;
                        }
                    }
                    if (aind == -1) {
                        log.error("Unable to find JSON body parameter for method: {}", method);
                    }
                    jsonBody = (JsonNode) arguments[aind];
                    break;
                case FTYPE_PATH_PARAM: {
                    Annotation[][] pan = method.getParameterAnnotations();
                    for (int i = 0; aind == -1 && i < pan.length; ++i) {
                        for (Annotation a : pan[i]) {
                            if (a instanceof PathParam) {
                                PathParam pp = (PathParam) a;
                                if (pp.value().equals(field)) {
                                    aind = i;
                                    break;
                                }
                            }
                        }
                    }
                    if (aind == -1) {
                        log.error("Unable to find @PathParam(\"{}\") annotated argument for method: {}", field, method);
                    }
                    break;
                }
                case FTYPE_REQ_PARAM:
                    //noinspection UnnecessaryCodeBlock
                {   //(see FTYPE_PATH_PARAM switch case)

                    Annotation[][] pan = method.getParameterAnnotations();
                    for (int i = 0; aind == -1 && i < pan.length; ++i) {
                        for (Annotation a : pan[i]) {
                            if (a instanceof QueryParam) {
                                QueryParam pp = (QueryParam) a;
                                if (pp.value().equals(field)) {
                                    aind = i;
                                    break;
                                }
                            }
                        }
                    }
                    if (aind == -1) {
                        log.error("Unable to find @QueryParam(\"{}\") annotated argument for method: {}", field, method);
                    }
                    break;
                }
            }
            if (aind != -1) {
                bean = arguments[aind];
            }
            return bean;
        }

        private String translate(String key, @Nullable Object val) {
            return key;
        }

        private void validateBean() {

            Object b = getBean();
            Object value = null;
            boolean valid;

            switch (ftype) {
                case FTYPE_BODY:
                    if (b != null) {
                        JsonNode at = ((JsonNode) b).at((field.charAt(0) != '/') ? '/' + field : field);
                        validatedValues.put(rawField, at);
                        if (!at.isMissingNode()) {
                            value = at.asText();
                        }
                    }
                    break;
                case FTYPE_PATH_PARAM:
                case FTYPE_REQ_PARAM:
                    value = b;
                    validatedValues.put(rawField, value);
                    break;
            }

            try {
                valid = validator.validate(value, validatorArgs.toArray(new String[validatorArgs.size()]));
            } catch (Exception e) {
                valid = false;
                log.error("Validator: {} thrown exception", validator, e);
            }

            if (!valid) {
                String translatedMessage =
                        translate(message != null
                                  ? message
                                  : "jaxrs.validate.error." + validatorName,
                                  value);
                if (!Objects.equals(prevTranslatedMessage, translatedMessage)) {
                    prevTranslatedMessage = translatedMessage;
                    errors.add(new JaxrsMethodValidationError(field, validatorName));
                }
            }
        }

        private void flush() {
            if (field != null) {
                if (validator != null) {
                    validateBean();
                } else {
                    log.error("Unknown validator: {}", validatorName);
                }
            }
            field = null;
            rawField = null;
            bean = null;
            beanFetched = false;
            validator = null;
            validatorName = null;
            validatorArgs.clear();
            message = null;
            prevTranslatedMessage = null;
        }
    }

    ///////////////////////////////////////////////////////////
    //                      Validators                       //
    ///////////////////////////////////////////////////////////

    public static class CyrillicValidator implements Validator {

        @Override
        public boolean validate(Object value, String... args) {
            if (value == null) {
                return true;
            }
            String extra = args.length > 0 ? args[0] : "";
            String s = value.toString();
            for (int i = 0, l = s.length(); i < l; ++i) {
                char c = s.charAt(i);
                if ((extra.indexOf(c) != -1)) {
                    continue;
                }
                if (!Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(c))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class DateValidator implements Validator {

        // 2011-12-03
        private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

        @Override
        public boolean validate(@Nullable Object value, String... args) {
            if (value == null) {
                return true;
            }
            String format = (args.length > 0) ? args[0] : null;
            if (format == null) { // OK FAST/STRICT CHECKING FOR ISO DATE
                Matcher m = ISO_DATE_PATTERN.matcher(value.toString());
                if (!m.matches()) {
                    return false;
                }
                LocalDate date = LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
                return Objects.equals(Integer.toString(date.getYear()), m.group(1))
                       && Objects.equals(date.getMonthValue(), Integer.parseInt(m.group(2)))
                       && Objects.equals(date.getDayOfMonth(), Integer.parseInt(m.group(3)));
            }
            try { // WE ARE ABLE TO PARSE IT?
                LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {
                return false;
            }
            return true;
        }
    }

    /**
     * String representation of specified value equals
     * to some validator argument.
     */
    public static class OneOfValidator implements Validator {
        @Override
        public boolean validate(@Nullable Object value, String... args) {
            if (value == null) {
                return true;
            }
            String sv = value.toString();
            return ArrayUtils.indexOf(args, sv) != -1;
        }
    }

    public static class HttpUrlValidator implements Validator {
        @Override
        public boolean validate(@Nullable Object value, String... args) {
            if (value == null) {
                return true;
            }
            try {
                URL url = new URL(value.toString());
                if (!"http".equals(url.getProtocol())
                    && !"https".equals(url.getProtocol())) {
                    return false;
                }
            } catch (MalformedURLException e) {
                return false;
            }
            return true;
        }
    }
}
