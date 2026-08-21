package com.example.anno;

import com.example.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
 import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = {StateValidation.class}) // 指定提供校验规则的类
public @interface State {

    String message() default "state参数值只能是已发布或草稿";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
