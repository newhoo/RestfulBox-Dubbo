package io.github.newhoo.restkit.ext.dubbo;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PsiClassHelper in Java
 *
 * @author huzunrong
 * @since 2.0.0
 */
class PsiClassHelper {

    private static final int MAX_CORRELATION_COUNT = 6;

    public static Object assemblePsiClass(String typeCanonicalText, Project project, int autoCorrelationCount, boolean putClass) {
        boolean containsGeneric = typeCanonicalText.contains("<");
        // 数组|集合
        if (TypeUtils.isArray(typeCanonicalText) || TypeUtils.isList(typeCanonicalText)) {
            String elementType = TypeUtils.isArray(typeCanonicalText)
                    ? typeCanonicalText.replace("[]", "")
                    : containsGeneric
                    ? typeCanonicalText.substring(typeCanonicalText.indexOf("<") + 1, typeCanonicalText.lastIndexOf(">"))
                    : Object.class.getCanonicalName();
            return Collections.singletonList(assemblePsiClass(elementType, project, autoCorrelationCount, putClass));
        }

        PsiClass psiClass = PsiClassHelper.findPsiClass(typeCanonicalText, project);
        if (psiClass == null) {
            //简单常用类型
            if (TypeUtils.isPrimitiveOrSimpleType(typeCanonicalText)) {
                return TypeUtils.getExampleValue(typeCanonicalText, false);
            }
            return Collections.emptyMap();
        }

        //简单常用类型
        if (TypeUtils.isPrimitiveOrSimpleType(typeCanonicalText)) {
            return TypeUtils.getExampleValue(typeCanonicalText, false);
        }

        // 枚举
        if (psiClass.isEnum()) {
            PsiField[] enumFields = psiClass.getFields();
            for (PsiField enumField : enumFields) {
                if (enumField instanceof PsiEnumConstant) {
                    return enumField.getName();
                }
            }
            return "";
        }

        // Map
        if (TypeUtils.isMap(typeCanonicalText)) {
            return Collections.emptyMap();
        }

        if (autoCorrelationCount > MAX_CORRELATION_COUNT) {
            return Collections.emptyMap();
        }
        autoCorrelationCount++;

        Map<String, Object> map = new LinkedHashMap<>();
        if (putClass) {
            if (containsGeneric) {
                map.put("class", typeCanonicalText.substring(0, typeCanonicalText.indexOf("<")));
            } else {
                map.put("class", typeCanonicalText);
            }
        }
        for (PsiField field : psiClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.FINAL) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                continue;
            }
            String fieldType = field.getType().getCanonicalText();
            // 不存在泛型
            if (!containsGeneric) {
                map.put(field.getName(), assemblePsiClass(fieldType, project, autoCorrelationCount, putClass));
                continue;
            }
            // 存在泛型
            if (TypeUtils.isPrimitiveOrSimpleType(fieldType.replaceAll("\\[]", ""))) {
                map.put(field.getName(), assemblePsiClass(fieldType, project, autoCorrelationCount, putClass));
            } else if (PsiClassHelper.findPsiClass(fieldType, project) == null) {
                map.put(field.getName(), assemblePsiClass(typeCanonicalText.substring(typeCanonicalText.indexOf("<") + 1, typeCanonicalText.lastIndexOf(">")), project, autoCorrelationCount, putClass));
            } else {
                map.put(field.getName(), assemblePsiClass(fieldType, project, autoCorrelationCount, putClass));
            }
        }
        return map;
    }

    /**
     * 查找类
     *
     * @param typeCanonicalText 参数类型全限定名称
     * @param project           当前project
     * @return 查找到的类
     */
    static PsiClass findPsiClass(String typeCanonicalText, Project project) {
        // 基本类型转化为对应包装类型
        typeCanonicalText = TypeUtils.primitiveToBox(typeCanonicalText);

        String className = typeCanonicalText;
        if (className.contains("[]")) {
            className = className.replaceAll("\\[]", "");
        }
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        if (className.lastIndexOf(".") > 0) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
        PsiClass[] classesByName = PsiShortNamesCache.getInstance(project).getClassesByName(className, GlobalSearchScope.allScope(project));
        for (PsiClass psiClass : classesByName) {
            if (typeCanonicalText.startsWith(psiClass.getQualifiedName())) {
                return psiClass;
            }
        }
        return null;
    }

    static final class TypeUtils {

        public static boolean isArray(String type) {
            return type.endsWith("[]");
        }

        public static boolean isList(String type) {
            if (type.contains("<")) {
                type = type.substring(0, type.indexOf("<"));
            }
            switch (type) {
                case "java.util.List":
                case "java.util.ArrayList":
                case "java.util.LinkedList":
                case "java.util.Set":
                case "java.util.HashSet":
                case "java.util.TreeSet":
                case "java.util.SortedSet":
                case "java.util.Collection":
                case "java.util.ArrayDeque":
                case "java.util.PriorityQueue":
                    return true;
                default:
                    return false;
            }
        }

        public static boolean isMap(String type) {
            if (type.contains("<")) {
                type = type.substring(0, type.indexOf("<"));
            }
            switch (type) {
                case "java.util.Properties":
                case "java.util.Map":
                case "java.util.HashMap":
                case "java.util.LinkedHashMap":
                case "java.util.TreeMap":
                case "java.util.SortedMap":
                case "java.util.ConcurrentHashMap":
                case "java.util.Hashtable":
                    return true;
                default:
                    return false;
            }
        }

        public static boolean isPrimitiveOrSimpleType(String parameterType) {
            if (parameterType == null) {
                return false;
            }
            String type = parameterType.replace("PsiType:", "");
            switch (type) {
                case "byte":
                case "java.lang.Byte":
                case "char":
                case "java.lang.String":
                case "java.util.Date":
                case "java.sql.Timestamp":
                case "java.time.LocalDate":
                case "java.time.LocalDateTime":
                case "short":
                case "java.lang.Short":
                case "int":
                case "java.lang.Integer":
                case "long":
                case "java.lang.Long":
                case "float":
                case "java.lang.Float":
                case "double":
                case "java.lang.Double":
                case "java.math.BigInteger":
                case "java.math.BigDecimal":
                case "boolean":
                case "java.lang.Boolean":
                    return true;
                default:
                    return false;
            }
        }

        /**
         * 基本类型转化为包装类型
         *
         * @param classType 基本类型
         */
        public static String primitiveToBox(String classType) {
            switch (classType) {
                case "char":
                    return "java.lang.Character";
                case "short":
                    return "java.lang.Short";
                case "int":
                    return "java.lang.Integer";
                case "long":
                    return "java.lang.Long";
                case "float":
                    return "java.lang.Float";
                case "double":
                    return "java.lang.Double";
                case "boolean":
                    return "java.lang.Boolean";
                default:
            }
            return classType;
        }

        @NotNull
        public static Object getExampleValue(String parameterType, boolean isRandom) {
            if (parameterType.isEmpty()) {
                return "";
            }
            if (parameterType.lastIndexOf(".") > 0) {
                parameterType = parameterType.substring(parameterType.lastIndexOf(".") + 1);
            }
            String type = parameterType.replace("PsiType:", "");
            switch (type) {
                case "byte":
                case "Byte":
                    return Byte.valueOf("1");
                case "char":
                    return 'a';
                case "String":
                    return isRandom ? "str" : "";
                case "Date":
                case "Timestamp":
                case "LocalDate":
                case "LocalDateTime":
                    return System.currentTimeMillis();
                case "short":
                case "Short":
                case "int":
                case "Integer":
                case "long":
                case "Long":
                case "BigInteger":
                case "BigDecimal":
                    return 0;
                case "float":
                case "Float":
                case "double":
                case "Double":
                    return 0.0;
                case "boolean":
                case "Boolean":
                    return true;
                default:
                    return "";
            }
        }
    }
}