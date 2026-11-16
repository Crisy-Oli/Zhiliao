package com.shatyuka.zhiliao.hooks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.shatyuka.zhiliao.Helper;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class ExternLink implements IHook {
    static Class<?> H5Event;

    static Method shouldOverrideUrlLoading;
    static Method openUrl;
    static Method openUrl2;

    static Field H5Event_params;

    @Override
    public String getName() {
        return "直接打开外部链接";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        shouldOverrideUrlLoading = Helper.WebViewClientWrapper.getMethod("shouldOverrideUrlLoading", WebView.class, WebResourceRequest.class);

        try {
            H5Event = classLoader.loadClass("com.zhihu.android.app.mercury.api.a");
        } catch (ClassNotFoundException e) {
            H5Event = classLoader.loadClass("com.zhihu.android.app.mercury.a.a");
        }

        Class<?> BasePlugin2 = classLoader.loadClass("com.zhihu.android.app.mercury.plugin.BasePlugin2");
        openUrl = BasePlugin2.getMethod("openUrl", H5Event);

        try {
            Class<?> IntentUtils = classLoader.loadClass("com.zhihu.android.app.router.IntentUtils");
            openUrl2 = IntentUtils.getMethod("openUrl", Context.class, Uri.class, boolean.class, boolean.class);
        } catch (Exception e) {
            Class<?> IntentUtils = classLoader.loadClass("com.zhihu.android.app.router.c");
            openUrl2 = IntentUtils.getMethod("a", Context.class, Uri.class, boolean.class, boolean.class);
        }

        H5Event_params = H5Event.getDeclaredField("i");
        H5Event_params.setAccessible(true);
    }

    private static boolean enabled() {
        return Helper.prefs != null
                && Helper.prefs.getBoolean("switch_mainswitch", false)
                && (Helper.prefs.getBoolean("switch_externlink", false)
                    || Helper.prefs.getBoolean("switch_externlinkex", false));
    }

    private static Uri extractTarget(Uri link) {
        if (link == null || !"link.zhihu.com".equalsIgnoreCase(link.getHost()))
            return null;
        String raw = link.getQueryParameter("target");
        if (raw == null || raw.isEmpty())
            return null;
        Uri target = Uri.parse(raw);
        String scheme = target.getScheme();
        return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                ? target : null;
    }

    private static boolean openOutside(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Helper.context.startActivity(intent);
            return true;
        } catch (RuntimeException e) {
            XposedBridge.log("[Zhiliao] Cannot open external URL: " + e);
            return false;
        }
    }

    @Override
    public void hook() throws Throwable {
        XposedBridge.hookMethod(shouldOverrideUrlLoading, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled())
                    return;
                WebResourceRequest request = (WebResourceRequest) param.args[1];
                Uri target = extractTarget(request == null ? null : request.getUrl());
                if (target == null)
                    return;
                if (Helper.prefs.getBoolean("switch_externlinkex", false)) {
                    if (openOutside(target))
                        param.setResult(true);
                } else {
                    WebResourceRequestImpl replacement = new WebResourceRequestImpl(request);
                    replacement.url = target;
                    param.args[1] = replacement;
                }
            }
        });

        XposedBridge.hookMethod(openUrl, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!enabled() || param.args[0] == null)
                    return;
                Object raw = H5Event_params.get(param.args[0]);
                if (!(raw instanceof JSONObject))
                    return;
                JSONObject params = (JSONObject) raw;
                Uri target = extractTarget(Uri.parse(params.optString("url", "")));
                if (target == null)
                    return;
                if (Helper.prefs.getBoolean("switch_externlinkex", false)) {
                    if (openOutside(target)) {
                        // BasePlugin2.openUrl(H5Event) returns void, not boolean.
                        param.setResult(null);
                    }
                } else {
                    params.put("url", target.toString());
                }
            }
        });

        XposedBridge.hookMethod(openUrl2, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled())
                    return;
                Uri target = extractTarget((Uri) param.args[1]);
                if (target == null)
                    return;
                if (Helper.prefs.getBoolean("switch_externlinkex", false)) {
                    if (openOutside(target))
                        param.setResult(true);
                } else {
                    param.args[1] = target;
                }
            }
        });
    }

    private static class WebResourceRequestImpl implements WebResourceRequest {
        public Uri url;
        public boolean isMainFrame;
        public boolean isRedirect;
        public boolean hasUserGesture;
        public String method;
        Map<String, String> requestHeaders;

        public WebResourceRequestImpl(final WebResourceRequest request) {
            url = request.getUrl();
            isMainFrame = request.isForMainFrame();
            hasUserGesture = request.hasGesture();
            method = request.getMethod();
            requestHeaders = request.getRequestHeaders();
            isRedirect = request.isRedirect();
        }

        @Override
        public Uri getUrl() {
            return url;
        }

        @Override
        public boolean isForMainFrame() {
            return isMainFrame;
        }

        @Override
        public boolean isRedirect() {
            return isRedirect;
        }

        @Override
        public boolean hasGesture() {
            return hasUserGesture;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public Map<String, String> getRequestHeaders() {
            return requestHeaders;
        }
    }
}
