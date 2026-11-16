package com.shatyuka.zhiliao;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.shatyuka.zhiliao.hooks.AnswerAd;
import com.shatyuka.zhiliao.hooks.AnswerListAd;
import com.shatyuka.zhiliao.hooks.Article;
import com.shatyuka.zhiliao.hooks.AutoRefresh;
import com.shatyuka.zhiliao.hooks.Cleaner;
import com.shatyuka.zhiliao.hooks.ColorMode;
import com.shatyuka.zhiliao.hooks.CommentAd;
import com.shatyuka.zhiliao.hooks.CustomFilter;
import com.shatyuka.zhiliao.hooks.ExternLink;
import com.shatyuka.zhiliao.hooks.FeedAd;
import com.shatyuka.zhiliao.hooks.FeedTopHotBanner;
import com.shatyuka.zhiliao.hooks.FollowButton;
import com.shatyuka.zhiliao.hooks.HeadZoneBanner;
import com.shatyuka.zhiliao.hooks.Horizontal;
import com.shatyuka.zhiliao.hooks.HotBanner;
import com.shatyuka.zhiliao.hooks.IHook;
import com.shatyuka.zhiliao.hooks.LaunchAd;
import com.shatyuka.zhiliao.hooks.LiveButton;
import com.shatyuka.zhiliao.hooks.MineHybridView;
import com.shatyuka.zhiliao.hooks.NavButton;
import com.shatyuka.zhiliao.hooks.NavRes;
import com.shatyuka.zhiliao.hooks.NextAnswer;
import com.shatyuka.zhiliao.hooks.FullScreen;
import com.shatyuka.zhiliao.hooks.RedDot;
import com.shatyuka.zhiliao.hooks.SearchAd;
import com.shatyuka.zhiliao.hooks.ShareAd;
import com.shatyuka.zhiliao.hooks.StatusBar;
import com.shatyuka.zhiliao.hooks.Tag;
import com.shatyuka.zhiliao.hooks.ThirdPartyLogin;
import com.shatyuka.zhiliao.hooks.VIPBanner;
import com.shatyuka.zhiliao.hooks.WebView;
import com.shatyuka.zhiliao.hooks.ZhihuPreference;

import de.robv.android.xposed.XposedBridge;

public class Hooks {
    static final IHook[] hooks = {
            new ZhihuPreference(),
            new LaunchAd(),
            new CustomFilter(),
            new FeedAd(),
            new AnswerListAd(),
            new CommentAd(),
            new AnswerAd(),
            new ShareAd(),
            new LiveButton(),
            new Horizontal(),
            new NextAnswer(),
            new RedDot(),
            new ExternLink(),
            new VIPBanner(),
            new NavButton(),
            new HotBanner(),
            new ColorMode(),
            new Article(),
            new Tag(),
            new SearchAd(),
            new StatusBar(),
            new ThirdPartyLogin(),
            new NavRes(),
            new WebView(),
            new Cleaner(),
            new FeedTopHotBanner(),
            new HeadZoneBanner(),
            new MineHybridView(),
            new FollowButton(),
            new FullScreen(),
            new AutoRefresh(),
    };

    public static void init(final ClassLoader classLoader) {
        List<String> failed = new ArrayList<>();
        int loaded = 0;
        for (IHook hook : hooks) {
            try {
                hook.init(classLoader);
                hook.hook();
                loaded++;
            } catch (Throwable e) {
                String name = hook.getName();
                failed.add(name);
                XposedBridge.log("[Zhiliao] Hook initialization failed: " + name + ": " + e);
                XposedBridge.log(e);
            }
        }
        XposedBridge.log("[Zhiliao] Initialized " + loaded + "/" + hooks.length
                + " hooks for Zhihu " + Helper.packageInfo.versionName);
        if (!failed.isEmpty() && !Helper.prefs.getBoolean("switch_hidetoast", false)) {
            // One notification avoids a long toast queue after each Zhihu launch.
            Helper.toast(failed.size() + " 项功能未能加载，请查看 LSPosed 日志",
                    Toast.LENGTH_LONG);
        }
    }
}
