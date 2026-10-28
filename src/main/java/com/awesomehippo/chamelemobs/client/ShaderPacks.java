package com.awesomehippo.chamelemobs.client;

import java.lang.reflect.Method;

public class ShaderPacks {
    private static final Object API;
    private static final Method IN_USE;
    private static final Method SHADOW;

    static {
        Object api = null;
        Method inUse = null;
        Method shadow = null;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            inUse = type.getMethod("isShaderPackInUse");
            try {
                shadow = type.getMethod("isRenderingShadowPass");
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        API = api;
        IN_USE = inUse;
        SHADOW = shadow;
    }

    public static boolean active() {
        return invoke(IN_USE);
    }

    public static boolean shadowPass() {
        return invoke(SHADOW);
    }

    private static boolean invoke(Method method) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(API));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
