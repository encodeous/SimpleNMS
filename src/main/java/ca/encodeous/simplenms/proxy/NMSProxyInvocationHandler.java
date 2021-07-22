package ca.encodeous.simplenms.proxy;

import ca.encodeous.simplenms.annotations.NMSField;
import ca.encodeous.simplenms.annotations.NMSMethod;
import ca.encodeous.simplenms.NMSProxy;
import ca.encodeous.simplenms.annotations.NMSStatic;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author theminecoder
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NMSProxyInvocationHandler implements InvocationHandler {

    private static Constructor<MethodHandles.Lookup> methodLookupConstructor;

    private final Object handle;
    private final NMSProxyInvocationMapper invocationMapper;

    NMSProxyInvocationHandler(Object handle, NMSProxyInvocationMapper invocationMapper) {
        this.handle = handle;
        this.invocationMapper = invocationMapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getProxyHandle")) {
            return handle;
        }

        if (method.getName().equals("getStaticProxyObject")) {
            if (handle == null) {
                return this;
            }

            return NMSCore.getStaticNMSObject((Class<? extends NMSProxy>) proxy.getClass().getInterfaces()[0]);
        }

        if(method.getName().equals("isProxyStatic")) {
            return handle == null;
        }

        if (method.isDefault()) {
            final Class<?> declaringClass = method.getDeclaringClass();
            boolean extraClass = false;
            if (methodLookupConstructor == null) {
                try {
                    // idk when they changed this but just in case
                    //noinspection JavaReflectionMemberAccess
                    methodLookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                } catch (NoSuchMethodException e) {
                    methodLookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
                    extraClass = true;
                }
                methodLookupConstructor.setAccessible(true);
            }
            return (extraClass ?
                    methodLookupConstructor.newInstance(declaringClass, null, -1) :
                    methodLookupConstructor.newInstance(declaringClass, -1)) //Trusted Flag
                    .unreflectSpecial(method, declaringClass)
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        }

        if (handle == null && method.getAnnotation(NMSStatic.class) == null) {
            throw new IllegalStateException("Proxy method \"" + method + "\" is attempting to call to instance method/field on a static proxy. Please mark the proxy method with @NMSStatic");
        }

        if (method.getAnnotation(NMSField.class) == null || method.getDeclaringClass() == Object.class) {
            NMSMethod nmsMethodAnnotation = method.getAnnotation(NMSMethod.class);

            Object[] fixedArgs = NMSCore.unwrapArguments(args);
            Class[] fixedArgTypes = Arrays.stream(fixedArgs).map(Object::getClass).toArray(Class[]::new);

            Method nmsMethod = invocationMapper.findNMSMethod((Class<? extends NMSProxy>) proxy.getClass().getInterfaces()[0], method, nmsMethodAnnotation, fixedArgTypes);

            Object invokerObject = method.getAnnotation(NMSStatic.class) != null ? null : handle;
            Object returnObject = nmsMethod.invoke(invokerObject, fixedArgs);

            if (returnObject == null) {
                return null;
            }

            if (method.getName().equals("toString") && method.getParameterCount() == 0) {
                return "Proxy|" + proxy.getClass().getInterfaces()[0].getCanonicalName() + "(" + returnObject + ")";
            }

            if (NMSProxy.class.isAssignableFrom(method.getReturnType())) {
                returnObject = NMSCore.getNMSObject((Class<? extends NMSProxy>) method.getReturnType(), returnObject);
            }

            return returnObject;
        } else {
            NMSField fieldAnnotation = method.getAnnotation(NMSField.class);

            Field field = invocationMapper.findNMSField((Class<? extends NMSProxy>) proxy.getClass().getInterfaces()[0], method, fieldAnnotation);

            Object invokerObject = method.getAnnotation(NMSStatic.class) != null ? null : handle;
            if (fieldAnnotation.value() == NMSField.Type.GETTER) {
                if (args != null && args.length != 0) {
                    throw new IllegalArgumentException("Must have 0 arguments on proxy method!");
                }

                Object value = field.get(invokerObject);
                if (NMSProxy.class.isAssignableFrom(method.getReturnType())) {
                    value = NMSCore.getNMSObject((Class<? extends NMSProxy>) method.getReturnType(), value);
                }
                return value;
            } else {
                if (args == null || args.length != 1) {
                    throw new IllegalArgumentException("Must only pass the new value to set!");
                }

                field.set(invokerObject, NMSCore.unwrapArgument(args[0]));
                return null;
            }
        }
    }
}
