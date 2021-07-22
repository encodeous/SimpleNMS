package ca.encodeous.simplenms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static ca.encodeous.simplenms.proxy.NMSProvider.NMS_VERSION;

/**
 * @author theminecoder
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NMSClass {

    public enum NMSType {
        NMS("net.minecraft.server.%version%."),
        CRAFTBUKKIT("org.bukkit.craftbukkit.%version%."),
        OTHER("");

        private final String prefix;

        NMSType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getClassName(String className) {
            return (prefix + className).replaceFirst("%version%", NMS_VERSION);
        }
    }

    NMSType type() default NMSType.OTHER;

    String value() default "";
}
