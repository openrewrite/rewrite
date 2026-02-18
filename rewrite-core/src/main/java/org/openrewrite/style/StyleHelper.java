/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.style;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

public class StyleHelper {

    private static final Set<Class<?>> primitiveWrapperClasses = new HashSet<>();

    static {
        primitiveWrapperClasses.add(Double.class);
        primitiveWrapperClasses.add(Float.class);
        primitiveWrapperClasses.add(Long.class);
        primitiveWrapperClasses.add(Integer.class);
        primitiveWrapperClasses.add(Short.class);
        primitiveWrapperClasses.add(Character.class);
        primitiveWrapperClasses.add(Byte.class);
        primitiveWrapperClasses.add(Boolean.class);
    }

    private static boolean isPrimitiveOrWrapper(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive() || primitiveWrapperClasses.contains(type);
    }

    private static boolean isEnum(Object value) {
        Class<?> type = value.getClass();
        return type.isEnum();
    }

    @lombok.Value
    private static class FieldAccessor {
        Method getter;
        Method wither;
    }

    private static final ClassValue<List<FieldAccessor>> FIELD_ACCESSORS = new ClassValue<List<FieldAccessor>>() {
        @Override
        protected List<FieldAccessor> computeValue(Class<?> styleClass) {
            List<FieldAccessor> accessors = new ArrayList<>();
            for (Field f : styleClass.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }
                try {
                    Method wither = styleClass.getMethod("with" + StringUtils.capitalize(f.getName()), f.getType());
                    Method getter = styleClass.getMethod("get" + StringUtils.capitalize(f.getName()));
                    accessors.add(new FieldAccessor(getter, wither));
                } catch (NoSuchMethodException ignored) {
                }
            }
            return accessors;
        }
    };

    /**
     * Copies all non-null properties from right into left, recursively. Assumes use of @With from project lombok.
     *
     * @param left  left object, target of merged properties
     * @param right right object, source of merged properties
     * @param <T>   Type of left and right
     * @return left object with merged properties from right
     */
    public static <T> T merge(T left, T right) {
        Class<?> styleClass = left.getClass();
        if (right.getClass() != styleClass) {
            throw new RuntimeException(left.getClass().getName() + " and " + right.getClass().getName() + " should match exactly.");
        }
        for (FieldAccessor accessor : FIELD_ACCESSORS.get(styleClass)) {
            try {
                Object rightValue = accessor.getGetter().invoke(right);
                if (rightValue != null) {
                    if (!isPrimitiveOrWrapper(rightValue) && !isEnum(rightValue)) {
                        Object leftValue = accessor.getGetter().invoke(left);
                        if (leftValue instanceof Collection) {
                            if (!((Collection<?>) leftValue).isEmpty()) {
                                rightValue = merge(leftValue, rightValue);
                            }
                        } else {
                            rightValue = merge(leftValue, rightValue);
                        }
                    }
                    //noinspection unchecked
                    left = (T) accessor.getWither().invoke(left, rightValue);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return left;
    }

    public static <T extends SourceFile> T addStyleMarker(T t, List<NamedStyles> styles) {
        if (!styles.isEmpty()) {
            Set<NamedStyles> newNamedStyles = new HashSet<>(styles);
            boolean styleAlreadyPresent = false;
            for (NamedStyles namedStyle : t.getMarkers().findAll(NamedStyles.class)) {
                styleAlreadyPresent = !newNamedStyles.add(namedStyle) || styleAlreadyPresent;
            }
            // As the order or NamedStyles matters, we cannot simply use addIfAbsent.
            if (!styleAlreadyPresent) {
                Markers markers = t.getMarkers().removeByType(NamedStyles.class);
                for (NamedStyles namedStyle : newNamedStyles) {
                    markers = markers.add(namedStyle);
                }

                return t.withMarkers(markers);
            }
        }
        return t;
    }

    public static <S extends Style> S getStyle(Class<S> styleClass, List<NamedStyles> styles, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(defaultStyle.get(), style);
        }
        return defaultStyle.get();
    }

    public static <S extends Style, T extends SourceFile> @Nullable S getStyle(Class<S> styleClass, List<NamedStyles> styles, T sourceFile) {
        S projectStyle = Style.from(styleClass, sourceFile);
        S style = NamedStyles.merge(styleClass, styles);
        if (projectStyle == null) {
            return style;
        }
        if (style != null) {
            return StyleHelper.merge(projectStyle, style);
        }
        return projectStyle;
    }

    public static <S extends Style, T extends SourceFile> S getStyle(Class<S> styleClass, List<NamedStyles> styles, T sourceFile, Supplier<S> defaultStyle) {
        S projectStyle = Style.from(styleClass, sourceFile, defaultStyle);
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(projectStyle, style);
        }
        return projectStyle;
    }

    public static <S extends Style> @Nullable S getStyle(Class<S> styleClass, List<NamedStyles> styles) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return (S) style.applyDefaults();
        }
        return null;
    }

    public static NamedStyles fromStyles(Style... styles) {
        return new NamedStyles(Tree.randomId(),
                "MergedStyles",
                "Merged styles",
                "Merged styles from " + Arrays.stream(styles).map(Object::getClass).map(Class::getSimpleName).collect(joining(", ")),
                emptySet(),
                Arrays.asList(styles));
    }
}
