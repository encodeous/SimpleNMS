package ca.encodeous.simplenms.proxy;

import ca.encodeous.simplenms.annotations.NMSField;
import ca.encodeous.simplenms.annotations.NMSMethod;
import ca.encodeous.simplenms.annotations.NMSVersionName;
import ca.encodeous.simplenms.util.ClassUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import ca.encodeous.simplenms.NMSProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author theminecoder
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NMSProxyInvocationMapper {

    private final BiMap<Class, Class> proxyToNMSClassMap;
    private final Map<Class, Map<Method, Method>> proxyToNmsMethodMap = Maps.newConcurrentMap();
    private final Map<Class, Map<Method, Field>> proxyToNMSFieldMap = Maps.newConcurrentMap();
    private final Table<Class, Class[], Constructor> proxyToNMSConstructorMap = HashBasedTable.create();

    NMSProxyInvocationMapper(BiMap<Class, Class> proxyToNMSClassMap) {
        this.proxyToNMSClassMap = proxyToNMSClassMap;
    }

    public Method findNMSMethod(Class<? extends NMSProxy> proxyClass, Method proxyMethod, NMSMethod nmsMethodAnnotation, Class[] fixedArgTypes) throws NoSuchMethodException {
        Map<Method, Method> methodMap = proxyToNmsMethodMap.computeIfAbsent(proxyClass, key -> Maps.newConcurrentMap());
        Method nmsMethod = methodMap.get(proxyMethod);
        if (nmsMethod == null) {
            String methodName = proxyMethod.getName();

            if (proxyMethod.getDeclaringClass() != Object.class) {
                for (NMSVersionName methodVersion : nmsMethodAnnotation.versionNames()) {
                    if (methodVersion.version().equalsIgnoreCase(NMSProvider.NMS_VERSION)) {
                        methodName = methodVersion.name();
                        break;
                    }
                }
            }

            final Class nmsClass = proxyToNMSClassMap.get(proxyClass);

            AtomicReference<Method> methodSearchRef = new AtomicReference<>();
            String finalMethodName = methodName;
            ClassUtil.forEachClassPossibility(fixedArgTypes, (searchTypes) -> {
                Class searchClass = nmsClass;
                do {
                    try {
                        methodSearchRef.set(searchClass.getDeclaredMethod(finalMethodName, searchTypes));
                        return true;
                    } catch (NoSuchMethodException ignored) {
                        searchClass = searchClass.getSuperclass();
                    }
                } while (methodSearchRef.get() == null && searchClass != null);
                return false;
            });

            if (methodSearchRef.get() == null) {
                throw new NoSuchMethodException(nmsClass.getCanonicalName() + "." + methodName);
            }

            nmsMethod = methodSearchRef.get();
            if (!nmsMethod.isAccessible()) {
                nmsMethod.setAccessible(true);
            }

            methodMap.put(proxyMethod, nmsMethod);
        }
        return nmsMethod;
    }

    public Field findNMSField(Class<? extends NMSProxy> proxyClass, Method proxyMethod, NMSField nmsFieldAnnotation) throws NoSuchFieldException, IllegalAccessException {
        Map<Method, Field> fieldMap = proxyToNMSFieldMap.computeIfAbsent(proxyClass, key -> Maps.newConcurrentMap());

        Field field = fieldMap.get(proxyMethod);
        if (field == null) {
            String fieldName = proxyMethod.getName();

            for (NMSVersionName versionName : nmsFieldAnnotation.versionNames()) {
                if (versionName.version().equalsIgnoreCase(NMSProvider.NMS_VERSION)) {
                    fieldName = versionName.name();
                    break;
                }
            }

            final Class nmsClass = proxyToNMSClassMap.get(proxyClass);
            Class searchClass = nmsClass;
            do {
                try {
                    field = searchClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    searchClass = searchClass.getSuperclass();
                }
            } while (field != null && searchClass == Object.class);

            if (field == null) {
                throw new NoSuchFieldException(nmsClass.getCanonicalName() + "#" + fieldName);
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            if (Modifier.isFinal(field.getModifiers())) {
                Field modifierField = Field.class.getDeclaredField("modifiers");
                modifierField.setAccessible(true);
                modifierField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }

            fieldMap.put(proxyMethod, field);
        }
        return field;
    }

    public Constructor findNMSConstructor(Class<? extends NMSProxy> proxyClass, Class[] fixedArgTypes) throws NoSuchMethodException {
        final Class nmsClass = proxyToNMSClassMap.get(proxyClass);

        Constructor constructor = proxyToNMSConstructorMap.get(nmsClass, fixedArgTypes);

        if (constructor == null) {
            AtomicReference<Constructor> constructerSearchRef = new AtomicReference<>();
            ClassUtil.forEachClassPossibility(fixedArgTypes, (searchTypes) -> {
                try {
                    constructerSearchRef.set(nmsClass.getDeclaredConstructor(searchTypes));
                    return true;
                } catch (NoSuchMethodException ignored) {
                }
                return false;
            });

            if (constructerSearchRef.get() == null) {
                throw new NoSuchMethodException(nmsClass.getCanonicalName() + ".<init>");
            }

            constructor = constructerSearchRef.get();
            proxyToNMSConstructorMap.put(nmsClass, fixedArgTypes, constructor);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
        }

        return constructor;
    }


}
