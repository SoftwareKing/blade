package com.blade.kit;

import com.blade.ioc.*;
import com.blade.ioc.annotation.InjectWith;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author biezhi
 *         2017/5/31
 */
public class ReflectKit {

    private static final List EMPTY_LIST = new ArrayList(0);

    public static <T> T newInstance(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get @Inject Annotated field
     *
     * @param ioc         ioc container
     * @param classDefine classDefine
     * @return return FieldInjector
     */
    public static List<FieldInjector> getInjectFields(Ioc ioc, ClassDefine classDefine) {
        List<FieldInjector> injectors = new ArrayList<>(8);
        for (Field field : classDefine.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                InjectWith with = annotation.annotationType().getAnnotation(InjectWith.class);
                if (with != null) {
                    injectors.add(new FieldInjector(ioc, field));
                }
            }
        }
        if (injectors.size() == 0) {
            return EMPTY_LIST;
        }
        return injectors;
    }

    public static void injection(Ioc ioc, Class<?> type) {
        BeanDefine beanDefine = ioc.getBeanDefine(type);
        ClassDefine classDefine = ClassDefine.create(type);
        List<FieldInjector> fieldInjectors = getInjectFields(ioc, classDefine);
        Object bean = beanDefine.getBean();
        for (FieldInjector fieldInjector : fieldInjectors) {
            fieldInjector.injection(bean);
        }
    }

    public static void injection(Ioc ioc, BeanDefine beanDefine) {
        ClassDefine classDefine = ClassDefine.create(beanDefine.getType());
        List<FieldInjector> fieldInjectors = getInjectFields(ioc, classDefine);
        Object bean = beanDefine.getBean();
        for (FieldInjector fieldInjector : fieldInjectors) {
            fieldInjector.injection(bean);
        }
    }

    private static final List<Class> primitiveTypes = Arrays.asList(int.class, Integer.class, long.class, Long.class,
            boolean.class, Boolean.class, float.class, Float.class, double.class, Double.class, byte.class, Byte.class, short.class, Short.class,
            String.class);

    /**
     * 是否是基本数据类型
     *
     * @param type
     * @return
     */
    public static boolean isBasicType(Class<?> type) {
        return primitiveTypes.contains(type);
    }

    public static Object convert(Class<?> type, String value) {
        if (type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == String.class) {
            return value;
        } else if (type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == Long.class) {
            return Long.parseLong(value);
        } else if (type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == Short.class) {
            return Short.parseShort(value);
        }
        return value;
    }

    /**
     * @param bean   类实例
     * @param method 方法名称
     * @param args   方法参数
     * @return
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static Object invokeMehod(Object bean, Method method, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] types = method.getParameterTypes();
        int argCount = args == null ? 0 : args.length;
        // 参数个数对不上
//        ExceptionKit.makeRunTimeWhen(argCount != types.length, "%s in %s", method.getName(), bean);

        // 转参数类型
        for (int i = 0; i < argCount; i++) {
            args[i] = cast(args[i], types[i]);
        }
        return method.invoke(bean, args);
    }

    public static <T> T cast(Object value, Class<T> type) {
        if (value != null && !type.isAssignableFrom(value.getClass())) {
            if (is(type, int.class, Integer.class)) {
                value = Integer.parseInt(String.valueOf(value));
            } else if (is(type, long.class, Long.class)) {
                value = Long.parseLong(String.valueOf(value));
            } else if (is(type, float.class, Float.class)) {
                value = Float.parseFloat(String.valueOf(value));
            } else if (is(type, double.class, Double.class)) {
                value = Double.parseDouble(String.valueOf(value));
            } else if (is(type, boolean.class, Boolean.class)) {
                value = Boolean.parseBoolean(String.valueOf(value));
            } else if (is(type, String.class)) {
                value = String.valueOf(value);
            }
        }
        return (T) value;
    }

    /**
     * 对象是否其中一个
     */
    public static boolean is(Object obj, Object... mybe) {
        if (obj != null && mybe != null) {
            for (Object mb : mybe)
                if (obj.equals(mb))
                    return true;
        }
        return false;
    }

    public static boolean hasInterface(Class<?> cls, Class<?> inter) {
        return Stream.of(cls.getInterfaces()).filter(c -> c.equals(inter)).count() > 0;
    }

    public static boolean isNormalClass(Class<?> cls) {
        return !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers());
    }

    public static Method getMethod(Class<?> cls, String methodName, Class<?>... types) {
        try {
            return cls.getMethod(methodName, types);
        } catch (Exception e) {
            return null;
        }
    }

}
