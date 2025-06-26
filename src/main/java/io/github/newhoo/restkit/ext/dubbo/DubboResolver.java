package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import io.github.newhoo.restkit.open.LanguageResolver;
import io.github.newhoo.restkit.open.ParamResolver;
import io.github.newhoo.restkit.open.RequestResolver;
import io.github.newhoo.restkit.open.ep.RestfulResolverProvider;
import io.github.newhoo.restkit.open.helper.java.JavaHelper;
import io.github.newhoo.restkit.open.model.KV;
import io.github.newhoo.restkit.open.model.ProjectSetting;
import io.github.newhoo.restkit.open.model.api.HttpMethod;
import io.github.newhoo.restkit.open.model.api.PsiRestItem;
import io.github.newhoo.restkit.open.model.api.RestItem;
import io.github.newhoo.restkit.open.model.api.SimpleLineMarkerInfo;
import io.github.newhoo.restkit.open.model.api.parameter.JsonStruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
    public int order() {
        return 5;
    }

    @Override
    public @NotNull String getDescription() {
        return "- 支持 Dubbo 接口扫描和在线调试，识别 @Service、@DubboService等注解<br/>- 支持 Java 语言";
    }

    @Override
    public @NotNull List<RestItem> findRestItemListInModule(Module module, GlobalSearchScope globalSearchScope) {
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

            if (psiElement instanceof PsiClass psiClass) {
                List<RestItem> serviceItemList = getRequestItemList(psiClass, module);
                itemList.addAll(serviceItemList);
            }
        }
        return itemList;
    }

    private List<RestItem> getRequestItemList(PsiClass psiClass, Module module) {
        List<RestItem> itemList = new ArrayList<>();
        PsiClass superClass = Arrays.stream(psiClass.getSupers()).filter(PsiClass::isInterface).findFirst().orElse(null);
        if (superClass == null) {
            return itemList;
        }
        LanguageResolver languageResolver = JavaHelper.getLanguageResolver(psiClass);
        if (languageResolver.isIgnored(psiClass)) {
            return itemList;
        }
        String groupName = languageResolver.findApiGroup(psiClass).orElseGet(psiClass::getQualifiedName);
        Set<String> classTags = languageResolver.findApiTags(psiClass);
        boolean showModuleFolder = ProjectSetting.getInstance(module.getProject()).isShowModuleFolder();
        for (PsiMethod psiMethod : psiClass.getMethods()) {
            if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC)
                    || psiMethod.hasModifierProperty(PsiModifier.STATIC)
                    || psiMethod.findSuperMethods(superClass).length == 0
                    || languageResolver.isIgnored(psiMethod)) {
                continue;
            }
            PsiRestItem<PsiMethod> item = new PsiRestItem<>(psiMethod.getName(), HttpMethod.UNDEFINED.name(), module.getName(), getFrameworkName(), psiMethod, this);
            item.setProtocol("dubbo");

            String apiName = languageResolver.findApiName(psiMethod).orElseGet(psiMethod::getName);
            Set<String> methodTags = languageResolver.findApiTags(psiMethod);
            item.setName(apiName);
            item.setFolderPath(item.getModuleName(), groupName, showModuleFolder);
            item.setTags(new LinkedHashSet<String>(CollectionUtils.union(classTags, methodTags)));

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
        if (!(psiElement instanceof PsiMethod psiMethod)) {
            return "";
        }
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
    public JsonStruct buildRequestBodyStruct(PsiElement psiElement) {
        return null;
    }

    @Override
    public @NotNull String buildResponseBodyJson(@NotNull PsiElement psiElement) {
        return "";
    }

    @Override
    public JsonStruct buildResponseBodyStruct(PsiElement psiElement) {
        return null;
    }

    @Override
    public @NotNull String buildDescription(@NotNull PsiElement psiElement) {
        return JavaHelper.getLanguageResolver(psiElement).findApiDescription(psiElement).orElse("");
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
            return Arrays.stream(element.getChildren())
                         .filter(e -> e instanceof PsiIdentifier)
                         .findFirst()
                         .map(child -> {
                             // fix: Performance warning: LineMarker is supposed to be registered for leaf elements only. 返回 PsiIdentifier
                             SimpleLineMarkerInfo simpleLineMarkerInfo1 = new SimpleLineMarkerInfo(child, child.getTextRange());
                             simpleLineMarkerInfo1.setNavElementSupplier(child::getParent);
                             return simpleLineMarkerInfo1;
                         })
                         .orElse(null);
        }
        return null;
    }

    @Override
    public RestItem tryGenerateRestItem(@NotNull PsiElement psiElement) {
        PsiMethod psiMethod = null;
        if (psiElement instanceof PsiMethod) {
            psiMethod = (PsiMethod) psiElement;
        }
        if (psiElement instanceof PsiIdentifier && psiElement.getParent() instanceof PsiMethod) {
            psiMethod = (PsiMethod) psiElement.getParent();
        }
        if (psiMethod == null) {
            return null;
        }
        PsiRestItem item = new PsiRestItem(psiMethod.getName(), HttpMethod.UNDEFINED.name(), "", psiMethod, this);
        item.setProtocol("dubbo");
        return item;
    }

    public static class DubboResolverProvider implements RestfulResolverProvider {
        @Override
        public @NotNull RequestResolver createRequestResolver() {
            return new DubboResolver();
        }
    }
}
