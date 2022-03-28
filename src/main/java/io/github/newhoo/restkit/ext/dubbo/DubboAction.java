package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import io.github.newhoo.restkit.toolwindow.RestToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * dubbo jumping action
 *
 * @author newhoo
 * @date 2022/3/13 6:39 PM
 * @since 1.0.0
 */
public class DubboAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) psiElement;
            if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC) || psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            PsiClass containingClass = psiMethod.getContainingClass();
            boolean flag = containingClass != null && (
                    containingClass.hasAnnotation("com.alibaba.dubbo.config.annotation.Service")
                            || containingClass.hasAnnotation("org.apache.dubbo.config.annotation.Service")
            );
            e.getPresentation().setEnabledAndVisible(flag);
            return;
        }
        e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiElement psiElement = e.getRequiredData(CommonDataKeys.PSI_ELEMENT);
        RestToolWindowFactory.getRestServiceToolWindow(e.getProject(), restServiceToolWindow -> {
            restServiceToolWindow.navigateToTree(psiElement);
        });
    }
}
