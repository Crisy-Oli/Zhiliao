package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Helper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class FeedTopHotBanner implements IHook {
    private static Class<?> deserializer;

    @Override
    public String getName() {
        return "隐藏推荐页置顶热门";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)
                || !Helper.prefs.getBoolean("switch_feedtophot", false))
            return;
        deserializer = classLoader.loadClass(
                "com.zhihu.android.api.model.FeedTopHotAutoJacksonDeserializer");
    }

    @Override
    public void hook() throws Throwable {
        if (deserializer == null)
            return;

        // 11.16.0 inherits deserialize() from BaseObjectStdDeserializer;
        // hookAllMethods on the subclass silently hooked nothing.
        XposedBridge.hookAllMethods(deserializer.getSuperclass(), "deserialize",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (deserializer.isInstance(param.thisObject)
                                && Helper.prefs.getBoolean("switch_mainswitch", false)
                                && Helper.prefs.getBoolean("switch_feedtophot", false)) {
                            param.setResult(null);
                        }
                    }
                });
    }
}
