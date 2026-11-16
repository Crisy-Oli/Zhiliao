package com.shatyuka.zhiliao.hooks;

import android.view.View;

import com.shatyuka.zhiliao.Helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class NavButton implements IHook {
    static Method addMenuTab;
    static Method getItemId;
    static Field tabView;

    @Override
    public String getName() {
        return "隐藏导航栏按钮";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)
                || !(Helper.prefs.getBoolean("switch_vipnav", false)
                    || Helper.prefs.getBoolean("switch_videonav", false)
                    || Helper.prefs.getBoolean("switch_friendnav", false)
                    || Helper.prefs.getBoolean("switch_panelnav", false)
                    || Helper.prefs.getBoolean("switch_findnav", false)))
            return;

        Class<?> navView = classLoader.loadClass("com.zhihu.android.bottomnav.core.BottomNavMenuView");
        Class<?> tabClass = classLoader.loadClass("com.google.android.material.tabs.TabLayout$Tab");

        // There are two methods taking the same menu-item interface in 11.16.0:
        // one returns Tab, the other is a void UI-test helper. Select by return type.
        for (Method method : navView.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (method.getReturnType() == tabClass && params.length == 1) {
                addMenuTab = method;
                break;
            }
        }
        if (addMenuTab == null)
            throw new NoSuchMethodException("BottomNavMenuView.addMenuTab");

        Class<?> itemType = addMenuTab.getParameterTypes()[0];
        // The ID getter is inherited from a parent menu-item interface in 11.16.0.
        // getDeclaredMethods() misses it; getMethods() sees inherited methods.
        for (Method method : itemType.getMethods()) {
            if (method.getReturnType() == String.class
                    && method.getParameterCount() == 0
                    && (getItemId == null || method.getName().equals("a"))) {
                getItemId = method;
            }
        }
        if (getItemId == null)
            throw new NoSuchMethodException("MenuItem.id");
        tabView = tabClass.getField("view");
    }

    @Override
    public void hook() throws Throwable {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false))
            return;

        if (!(Helper.prefs.getBoolean("switch_vipnav", false)
                || Helper.prefs.getBoolean("switch_videonav", false)
                || Helper.prefs.getBoolean("switch_friendnav", false)
                || Helper.prefs.getBoolean("switch_panelnav", false)
                || Helper.prefs.getBoolean("switch_findnav", false)))
            return;

        XposedBridge.hookMethod(addMenuTab, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] == null || param.getResult() == null)
                    return;
                String id = (String) getItemId.invoke(param.args[0]);
                if (id == null)
                    return;
                boolean hide = ("market".equals(id) && Helper.prefs.getBoolean("switch_vipnav", false))
                        || ("video".equals(id) && Helper.prefs.getBoolean("switch_videonav", false))
                        || ("friend".equals(id) && Helper.prefs.getBoolean("switch_friendnav", false))
                        || ("panel".equals(id) && Helper.prefs.getBoolean("switch_panelnav", false))
                        || ("find".equals(id) && Helper.prefs.getBoolean("switch_findnav", false));
                if (hide) {
                    View view = (View) tabView.get(param.getResult());
                    if (view != null)
                        view.setVisibility(View.GONE);
                }
            }
        });
    }
}
