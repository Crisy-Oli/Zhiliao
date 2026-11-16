package com.shatyuka.zhiliao.hooks;

import android.content.Context;

import com.shatyuka.zhiliao.Helper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class CommentAd implements IHook {
    private static Class<?> commentListAd;
    private static Class<?> commentList;
    private static Field commentData;

    @Override
    public String getName() {
        return "去评论广告";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        try {
            commentListAd = classLoader.loadClass("com.zhihu.android.api.model.CommentListAd");
        } catch (ClassNotFoundException ignored) {
            commentListAd = classLoader.loadClass("com.zhihu.android.adbase.model.CommentListAd");
        }

        // Modern versions no longer expose MorphAdHelper.resolveCommentAdParam.
        // Strip ad entries after JSON conversion rather than depending on that method.
        commentList = classLoader.loadClass("com.zhihu.android.api.model.CommentList");
        commentData = commentList.getField("data");
    }

    private static boolean enabled() {
        return Helper.prefs != null
                && Helper.prefs.getBoolean("switch_mainswitch", false)
                && Helper.prefs.getBoolean("switch_commentad", true);
    }

    private static void filterCommentList(Object value) throws IllegalAccessException {
        if (!enabled() || value == null || !commentList.isInstance(value))
            return;

        Object raw = commentData.get(value);
        if (!(raw instanceof List))
            return;

        List<?> data = (List<?>) raw;
        ArrayList<Object> filtered = null;
        for (int i = 0; i < data.size(); i++) {
            Object item = data.get(i);
            if (item != null && commentListAd.isInstance(item)) {
                if (filtered == null)
                    filtered = new ArrayList<>(data.subList(0, i));
            } else if (filtered != null) {
                filtered.add(item);
            }
        }
        // Do not mutate a potentially immutable deserialized list in-place.
        if (filtered != null)
            commentData.set(value, filtered);
    }

    @Override
    public void hook() throws Throwable {
        // Retain compatibility with versions that still have the resolver.
        try {
            XposedHelpers.findAndHookMethod(Helper.MorphAdHelper, "resolveCommentAdParam",
                    Context.class, commentListAd, Boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (enabled())
                                param.setResult(false);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
            // Zhihu 11.16.0: method removed.
        }

        XC_MethodHook strip = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    filterCommentList(param.getResult());
                } catch (IllegalAccessException e) {
                    XposedBridge.log("[Zhiliao] CommentAd list filter: " + e);
                }
            }
        };

        // The JSON converter can emit CommentList before it is passed to a view.
        // Hook only known converter implementations; no broad runtime class scans.
        String[] converters = {
                "com.zhihu.android.net.c.b",
                "retrofit2.b.a.c",
                "com.zhihu.android.net.b.b",
                "retrofit2.p.a.c"
        };
        for (String className : converters) {
            try {
                Class<?> converter = Helper.context.getClassLoader().loadClass(className);
                XposedHelpers.findAndHookMethod(converter, "convert", Object.class, strip);
            } catch (ClassNotFoundException | NoSuchMethodError ignored) {
                // Converter names differ across releases.
            }
        }
    }
}
