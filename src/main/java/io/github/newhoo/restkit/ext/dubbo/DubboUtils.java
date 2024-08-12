package io.github.newhoo.restkit.ext.dubbo;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author zunrong
 */
public class DubboUtils {

    public static final Icon DUBBO_API_ICON = IconLoader.getIcon("icons/dubbo.svg", DubboUtils.class);

    public static String toJson(Object obj) {
        if (obj == null || (obj instanceof CharSequence && ((CharSequence) obj).length() == 0)) {
            return "";
        }
        return new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create().toJson(obj);
    }
}