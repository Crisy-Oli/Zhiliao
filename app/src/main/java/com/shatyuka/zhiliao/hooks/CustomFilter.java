package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Helper;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class CustomFilter implements IHook {
    static Class<?> InnerDeserializer;
    static Class<?> ApiTemplateRoot;
    static Class<?> ApiFeedCard;
    static Class<?> MarketCard;
    static Class<?> ApiFeedContent;
    static Class<?> ApiText;
    static Class<?> ApiLine;
    static Class<?> ApiElement;
    static Class<?> Card;
    static Class<?> TextElement;
    static Class<?> Line;
    static Class<?> Avatar;
    static Class<?> Video;

    static Field ApiTemplateRoot_extra;
    static Field ApiTemplateRoot_common_card;
    static Field ApiFeedCard_feed_content;
    static Field ApiFeedContent_title;
    static Field ApiFeedContent_content;
    static Field ApiFeedContent_sourceLine;
    static Field ApiFeedContent_video;
    static Field ApiText_panel_text;
    static Field ApiLine_elements;
    static Field ApiElement_text;
    static Field Card_elements;
    static Field Card_extra;
    static Field Card_Extra_contentType;
    static Field Element_id;
    static Field Line_elements;
    static Field TextElement_text;

    @Override
    public String getName() {
        return "自定义过滤";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        InnerDeserializer = classLoader.loadClass("com.zhihu.android.api.util.ZHObjectRegistryCenter$InnerDeserializer");
        ApiTemplateRoot = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiTemplateRoot");
        ApiFeedCard = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiFeedCard");
        MarketCard = classLoader.loadClass("com.zhihu.android.api.model.MarketCard");
        ApiFeedContent = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiFeedContent");
        ApiText = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiText");
        ApiLine = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiLine");
        ApiElement = classLoader.loadClass("com.zhihu.android.api.model.template.api.ApiElement");
        try {
            Card = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Card");
        } catch (ClassNotFoundException ignored) {
        }
        if (Card != null) {
            Card_elements = Card.getDeclaredField("elements");
            Card_elements.setAccessible(true);
            Card_extra = Card.getDeclaredField("extra");
            Card_extra.setAccessible(true);
            Card_Extra_contentType = Card_extra.getType().getDeclaredField("contentType");
            Card_Extra_contentType.setAccessible(true);

            Class<?> Element = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Element");
            Element_id = Element.getDeclaredField("id");
            Element_id.setAccessible(true);
            TextElement = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.TextElement");
            TextElement_text = TextElement.getDeclaredField("text");
            TextElement_text.setAccessible(true);
            Line = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Line");
            Line_elements = Line.getDeclaredField("elements");
            Line_elements.setAccessible(true);
            Avatar = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Avatar");
            Video = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Video");
        }

        ApiTemplateRoot_extra = ApiTemplateRoot.getField("extra");
        ApiTemplateRoot_common_card = ApiTemplateRoot.getField("common_card");
        ApiFeedCard_feed_content = ApiFeedCard.getField("feed_content");
        ApiFeedContent_title = ApiFeedContent.getField("title");
        ApiFeedContent_content = ApiFeedContent.getField("content");
        ApiFeedContent_sourceLine = ApiFeedContent.getField("sourceLine");
        ApiFeedContent_video = ApiFeedContent.getField("video");
        ApiText_panel_text = ApiText.getField("panel_text");
        ApiLine_elements = ApiLine.getField("elements");
        ApiElement_text = ApiElement.getField("text");
    }

    private static Object read(Field field, Object target) {
        if (field == null || target == null || !field.getDeclaringClass().isInstance(target))
            return null;
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static String readText(Field field, Object target) {
        Object value = read(field, target);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public void hook() throws Throwable {
        XposedBridge.hookAllMethods(InnerDeserializer, "deserialize", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Helper.prefs.getBoolean("switch_mainswitch", false))
                    return;
                Object result = param.getResult();
                if (result == null)
                    return;

                if (ApiTemplateRoot.isInstance(result)) {
                    Object extra = read(ApiTemplateRoot_extra, result);
                    String type = readText(Helper.DataUnique_type, extra);
                    Object card = read(ApiTemplateRoot_common_card, result);
                    Object feed = read(ApiFeedCard_feed_content, card);

                    boolean hasVideo = read(ApiFeedContent_video, feed) != null;
                    String title = readText(ApiText_panel_text,
                            read(ApiFeedContent_title, feed));
                    String content = readText(ApiText_panel_text,
                            read(ApiFeedContent_content, feed));
                    String author = null;

                    Object sourceLine = read(ApiFeedContent_sourceLine, feed);
                    Object lineItems = read(ApiLine_elements, sourceLine);
                    if (lineItems instanceof List) {
                        List<?> entries = (List<?>) lineItems;
                        if (entries.size() > 1)
                            author = readText(ApiText_panel_text,
                                    read(ApiElement_text, entries.get(1)));
                    }
                    if (filter(type, hasVideo, title, author, content))
                        param.setResult(null);
                } else if (Card != null && Card.isInstance(result)) {
                    String type = readText(Card_Extra_contentType,
                            read(Card_extra, result));
                    boolean hasVideo = false;
                    String title = null;
                    String author = null;
                    String content = null;

                    Object rawElements = read(Card_elements, result);
                    if (rawElements instanceof List) {
                        for (Object element : (List<?>) rawElements) {
                            if (element == null)
                                continue;
                            if (Video != null && Video.isInstance(element))
                                hasVideo = true;

                            String id = readText(Element_id, element);
                            if (id == null)
                                continue;
                            if (title == null && author == null
                                    && "Text".equals(id) && TextElement.isInstance(element)) {
                                title = readText(TextElement_text, element);
                                continue;
                            }
                            if (author == null && content == null && "0".equals(id)
                                    && Line != null && Line.isInstance(element)) {
                                Object subItems = read(Line_elements, element);
                                if (subItems instanceof List) {
                                    List<?> lineElements = (List<?>) subItems;
                                    if (!lineElements.isEmpty() && Avatar.isInstance(lineElements.get(0))) {
                                        for (Object lineElement : lineElements) {
                                            if (lineElement != null && TextElement.isInstance(lineElement)) {
                                                author = readText(TextElement_text, lineElement);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (content == null && id.endsWith("_summary")
                                    && TextElement.isInstance(element))
                                content = readText(TextElement_text, element);
                        }
                    }
                    if (filter(type, hasVideo, title, author, content))
                        param.setResult(null);
                } else if (MarketCard.isInstance(result)
                        && Helper.prefs.getBoolean("switch_marketcard", false)) {
                    param.setResult(null);
                }
            }
        });
    }

    private boolean filter(String type, boolean hasVideo, String title, String author, String content) {
        if (Helper.prefs.getBoolean("switch_video", false) && ("zvideo".equals(type) || "drama".equals(type) || hasVideo)) {
            return true;
        }
        if (Helper.prefs.getBoolean("switch_removearticle", false) && ("article".equals(type) || "Post".equals(type))) {
            return true;
        }
        if (Helper.prefs.getBoolean("switch_pin", false) && "pin".equals(type)) {
            return true;
        }
        if (Helper.prefs.getBoolean("switch_feedad", true) && "SvipActivity".equals(type)) {
            return true;
        }

        if (Helper.regex_title != null && title != null && Helper.regex_title.matcher(title).find()) {
            return true;
        }
        if (Helper.regex_author != null && author != null && Helper.regex_author.matcher(author).find()) {
            return true;
        }
        if (Helper.regex_content != null && content != null && Helper.regex_content.matcher(content).find()) {
            return true;
        }

        return false;
    }
}
