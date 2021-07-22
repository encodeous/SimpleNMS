package ca.encodeous.simplenms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface NMSMethod {

    //TODO move this into repeatable annotation
    NMSVersionName[] versionNames() default {};
}
