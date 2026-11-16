package com.shatyuka.zhiliao.hooks;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.shatyuka.zhiliao.Helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class AnswerAd implements IHook {
    static Method shouldInterceptRequest;

    @Override
    public String getName() {
        return "去回答底部广告";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        Class<?> AppView = classLoader.loadClass("com.zhihu.android.answer.module.content.appview.AnswerAppView").getSuperclass();
        if (AppView == null)
            throw new ClassNotFoundException("com.zhihu.android.appview.AppView");
        shouldInterceptRequest = null;
        Helper.findClass(classLoader, AppView.getName() + "$",
                (Class<?> ZhihuWebViewClient) -> {
                    for (Method method : ZhihuWebViewClient.getDeclaredMethods()) {
                        // The 11.16.0 client has both response-returning and boolean
                        // methods with identical argument types. Only hook the former.
                        if (method.getReturnType() == WebResourceResponse.class &&
                                Arrays.equals(method.getParameterTypes(),
                                        new Class<?>[]{Helper.IZhihuWebView, WebResourceRequest.class})) {
                            shouldInterceptRequest = method;
                            return true;
                        }
                    }
                    return false;
                });
        if (shouldInterceptRequest == null)
            throw new NoSuchMethodException("com.zhihu.android.appview.AppView$ZhihuWebViewClient.shouldInterceptRequest(IZhihuWebView, WebResourceRequest)");
    }

    @Override
    public void hook() throws Throwable {
        XposedBridge.hookMethod(shouldInterceptRequest, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Helper.prefs.getBoolean("switch_mainswitch", false))
                    return;
                WebResourceRequest request = (WebResourceRequest) param.args[1];
                if (request == null || !"GET".equals(request.getMethod()))
                    return;
                List<String> segments = request.getUrl().getPathSegments();
                int count = segments.size();
                if (count == 0)
                    return;

                boolean blockAnswerModules = count > 2 && (
                        (Helper.prefs.getBoolean("switch_answerad", true) &&
                                (segments.get(count - 1).equals("recommendations") ||
                                        (segments.get(2).equals("brand") && segments.get(count - 1).equals("card"))))
                        || segments.get(count - 2).equals("hotmodule")
                        || (Helper.prefs.getBoolean("switch_club", false) && segments.get(count - 1).equals("bind_club"))
                        || (Helper.prefs.getBoolean("switch_goods", false) && segments.get(count - 2).equals("goods"))
                        || (Helper.prefs.getBoolean("switch_article", false) && segments.get(count - 1).equals("recommendation"))
                        || (Helper.prefs.getBoolean("switch_related", false) && segments.get(count - 3).equals("related_queries")));
                boolean blockSearchWords = Helper.prefs.getBoolean("switch_searchwords", false)
                        && segments.get(count - 1).equals("entity_word");

                if (blockAnswerModules || blockSearchWords) {
                    WebResourceResponse response = new WebResourceResponse("application/json", "UTF-8", new ByteArrayInputStream("{}".getBytes()));
                    response.setStatusCodeAndReasonPhrase(200, "OK");
                    param.setResult(response);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Helper.prefs.getBoolean("switch_mainswitch", false))
                    return;
                WebResourceRequest request = (WebResourceRequest) param.args[1];
                if (request == null || !"GET".equals(request.getMethod()))
                    return;
                List<String> segments = request.getUrl().getPathSegments();
                if (segments.size() > 2) {
                    if (Helper.prefs.getBoolean("switch_searchwords", false) && segments.get(0).equals("appview") && segments.get(segments.size() - 2).equals("answer")) {
                        WebResourceResponse response = (WebResourceResponse) param.getResult();
                        if (response == null || response.getData() == null)
                            return;
                        try {
                            InputStream stream = response.getData();
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
                            byte[] chunk = new byte[8192];
                            int length;
                            final int maxBytes = 4 * 1024 * 1024;
                            while ((length = stream.read(chunk)) != -1) {
                                buffer.write(chunk, 0, length);
                                if (buffer.size() > maxBytes) {
                                    // Large responses are left untouched. Replay the
                                    // consumed prefix before the original stream.
                                    response.setData(new SequenceInputStream(
                                            new ByteArrayInputStream(buffer.toByteArray()), stream));
                                    return;
                                }
                            }
                            byte[] original = buffer.toByteArray();
                            String content = new String(original, StandardCharsets.UTF_8);
                            String updated = content.replace("\"searchWords\"", "\"searchWords_bak\"")
                                    .replace("\"search_words\"", "\"search_words_bak\"");
                            response.setData(new ByteArrayInputStream(
                                    content.equals(updated) ? original : updated.getBytes(StandardCharsets.UTF_8)));
                            param.setResult(response);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
    }
}
