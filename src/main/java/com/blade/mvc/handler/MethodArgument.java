package com.blade.mvc.handler;

import com.blade.BladeException;
import com.blade.kit.MethodParamNamesKit;
import com.blade.kit.ReflectKit;
import com.blade.kit.StringKit;
import com.blade.mvc.annotation.*;
import com.blade.mvc.hook.Invoker;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.http.Session;
import com.blade.mvc.multipart.FileItem;
import com.blade.mvc.ui.ModelAndView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

public final class MethodArgument {

    public static Object[] getArgs(Request request, Response response, Method actionMethod) throws Exception {

        actionMethod.setAccessible(true);

        Parameter[] parameters = actionMethod.getParameters();
        Object[] args = new Object[parameters.length];
        String[] paramaterNames = MethodParamNamesKit.getParamNames(actionMethod).toArray(new String[MethodParamNamesKit.getParamNames(actionMethod).size()]);// AsmKit.getMethodParamNames(actionMethod);

        for (int i = 0, len = parameters.length; i < len; i++) {
            Parameter parameter = parameters[i];
            String paramName = paramaterNames[i];
            int annoLen = parameter.getAnnotations().length;
            Class<?> argType = parameter.getType();
            if (annoLen > 0) {
                QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
                if (null != queryParam) {
                    args[i] = getQueryParam(argType, queryParam, paramName, request);
                }
                PathParam pathParam = parameter.getAnnotation(PathParam.class);
                if (null != pathParam) {
                    args[i] = getPathParam(argType, pathParam, paramName, request);
                    continue;
                }
                HeaderParam headerParam = parameter.getAnnotation(HeaderParam.class);
                if (null != headerParam) {
                    args[i] = getHeader(argType, headerParam, paramName, request);
                    continue;
                }
                // cookie param
                CookieParam cookieParam = parameter.getAnnotation(CookieParam.class);
                if (null != cookieParam) {
                    args[i] = getCookie(argType, cookieParam, paramName, request);
                }
                // form multipart
                MultipartParam multipartParam = parameter.getAnnotation(MultipartParam.class);
                if (null != multipartParam && argType == FileItem.class) {
                    String name = StringKit.isBlank(multipartParam.value()) ? paramName : multipartParam.value();
                    args[i] = request.fileItem(name);
                }
            } else {
                if (ReflectKit.isBasicType(argType)) {
                    args[i] = request.query(paramName);
                } else {
                    if (argType == Invoker.class) {
                        args[i] = new Invoker(request, response);
                        continue;
                    } else if (argType == Request.class) {
                        args[i] = request;
                        continue;
                    } else if (argType == Response.class) {
                        args[i] = response;
                    } else if (argType == Session.class) {
                        args[i] = request.session();
                    } else if (argType == ModelAndView.class) {
                        args[i] = new ModelAndView();
                    } else if (argType == Map.class) {
                        args[i] = request.querys();
                    } else {
                        args[i] = parseModel(argType, request, null);
                    }
                }
            }
        }
        return args;
    }

    private static Object getQueryParam(Class<?> argType, QueryParam queryParam, String paramName, Request request) throws BladeException {
        String name = StringKit.isBlank(queryParam.name()) ? paramName : queryParam.name();

        if (ReflectKit.isBasicType(argType)) {
            Optional<String> val = request.query(name);
            boolean required = queryParam.required();
            if (!val.isPresent()) {
                val = Optional.of(queryParam.defaultValue());
            }
            if (required && !val.isPresent()) {
                throw new BladeException("query param [" + paramName + "] not is empty.");
            }
            return getRequestParam(argType, val.get());
        } else {
            return parseModel(argType, request, name);
        }
    }

    private static Object getCookie(Class<?> argType, CookieParam cookieParam, String paramName, Request request) {
        String cookieName = StringKit.isBlank(cookieParam.value()) ? paramName : cookieParam.value();
        Optional<String> val = request.cookie(cookieName);
        boolean required = cookieParam.required();
        if (!val.isPresent()) {
            val = Optional.of(cookieParam.defaultValue());
        }
        if (required && !val.isPresent()) {
            throw new BladeException("cookie param [" + paramName + "] not is empty.");
        }
        return getRequestParam(argType, val.get());
    }

    private static Object getHeader(Class<?> argType, HeaderParam headerParam, String paramName, Request request) {
        String key = StringKit.isBlank(headerParam.value()) ? paramName : headerParam.value();
        Optional<String> val = request.header(key);
        boolean required = headerParam.required();
        if (!val.isPresent()) {
            val = Optional.of(headerParam.defaultValue());
        }
        if (required && !val.isPresent()) {
            throw new BladeException("header param [" + paramName + "] not is empty.");
        }
        return getRequestParam(argType, val.get());
    }

    private static Object getPathParam(Class<?> argType, PathParam pathParam, String paramName, Request request) {
        String name = StringKit.isBlank(pathParam.name()) ? paramName : pathParam.name();
        Optional<String> val = request.pathString(name);
        if (!val.isPresent()) {
            val = Optional.of(pathParam.defaultValue());
        }
        return getRequestParam(argType, val.get());
    }

    private static Object parseModel(Class<?> argType, Request request, String name) {
        try {
            Field[] fields = argType.getDeclaredFields();
            if (null == fields || fields.length == 0) {
                return null;
            }
            Object obj = ReflectKit.newInstance(argType);
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getName().equals("serialVersionUID")) {
                    continue;
                }
                Optional<String> fieldValue = request.query(field.getName());
                if (null != name) {
                    String fieldName = name + "[" + field.getName() + "]";
                    fieldValue = request.query(fieldName);
                }
                if (fieldValue.isPresent()) {
                    Object value = ReflectKit.convert(field.getType(), fieldValue.get());
                    field.set(obj, value);
                }
            }
            return obj;
        } catch (NumberFormatException | IllegalAccessException | SecurityException e) {
            throw new BladeException(e.getMessage());
        }
    }

    public static Object getRequestParam(Class<?> parameterType, String val) {
        Object result = null;
        if (parameterType.equals(String.class)) {
            return val;
        }
        if (StringKit.isBlank(val)) {
            if (parameterType.equals(int.class) || parameterType.equals(double.class) ||
                    parameterType.equals(long.class) || parameterType.equals(byte.class) || parameterType.equals(float.class)) {
                result = 0;
            }
            if (parameterType.equals(boolean.class)) {
                result = false;
            }
        } else {
            if (parameterType.equals(Integer.class) || parameterType.equals(int.class)) {
                result = Integer.parseInt(val);
            }
            if (parameterType.equals(Long.class) || parameterType.equals(long.class)) {
                result = Long.parseLong(val);
            }
            if (parameterType.equals(Double.class) || parameterType.equals(double.class)) {
                result = Double.parseDouble(val);
            }
            if (parameterType.equals(Float.class) || parameterType.equals(float.class)) {
                result = Float.parseFloat(val);
            }
            if (parameterType.equals(Boolean.class) || parameterType.equals(boolean.class)) {
                result = Boolean.parseBoolean(val);
            }
            if (parameterType.equals(Byte.class) || parameterType.equals(byte.class)) {
                result = Byte.parseByte(val);
            }
        }
        return result;
    }

}