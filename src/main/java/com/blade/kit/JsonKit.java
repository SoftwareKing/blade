package com.blade.kit;

import com.blade.kit.ason.Ason;

/**
 * @author biezhi
 *         2017/6/2
 */
public final class JsonKit {

    private JsonKit() {
    }

    public static String toString(Object object) {
        return Ason.serialize(object).toString();
    }

    public static String toString(Object object, int spaces) {
        return Ason.serialize(object).toString(spaces);
    }

    public static <T> T formJson(String json, Class<T> cls) {
        return Ason.deserialize(json, cls);
    }

}
