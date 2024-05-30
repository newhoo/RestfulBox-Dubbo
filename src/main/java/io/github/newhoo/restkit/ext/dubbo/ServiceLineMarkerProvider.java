package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import io.github.newhoo.restkit.common.HttpMethod;
import io.github.newhoo.restkit.common.PsiRestItem;
import io.github.newhoo.restkit.config.ConfigHelper;
import io.github.newhoo.restkit.toolwindow.ToolWindowHelper;
import org.jetbrains.annotations.NotNull;

/**
 * ServiceLineMarkerProvider
 *
 * @author newhoo
 * @date 2023/1/8
 * @since 1.0.1
 */
public class ServiceLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiMethod && ConfigHelper.getGlobalSetting().isEnableMethodLineMarker()) {
            PsiMethod psiMethod = (PsiMethod) element;
            if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC) || psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                return null;
            }
            PsiClass containingClass = psiMethod.getContainingClass();
            boolean flag = containingClass != null && (
                    containingClass.hasAnnotation("com.alibaba.dubbo.config.annotation.Service")
                            || containingClass.hasAnnotation("org.apache.dubbo.config.annotation.Service")
                            || containingClass.hasAnnotation("org.apache.dubbo.config.annotation.DubboService")
            );
            if (flag) {
                return new LineMarkerInfo<>(element, element.getTextRange(), ConfigHelper.NAVIGATE_ICON,
                                            psiElement -> ConfigHelper.NAVIGATE_TEXT,
                                            (e, elt) -> {
                                                ToolWindowHelper.navigateToTree(elt, () -> {
                                                    PsiRestItem item = new PsiRestItem(psiMethod.getName(), HttpMethod.UNDEFINED.name(), "", "", psiMethod, DubboResolver.paramResolver);
                                                    item.setProtocol("dubbo");
                                                    return item;
                                                });
                                            },
                                            GutterIconRenderer.Alignment.LEFT, () -> "RestfulBox-Dubbo");
            }
        }
        return null;
    }
}
