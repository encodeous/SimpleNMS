package ca.encodeous.simplenms.proxy;

import ca.encodeous.simplenms.NMSProxy;
import ca.encodeous.simplenms.annotations.NMSClass;
import ca.encodeous.simplenms.util.NullReference;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

public class NMSCore {
    public static Class<?> getClass(String... names) {
        for(int i = 0; i<names.length; i++) {
            try {
                return Class.forName(names[i]);
            } catch (ClassNotFoundException ignored) {}
        }
        new ClassNotFoundException(Arrays.toString(names)).printStackTrace();
        return null;
    }
    public static void registerNMSClasses(Class<? extends NMSProxy> clazz) {
        if (NMSProvider.proxyToNMSClassMap.containsKey(clazz)) {
            return;
        }

        NMSClass nmsClassAnnotation = clazz.getAnnotation(NMSClass.class);
        if (nmsClassAnnotation == null) {
            throw new IllegalStateException("NMSProxy interfaces must have a valid @NMSClass annotation");
        }

        String className = nmsClassAnnotation.value();

        Class nmsClass;
        nmsClass = getClass(nmsClassAnnotation.type().getClassNames(className)); //TODO Move %version% replacement here
        if(nmsClass == null){
            throw new IllegalStateException("Class proxy "+ clazz.getName() +" for " + className + " (" + nmsClassAnnotation.type() + ") was not found!");
        }

        NMSProvider.proxyToNMSClassMap.put(clazz, nmsClass);
    }

    /**
     * Checks if the passed NMS object is an instance of the passed class
     *
     * @param object Object to check
     * @param clazz  Class to check
     */
    public static boolean isInstanceOf(Object object, Class<? extends NMSProxy> clazz) {
        registerNMSClasses(clazz);

        if (object instanceof NMSProxy) {
            object = ((NMSProxy) object).getProxyHandle();
        }

        return NMSProvider.proxyToNMSClassMap.get(clazz).isAssignableFrom(object.getClass());
    }

    /**
     * Generates a static only proxy to an NMS class
     *
     * @param clazz {@link NMSClass} annotated {@link NMSProxy} interface.
     * @return Generated Proxy
     */
    public static <T extends NMSProxy> T getStaticNMSObject(Class<T> clazz) {
        registerNMSClasses(clazz);

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new NMSProxyInvocationHandler(null, NMSProvider.invocationMapper));
    }

    /**
     * Generates a proxy to an NMS class instance
     *
     * @param clazz  {@link NMSClass} annotated {@link NMSProxy} interface.
     * @param object Object to proxy
     * @return Generated Proxy
     */
    public static <T extends NMSProxy> T getNMSObject(Class<T> clazz, Object object) {
        registerNMSClasses(clazz);

        if (!NMSProvider.proxyToNMSClassMap.get(clazz).isAssignableFrom(object.getClass())) {
            throw new IllegalStateException("Object is not of type " + NMSProvider.proxyToNMSClassMap.get(clazz).getCanonicalName() + "!");
        }

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new NMSProxyInvocationHandler(object, NMSProvider.invocationMapper));
    }

    /**
     * Constructs and returns a NMS object wrapped in a proxy.
     *
     * @param clazz  {@link NMSClass} annotated {@link NMSProxy} interface class
     * @param params Objects to pass to the constructor (NMSProxy instances will be converted to their actual objects for you).
     *               Use of null must be modified to use a {@link NullReference} object instead, so the type of null is known.
     * @return The constructed NMS object wrapped in a proxy.
     * @throws ReflectiveOperationException
     */
    public static <T extends NMSProxy> T constructNMSObject(Class<T> clazz, Object... params) throws ReflectiveOperationException {
        registerNMSClasses(clazz);

        NullReference[] nullReferences = new NullReference[params.length];

        //pull out null references and swap them to null
        for (int i = 0; i < params.length; i++) {
            if(params[i] == null) throw new IllegalArgumentException("null argument is not supported directly. Use a NullReference instead.");
            if(params[i] instanceof NullReference) {
                nullReferences[i] = (NullReference) params[i];
                params[i] = null;
            }
        }

        Object[] fixedArgs = unwrapArguments(params);
        Class[] fixedArgTypes = Arrays.stream(fixedArgs).map(arg -> arg != null ? arg.getClass() : null).toArray(Class[]::new);

        for (int i = 0; i < nullReferences.length; i++) {
            if(nullReferences[i] != null) {
                Class type = nullReferences[i].getType();
                if (NMSProxy.class.isAssignableFrom(type)) {
                    registerNMSClasses(type);
                    type = NMSProvider.proxyToNMSClassMap.get(type);
                }
                fixedArgTypes[i] = type;
            }
        }

        Object nmsObject = NMSProvider.invocationMapper.findNMSConstructor(clazz, fixedArgTypes).newInstance(fixedArgs);

        return getNMSObject(clazz, nmsObject);
    }
    public static Object[] unwrapArguments(Object[] args) {
        if (args == null) {
            return new Object[]{};
        }

        ArrayList<Object> results = new ArrayList<>();
        for(Object arg : args){
            results.add(unwrapArgument(arg));
        }
        return results.toArray();
    }

    public static Object unwrapArgument(Object arg) {
        if (arg == null) {
            return null;
        }

        if (arg instanceof NMSProxy) {
            registerNMSClasses((Class<? extends NMSProxy>) arg.getClass().getInterfaces()[0]);
            return ((NMSProxy) arg).getProxyHandle();
        }

        return arg;
    }
}
