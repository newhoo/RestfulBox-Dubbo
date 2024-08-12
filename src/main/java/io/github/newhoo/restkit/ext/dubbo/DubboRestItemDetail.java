package io.github.newhoo.restkit.ext.dubbo;

import io.github.newhoo.restkit.open.RestItemDetail;
import io.github.newhoo.restkit.open.ep.RestItemDetailProvider;
import io.github.newhoo.restkit.open.model.RestItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static io.github.newhoo.restkit.ext.dubbo.DubboUtils.DUBBO_API_ICON;

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
        return DUBBO_API_ICON;
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
