# 知了 × 知乎 11.16.0 (41605) 兼容性维护记录

> 状态：维护候选版本（不是已完成实机验收的正式版）。

- 知乎包名：`com.zhihu.android`
- 目标版本：`11.16.0` / `41605`
- 提供的 APK SHA-256：`65cd8d9092ed348aa7f2aeeca474ff5169dc2679b8a15fbaa1a66f4c9fd34cb7`
- 维护分支：`dev/zhihu-hook-maintenance`
- 原版备份：`backup/pre-upstream-sync-2026-11-16`
- 生产基线：`master`（仍对应上游，不直接开发）

## 测试定义

1. **DEX 静态检查**：解析用户提供的官方 APK 的 DEX 类定义、方法原型、字段。
2. **CI 编译检查**：GitHub Actions `assembleRelease` 构建成功，确保 Java、NDK、资源和 APK 打包无构建错误。
3. **实机验证（未完成）**：需要安装知乎 11.16.0 的 Android 设备、LSPosed、相应模块作用域，逐项测试后才能宣称功能正常。

注意：类或方法存在不意味着用户界面行为、服务器响应或 Hook 成功；构建通过不意味着实机不崩溃。

## 31 项 Hook 清单

| Hook | 功能 | 当前维护 / 风险 |
| --- | --- | --- |
| ZhihuPreference | 模块设置入口 | 静态依赖类存在；需检查设置实际可操作 |
| LaunchAd | 启动页去广告 | 旧接口仍存在；需实机检查开屏场景 |
| CustomFilter | 信息流自定义过滤 | 修复空字段与嵌套列表异常；未启用过滤时快速退出 |
| FeedAd | 信息流广告 | 添加 `resolveCommonAdParam`、`resolveHotAdParam`；保留旧版接口 |
| AnswerListAd | 回答列表广告 | `resolveAnswerAdParam` 静态存在，需实测 |
| CommentAd | 评论广告 | 旧解析接口已删除；新增加反序列化/展示前广告项过滤 |
| AnswerAd | 回答底部广告 | 只选返回 `WebResourceResponse` 的方法；修复空响应与响应读取 |
| ShareAd | 分享广告 | `showShareAd(View)` 静态存在；需实测 |
| LiveButton | 直播入口 | 静态检查未发现明确失效点；需实测 |
| Horizontal | 左右划切换回答 | 修复监听器字段、手势取消/父容器处理；需实测手势不冲突 |
| NextAnswer | 下一个回答按钮 | 修复视图类型判断；需实测按钮及 ViewPager 行为 |
| RedDot | 通知红点 | 补充 `hasUnreadCount()`；不同红点入口需实测 |
| ExternLink | 外链跳转 | 目标 URL、异常路径及返回类型兼容修复 |
| VIPBanner | 会员卡片 | 静态依赖存在；仍需实测新布局 |
| NavButton | 导航按钮隐藏 | 按返回类型选入口、继承方法取导航 ID，修复「发现」开关 |
| HotBanner | 热点通知 | `fetchFloatNotification` 存在，需实测 |
| ColorMode | 色彩模式 | Android API 接口可用，需实测 |
| Article | 文章精简 | 字符串值比较与 fragment 空值保护 |
| Tag | 信息流卡片类别标签 | 相关字段静态存在；禁用时跳过初始化 |
| SearchAd | 搜索广告 | 清理新版 `SearchRecommendQuery.commercialData` |
| StatusBar | 沉浸式状态栏 | 静态依赖存在，需检查系统栏对比 |
| ThirdPartyLogin | 第三方登录入口 | 静态依赖存在；禁用时跳过初始化 |
| NavRes | 导航栏样式 | 禁用时跳过混淆类搜索；新版布局需实测 |
| WebView | 网页净化、JS | 文档完成后注入，减少无用 JS 和全局构造 Hook |
| Cleaner | 临时文件清理 | 递归 I/O 移至工作线程；仅统计真实删除的文件 |
| FeedTopHotBanner | 推荐页顶部热点 | 目标反序列化方法继承自父类；修复原子类 Hook 无效 |
| HeadZoneBanner | 热榜置顶 | 静态字段与方法存在，需实测 |
| MineHybridView | 「我的」混合卡片 | 静态字段存在，需实测 |
| FollowButton | 关注按钮 | 静态字段存在，需实测 UI |
| FullScreen | 禁止自动进入清屏 | 修复新版 c$c 入口，保留 c$d 退出路径 |
| AutoRefresh | 禁止自动刷新 | 类名迁移有 fallback；未确认所有刷新行为，需实测 |

## 已实施的性能措施

- 未启用的重型 Hook 跳过 `init()` 内的反射/混淆类扫描：Horizontal、AutoRefresh、NextAnswer、Tag、RedDot、Article、VIPBanner、ThirdPartyLogin、NavRes、NavButton。
- 自定义过滤未开启对应条件时，跳过卡片对象反射遍历。
- 缓存清理从 UI 主线程迁出，不计入失败删除量。
- WebView 仅在需要时注入脚本，不再每个 WebView 构造都执行调试 Hook。
- 初始化异常单条记录在 LSPosed 日志中，用户只收到一次汇总提示，避免启动时连续 Toast。
- CI 自动生成调试签名的 Release APK；与原作者签名可能不同，不保证直接覆盖安装。

## 实机验收方法

1. 用安装包管理工具确认知乎版本为 11.16.0（41605）。
2. 安装新编译的知了模块，LSPosed 作用域仅勾选知乎，重启知乎进程；检查有无启动崩溃。
3. 开启总开关，分组对照：启动广告、首页信息流、搜索广告、回答列表/回答底部、评论广告、分享广告。
4. 检查导航按钮/会员卡片/红点/热榜/关注按钮是否消失但正常操作保留。
5. 开启左右划切换回答、自动刷新禁止、全屏禁止，分别测试触摸取消、内容刷新、页面切换和退出页面。
6. 检查网页文章正常渲染、页面链接跳转、夜间模式、返回行为和长内容加载。
7. 对照 LSPosed 日志 `[Zhiliao]` 检查失败 Hook、异常堆栈和功能名称。
8. 分别在所有可选功能关闭和常用功能打开的情况下，比较冷启动耗时、列表帧率、内存与耗电（使用同一设备、同一版本、相同网络条件）。

不能用“CI 成功”替代这些实际验收项。
