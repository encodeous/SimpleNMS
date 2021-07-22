package ca.encodeous.simplenms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author theminecoder
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NMSField {

    public enum Type {
        GETTER,
        SETTER
    }

    Type value() default Type.GETTER;

    //TODO move this into repeatable annotation
    NMSVersionName[] versionNames() default {};

}
