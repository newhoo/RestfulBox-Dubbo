package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.search.GlobalSearchScope;
import io.github.newhoo.restkit.open.ParamResolver;
import io.github.newhoo.restkit.open.RequestResolver;
import io.github.newhoo.restkit.open.common.HttpMethod;
import io.github.newhoo.restkit.open.ep.RestfulResolverProvider;
import io.github.newhoo.restkit.open.model.JsonStruct;
import io.github.newhoo.restkit.open.model.KV;
import io.github.newhoo.restkit.open.model.PsiRestItem;
import io.github.newhoo.restkit.open.model.RestItem;
import io.github.newhoo.restkit.open.model.SimpleLineMarkerInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.newhoo.restkit.ext.dubbo.DubboUtils.DUBBO_API_ICON;

/**
 * dubbo service scanner
 *
 * @author newhoo
 * @date 2022/3/13 5:27 PM
 * @since 1.0.0
 */
public class DubboResolver implements RequestResolver, ParamResolver {

    @NotNull
    @Override
    public String getFrameworkName() {
        return "Dubbo";
    }

    @Override
    public List<RestItem> findRestItemListInModule(Module module, GlobalSearchScope globalSearchScope) {
        List<RestItem> itemList = new ArrayList<>();
        Collection<PsiAnnotation> psiAnnotations = JavaAnnotationIndex.getInstance().getAnnotations("Service", module.getProject(), globalSearchScope);
        Collection<PsiAnnotation> psiAnnotations2 = JavaAnnotationIndex.getInstance().getAnnotations("DubboService", module.getProject(), globalSearchScope);
        if (!psiAnnotations2.isEmpty()) {
            psiAnnotations.addAll(psiAnnotations2);
        }
        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            if (!"com.alibaba.dubbo.config.annotation.Service".equals(psiAnnotation.getQualifiedName())
                    && !"org.apache.dubbo.config.annotation.Service".equals(psiAnnotation.getQualifiedName())
                    && !"org.apache.dubbo.config.annotation.DubboService".equals(psiAnnotation.getQualifiedName())
            ) {
                continue;
            }
            PsiModifierList psiModifierList = (PsiModifierList) psiAnnotation.getParent();
            PsiElement psiElement = psiModifierList.getParent();

            if (psiElement instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) psiElement;
                List<RestItem> serviceItemList = getRequestItemList(psiClass, module);
                itemList.addAll(serviceItemList);
            }
        }
        return itemList;
    }

    private List<RestItem> getRequestItemList(PsiClass psiClass, Module module) {
        List<RestItem> itemList = new ArrayList<>();
        PsiClass superClass = null;
        for (PsiClass aSuper : psiClass.getSupers()) {
            if (aSuper.isInterface()) {
                superClass = aSuper;
                break;
            }
        }
        if (superClass == null) {
            return itemList;
        }
        for (PsiMethod psiMethod : psiClass.getMethods()) {
            if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC)
                    || psiMethod.hasModifierProperty(PsiModifier.STATIC)
                    || psiMethod.findSuperMethods(superClass).length == 0) {
                continue;
            }
            PsiRestItem item = new PsiRestItem(psiMethod.getName(), HttpMethod.UNDEFINED.name(), module.getName(), getFrameworkName(), psiMethod, this);
            item.setProtocol("dubbo");
            item.setPackageName(superClass.getQualifiedName());
            itemList.add(item);
        }
        return itemList;
    }

    @NotNull
    @Override
    public List<KV> buildHeaders(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KV> buildParams(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull List<JsonStruct> buildParamStruct(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String buildRequestBodyJson(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof PsiMethod)) {
            return "";
        }
        PsiMethod psiMethod = (PsiMethod) psiElement;
        PsiClass superClass = null;
        for (PsiClass aSuper : psiMethod.getContainingClass().getSupers()) {
            if (aSuper.isInterface()) {
                superClass = aSuper;
                break;
            }
        }
        if (superClass == null) {
            return "";
        }

        Map<String, Object> map = new LinkedHashMap<>();
        List<String> parameterTypes = Arrays.stream(psiMethod.getParameterList().getParameters())
                                            .map(o -> o.getType().getCanonicalText())
                                            .collect(Collectors.toList());
        List<Object> parameterValues = parameterTypes.stream()
                                                     .map(o -> PsiClassHelper.assemblePsiClass(o, psiMethod.getProject(), 0, true))
                                                     .collect(Collectors.toList());
        parameterTypes = parameterTypes.stream()
                                       .map(type -> type.contains("<") ? type.substring(0, type.indexOf("<")) : type)
                                       .collect(Collectors.toList());
        PsiClass containingClass = psiMethod.getContainingClass();
        PsiAnnotation annotation = containingClass.getAnnotation("com.alibaba.dubbo.config.annotation.Service");
        if (annotation == null) {
            annotation = containingClass.getAnnotation("org.apache.dubbo.config.annotation.Service");
        }
        if (annotation == null) {
            annotation = containingClass.getAnnotation("org.apache.dubbo.config.annotation.DubboService");
        }
        map.put("interface", superClass.getQualifiedName());
        map.put("method", psiMethod.getName());
        String group = ObjectUtils.defaultIfNull(getAnnotationValue(annotation, "group"), "").toString();
        map.put("group", group.contains("${") ? "" : group);
        String version = ObjectUtils.defaultIfNull(getAnnotationValue(annotation, "version"), "").toString();
        map.put("version", version.contains("${") ? "" : version);
        map.put("parameterTypes", parameterTypes);
        map.put("parameterValues", parameterValues);
        String s = DubboUtils.toJson(map);
        return Objects.nonNull(s) ? s : "";
    }

    @Override
    public String buildResponseBodyJson(@NotNull PsiElement psiElement) {
        return "";
    }

    @Override
    public JsonStruct buildRequestBodyStruct(PsiElement psiElement) {
        return null;
    }

    @Override
    public JsonStruct buildResponseBodyStruct(PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String buildDescription(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof PsiMethod)) {
            return "";
        }
        PsiMethod psiMethod = (PsiMethod) psiElement;

        PsiClass superClass = null;
        for (PsiClass aSuper : psiMethod.getContainingClass().getSupers()) {
            if (aSuper.isInterface()) {
                superClass = aSuper;
                break;
            }
        }
        if (superClass != null) {
            for (PsiMethod superMethod : psiMethod.findSuperMethods(superClass)) {
                String restName = null;
                String location;
                if (superMethod.getDocComment() != null) {
                    restName = Arrays.stream(superMethod.getDocComment().getDescriptionElements())
                                     .filter(e -> e instanceof PsiDocToken)
                                     .filter(e -> StringUtils.isNotBlank(e.getText()))
                                     .findFirst()
                                     .map(e -> e.getText().trim()).orElse(null);
                }
                location = superMethod.getContainingClass().getName().concat("#").concat(superMethod.getName());
                if (StringUtils.isNotEmpty(restName)) {
                    location = location.concat("#").concat(restName);
                }
                return location;
            }
        }
        return "";
    }

    private static Object getAnnotationValue(PsiAnnotation annotation, String name) {
        if (annotation == null) {
            return null;
        }
        Object paramName = null;
        PsiAnnotationMemberValue attributeValue = annotation.findDeclaredAttributeValue(name);

        if (attributeValue instanceof PsiLiteralExpression) {
            paramName = ((PsiLiteralExpression) attributeValue).getValue();
        }
        return paramName;
    }

    @Override
    public boolean canNavigateToTree(@NotNull PsiElement element) {
        if (!(element instanceof PsiMethod)) {
            return false;
        }
        PsiMethod psiMethod = (PsiMethod) element;
        if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC) || psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }
        PsiClass containingClass = psiMethod.getContainingClass();
        return containingClass != null && (
                containingClass.hasAnnotation("com.alibaba.dubbo.config.annotation.Service")
                        || containingClass.hasAnnotation("org.apache.dubbo.config.annotation.Service")
                        || containingClass.hasAnnotation("org.apache.dubbo.config.annotation.DubboService")
        );
    }

    @Override
    public SimpleLineMarkerInfo tryGenerateLineMarker(@NotNull PsiElement element) {
        if (canNavigateToTree(element)) {
            TextRange textRange = Arrays.stream(element.getChildren())
                                        .filter(e -> e instanceof PsiIdentifier)
                                        .findFirst()
                                        .map(e -> e.getTextRange())
                                        .orElse(null);
            if (textRange != null) {
                SimpleLineMarkerInfo simpleLineMarkerInfo = new SimpleLineMarkerInfo(element, textRange);
                simpleLineMarkerInfo.setIcon(DUBBO_API_ICON);
                return simpleLineMarkerInfo;
            }
        }
        return null;
    }

    @Override
    public RestItem tryGenerateRestItem(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof PsiMethod)) {
            return null;
        }
        PsiMethod psiMethod = (PsiMethod) psiElement;
        PsiRestItem item = new PsiRestItem(psiMethod.getName(), HttpMethod.UNDEFINED.name(), "", "", psiMethod, this);
        item.setProtocol("dubbo");
        return item;
    }

    public static class DubboResolverProvider implements RestfulResolverProvider {
        @Override
        public RequestResolver createRequestResolver(@NotNull Project project) {
            return new DubboResolver();
        }
    }
}
