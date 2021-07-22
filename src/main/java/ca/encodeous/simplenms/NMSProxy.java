package ca.encodeous.simplenms;

import ca.encodeous.simplenms.proxy.NMSCore;
import ca.encodeous.simplenms.proxy.NMSProvider;

/**
 * @author theminecoder
 */
public interface NMSProxy {

    /**
     * @return An static only compatible version of the proxy.
     */
    NMSProxy getStaticProxyObject();

    /**
     * @return The original object used to create the proxy
     */
    Object getProxyHandle();

    boolean isProxyStatic();
}
