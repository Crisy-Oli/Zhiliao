package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Helper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class FullScreen implements IHook {
    static Class<?> ClearScreenHelper_lambda;

    @Override
    public String getName() {
        return "禁止进入全屏模式";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        try {
            ClearScreenHelper_lambda = classLoader.loadClass("com.zhihu.android.feature.short_container_feature.ui.widget.toolbar.clearscreen.d$c");
        } catch (ClassNotFoundException ignored) {
            // Zhihu 11.16.0: c$c invokes the enter-clear-screen path c.d().
            // Do not block c$d: it invokes c.e() and restores the UI.
            try {
                ClearScreenHelper_lambda = classLoader.loadClass(
                        "com.zhihu.android.feature.short_container_feature.ui.widget.toolbar.clearscreen.c$c");
            } catch (ClassNotFoundException ignoredNew) {
            }
        }
    }

    @Override
    public void hook() throws Throwable {
        if (ClearScreenHelper_lambda != null) {
            XC_MethodHook preventEnter = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Helper.prefs != null && Helper.prefs.getBoolean("switch_mainswitch", false)
                            && Helper.prefs.getBoolean("switch_fullscreen", false)) {
                        param.setResult(null);
                    }
                }
            };
            try {
                // The Kotlin lambda's a() is void; intercept it rather than
                // returning null from invoke() which normally yields Unit.INSTANCE.
                XposedHelpers.findAndHookMethod(ClearScreenHelper_lambda, "a", preventEnter);
            } catch (NoSuchMethodError ignored) {
                XposedHelpers.findAndHookMethod(ClearScreenHelper_lambda, "invoke", preventEnter);
            }
        }
    }
}
