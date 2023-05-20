package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.openapi.util.IconLoader;
import io.github.newhoo.restkit.common.RestItem;
import io.github.newhoo.restkit.restful.RestItemDetail;
import io.github.newhoo.restkit.restful.ep.RestItemDetailProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * k8s restItm detail
 */
public class DubboRestItemDetail implements RestItemDetail {

    @NotNull
    @Override
    public String getProtocol() {
        return "dubbo";
    }

    @Override
    public Icon getIcon(RestItem restItem) {
        return IconLoader.getIcon("icons/dubbo.svg", getClass());
    }

    @Override
    public @NotNull String getName(@NotNull RestItem restItem, boolean useApiDesc) {
        return restItem.getUrl();
    }

    public static class DubboRestItemDetailProvider implements RestItemDetailProvider {
        @Override
        public DubboRestItemDetail createRestItemDetail() {
            return new DubboRestItemDetail();
        }
    }
}
