package ca.encodeous.simplenms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

import static ca.encodeous.simplenms.proxy.NMSProvider.NMS_VERSION;

/**
 * @author theminecoder
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NMSClass {

    public enum NMSType {
        NMS("net.minecraft.server.%version%.", "net.minecraft.", "net.minecraft.%version%."),
        CRAFTBUKKIT("org.bukkit.craftbukkit.%version%.", "org.bukkit.craftbukkit."),
        OTHER("");

        private final String[] prefixes;

        NMSType(String... prefix) {
            this.prefixes = prefix;
        }

        public String[] getPrefixes() {
            return prefixes;
        }

        public String[] getClassNames(String className) {
            ArrayList<String> prefixes = new ArrayList<>();
            for(String pre : prefixes){
                prefixes.add((pre + className).replaceFirst("%version%", NMS_VERSION));
            }
            return prefixes.toArray(new String[0]);
        }
    }

    NMSType type() default NMSType.OTHER;

    String value() default "";
}
