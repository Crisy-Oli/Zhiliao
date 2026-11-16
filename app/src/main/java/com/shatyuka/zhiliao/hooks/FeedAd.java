package com.shatyuka.zhiliao.hooks;

import android.content.Context;

import com.shatyuka.zhiliao.Helper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FeedAd implements IHook {
    static Class<?> BasePagingFragment;
    static Class<?> FeedAdvert;
    static Class<?> ListAd;
    static Class<?> Advert;
    static Class<?> Ad;

    static Field FeedList_data;

    @Override
    public String getName() {
        return "去信息流广告";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        BasePagingFragment = classLoader.loadClass("com.zhihu.android.app.ui.fragment.paging.BasePagingFragment");
        try {
            FeedAdvert = classLoader.loadClass("com.zhihu.android.api.model.FeedAdvert");
            ListAd = classLoader.loadClass("com.zhihu.android.api.model.ListAd");
            Advert = classLoader.loadClass("com.zhihu.android.api.model.Advert");
            Ad = classLoader.loadClass("com.zhihu.android.api.model.Ad");
        } catch (ClassNotFoundException e) {
            FeedAdvert = classLoader.loadClass("com.zhihu.android.adbase.model.FeedAdvert");
            ListAd = classLoader.loadClass("com.zhihu.android.adbase.model.ListAd");
            Advert = classLoader.loadClass("com.zhihu.android.adbase.model.Advert");
            Ad = classLoader.loadClass("com.zhihu.android.adbase.model.Ad");
        }

        FeedList_data = classLoader.loadClass("com.zhihu.android.api.model.FeedList").getField("data");
    }

    private static List<?> withoutFeedAds(List<?> original) {
        if (original == null || original.isEmpty())
            return original;

        ArrayList<Object> clean = null;
        for (int i = 0; i < original.size(); i++) {
            Object item = original.get(i);
            if (item != null && FeedAdvert.isInstance(item)) {
                if (clean == null)
                    clean = new ArrayList<>(original.subList(0, i));
            } else if (clean != null) {
                clean.add(item);
            }
        }
        return clean == null ? original : clean;
    }

    @Override
    public void hook() throws Throwable {
        XposedBridge.hookAllMethods(BasePagingFragment, "postRefreshSucceed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                    if (param.args[0] == null)
                        return;
                    Object raw = FeedList_data.get(param.args[0]);
                    if (!(raw instanceof List))
                        return;
                    List<?> original = (List<?>) raw;
                    List<?> clean = withoutFeedAds(original);
                    if (clean != original)
                        FeedList_data.set(param.args[0], clean);
                }
            }
        });
        XposedHelpers.findAndHookMethod(BasePagingFragment, "insertDataRangeToList", int.class, List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                    if (param.args[1] == null)
                        return;
                    if (!(param.args[1] instanceof List))
                        return;
                    List<?> original = (List<?>) param.args[1];
                    List<?> clean = withoutFeedAds(original);
                    if (clean != original)
                        param.args[1] = clean;
                }
            }
        });
        // Zhihu 11.16.0 routes feed placements through these newer resolvers.
        // Keep the legacy resolver below as a fallback for older versions.
        try {
            XposedHelpers.findAndHookMethod(Helper.MorphAdHelper, "resolveCommonAdParam",
                    Context.class, FeedAdvert, boolean.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (Helper.prefs.getBoolean("switch_mainswitch", false)
                                    && Helper.prefs.getBoolean("switch_feedad", true)) {
                                param.setResult(false);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
            // Older Zhihu releases do not have this resolver.
        }
        try {
            XposedHelpers.findAndHookMethod(Helper.MorphAdHelper, "resolveHotAdParam",
                    Context.class, FeedAdvert, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (Helper.prefs.getBoolean("switch_mainswitch", false)
                                    && Helper.prefs.getBoolean("switch_feedad", true)) {
                                param.setResult(false);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
            // Legacy releases have different hot-ad routing.
        }

        try {
            XposedHelpers.findAndHookMethod(Helper.MorphAdHelper, "resolve", Context.class, FeedAdvert, boolean.class, Boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                        param.setResult(false);
                    }
                }
            });
        } catch (NoSuchMethodError ignore) {
        }
        try {
            XposedHelpers.findAndHookMethod(Helper.MorphAdHelper, "resolve", Context.class, ListAd, Boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                        param.setResult(false);
                    }
                }
            });
        } catch (NoSuchMethodError ignore) {
        }
        XposedHelpers.findAndHookMethod(Advert, "isSlidingWindow", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                    param.setResult(false);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Ad, "isFloatAdCard", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_feedad", true)) {
                    param.setResult(false);
                }
            }
        });
    }
}
