package ca.encodeous.simplenms.proxy;

import ca.encodeous.simplenms.annotations.NMSClass;
import ca.encodeous.simplenms.util.NullReference;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import ca.encodeous.simplenms.NMSProxy;
import net.md_5.bungee.api.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

/**
 * @author theminecoder
 */
@SuppressWarnings({"rawtypes", "unchecked", "JavaDoc"})
public final class NMSProvider {

    public static final String NMS_VERSION;

    static {
        String LOADED_VERSION;
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            LOADED_VERSION = packageName.substring(packageName.lastIndexOf(".") + 1);
        } catch (LinkageError e) {
            //Not loaded in bukkit context
            LOADED_VERSION = "";
        }
        NMS_VERSION = LOADED_VERSION;
    }

    protected static final BiMap<Class, Class> proxyToNMSClassMap = HashBiMap.create();
    protected static final NMSProxyInvocationMapper invocationMapper = new NMSProxyInvocationMapper(proxyToNMSClassMap);

    public NMSProvider() {
    }
}
