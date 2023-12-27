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
package org.openrewrite.internal.lang;

import org.openrewrite.Option;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class NullUtils {

    /**
     * A list of package-level annotation names that specifies the default for member fields is non-null. The matching
     * logic is not sophisticated, if there is an annotation (with runtime retention) on a package matching one of the
     * simple names, it is assumed non-null is the default for all class members contained in the package.
     * <P><P>
     * NOTE: Classes in subpackages are NOT included.
     * <P><P>
     * Supported:
     * <li>org.openrewrite.internal.lang.NonNullFields</li>
     * <li>org.springframework.lang.NonNullFields</li>
     */
    private static final List<String> PACKAGE_LEVEL_NON_NULL_ANNOTATIONS = Collections.singletonList(
            "NonNullFields"
    );

    /**
     * A list of field-level annotation names that indicate a field is non-null. The matching logic is not
     * sophisticated, if there is an annotation (with runtime retention) on a member field matching one of the simple
     * names, it is assumed non-null.
     * <P><P>
     * Examples of matching annotations:
     *
     * <li>org.openrewrite.internal.lang.NonNull</li>
     * <li>org.springframework.lang.NonNull</li>
     * <li>javax.annotations.Nonnull</li>
     * <li>org.checkerframework.checker.nullness.qual.NonNull</li>
     * <li>javax.validation.constraints.NotNull</li>
     */
    private static final List<String> FIELD_LEVEL_NON_NULL_ANNOTATIONS = Arrays.asList(
            "NonNull",
            "Nonnull",
            "NotNull"
            );

    /**
     * A list of field-level annotation names that indicate a field is Nullable. The matching logic is not
     * sophisticated, if there is an annotation (with runtime retention) on a member field matching one of the
     * simple names, it is assumed nullable.
     * <P><P>
     * Supported:
     *
     * <li>org.openrewrite.internal.lang.Nullable</li>
     * <li>org.springframework.lang.Nullable</li>
     * <li>javax.annotations.Nullable</li>
     * <li>org.checkerframework.checker.nullness.qual.Nullable</li>
     */
    private static final List<String> FIELD_LEVEL_NULLABLE_ANNOTATIONS = Collections.singletonList(
            "Nullable"
    );

    /**
     * The method uses reflection to find all declared fields of a class that have been marked (via commonly used
     * annotations) as being Non-Null, or a required {@link Option}.
     * This method will also look at the class's package level to see if the API
     * for that package is defaulted as Non-Null.
     * Fields with explicit Nullable annotations will be excluded.
     * Any other annotation that has runtime retention and matches one of the simple
     * annotation names (minus any package) will be considered a match.
     *
     * @param _class The class to reflect over
     * @return A list of fields marked as non-null, sorted by their name in alphabetical order.
     */
    public static List<Field> findNonNullFields(@NonNull Class<?> _class) {

        boolean defaultNonNull = Arrays.stream(_class.getPackage().getDeclaredAnnotations())
                .map(a -> a.annotationType().getSimpleName())
                .anyMatch(PACKAGE_LEVEL_NON_NULL_ANNOTATIONS::contains);

        Field[] fields = _class.getDeclaredFields();
        if (fields.length == 0) {
            return Collections.emptyList();
        }

        List<Field> nonNullFields = new ArrayList<>(fields.length);
        for (Field field : fields) {
            field.setAccessible(true);
            if (fieldHasNullableAnnotation(field)) {
                continue;
            }
            if (defaultNonNull || fieldHasNonNullableAnnotation(field) || fieldIsRequiredOption(field)) {
                nonNullFields.add(field);
            }
        }
        nonNullFields.sort(Comparator.comparing(Field::getName));
        return nonNullFields;
    }

    private static boolean fieldIsRequiredOption(Field field) {
        Option annotation = field.getAnnotation(Option.class);
        if (annotation != null)
            return annotation.required();
        return false;
    }

    private static boolean fieldHasNonNullableAnnotation(Field field) {
        for (Annotation a : field.getDeclaredAnnotations()) {
            String simpleName = a.annotationType().getSimpleName();
            if (FIELD_LEVEL_NON_NULL_ANNOTATIONS.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }
    private static boolean fieldHasNullableAnnotation(Field field) {
        for (Annotation a : field.getDeclaredAnnotations()) {
            String simpleName = a.annotationType().getSimpleName();
            if (FIELD_LEVEL_NULLABLE_ANNOTATIONS.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
