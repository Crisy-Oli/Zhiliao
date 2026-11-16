package com.shatyuka.zhiliao.hooks;

import android.graphics.Bitmap;

import com.shatyuka.zhiliao.Helper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class WebView implements IHook {
    static final String script_hide_water_mark = "var styleWaterMark=document.createElement('style');styleWaterMark.innerHTML='.App{background-image:none!important}';document.body.append(styleWaterMark);";

    /**
     * todo: 抽成文件
     */
    static final String script_hide_subscribe =
            "var bar=document.getElementsByClassName('Toolbar-functionButtons')[0];"
            + "var subscribe=bar&&bar.lastElementChild;"
            + "if(subscribe){for(var i=0;i<subscribe.children.length;i++){"
            + "if(subscribe.children[i].classList.contains('Avatar')){"
            + "subscribe.style.setProperty('display','none','important');break;}}}"
            + "var author=document.getElementsByClassName('UserLine AuthorCard')[0];"
            + "var subscribe2=author&&author.lastElementChild;"
            + "if(subscribe2&&subscribe2.tagName==='BUTTON'){"
            + "subscribe2.style.setProperty('display','none','important');}";


    static Class<?> answerAppView;

    @Override
    public String getName() {
        return "WebView修改";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        answerAppView = classLoader.loadClass("com.zhihu.android.answer.module.content.appview.AnswerAppView");
    }

    @Override
    public void hook() throws Throwable {
        if (Helper.prefs.getBoolean("switch_mainswitch", false)
                && Helper.prefs.getBoolean("switch_subscribe", false)) {
            // 禁用, 使用pre render时, js会失效
            XposedBridge.hookAllMethods(answerAppView, "canPreRender", XC_MethodReplacement.returnConstant(false));
        }

        if (Helper.prefs.getBoolean("switch_webview_debug", false)) {
            // Enabling WebView debug once is enough; don't hook every WebView constructor.
            android.webkit.WebView.setWebContentsDebuggingEnabled(true);
        }

        XposedHelpers.findAndHookMethod(Helper.WebViewClientWrapper, "onPageStarted", android.webkit.WebView.class, String.class, Bitmap.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                boolean hideWaterMark = Helper.prefs.getBoolean("switch_mainswitch", false)
                        && Helper.prefs.getBoolean("switch_watermark", false);
                boolean hideSubscribe = Helper.prefs.getBoolean("switch_mainswitch", false)
                        && Helper.prefs.getBoolean("switch_subscribe", false);
                if (!hideWaterMark && !hideSubscribe)
                    return;
                android.webkit.WebView webview = (android.webkit.WebView) param.args[0];
                if (webview != null)
                    webview.evaluateJavascript(getScript(hideWaterMark, hideSubscribe), null);
            }
        });

        XposedHelpers.findAndHookMethod(Helper.WebViewClientWrapper, "onPageFinished", android.webkit.WebView.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                android.webkit.WebView webview = (android.webkit.WebView) param.args[0];
                String js = Helper.prefs.getString("edit_js", null);
                if (webview != null && js != null && !js.isEmpty()) {
                    webview.evaluateJavascript(js, null);
                }
            }
        });
    }

    static String getScript(boolean hideWaterMark, boolean hideSubscribe) {
        StringBuilder sb = new StringBuilder();
        sb.append("document.addEventListener('DOMContentLoaded',(event)=>{");
        if (hideWaterMark) {
            sb.append(script_hide_water_mark);
        }
        if (hideSubscribe) {
            sb.append(script_hide_subscribe);
        }
        sb.append("});");
        return sb.toString();
    }
}
