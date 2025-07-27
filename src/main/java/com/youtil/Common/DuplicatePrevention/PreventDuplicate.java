package com.youtil.Common.DuplicatePrevention;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicate {

    String action();

    /**
     * 중복 체크에 사용할 데이터 필드들
     * Path Variable이나 Query Parameter 이름
     */
    String[] dataFields() default {};
}
