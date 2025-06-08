/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.parser.data;

import java.lang.annotation.ElementType;

@BasicAnnotation(
    intValue = 100,
    longValue = 200L,
    floatValue = 1.5f,
    doubleValue = 2.5,
    boolValue = false,
    charValue = 'Y',
    byteValue = 10,
    shortValue = 20
)
@StringAnnotation(
    value = "Custom value",
//    escaped = "Testing \"quotes\" and \\ backslashes",
    unicode = "More unicode: \u2764\uFE0F"
//    multiLine = "Line 1\nLine 2\nLine 3"
)
@NestedAnnotation(
    basic = @BasicAnnotation(intValue = 999),
    string = @StringAnnotation(value = "Nested string"),
    nested = @NestedLevel2(
        value = "Custom nested",
        deepNested = @NestedLevel3(value = "Deep nested custom")
    )
)
@ArrayAnnotation(
    strings = {"A", "B", "C"},
    ints = {10, 20, 30},
    classes = {String.class, Integer.class, Object.class},
    annotations = {
        @BasicAnnotation(intValue = 1),
        @BasicAnnotation(intValue = 2)
    }
)
@ClassRefAnnotation(
    value = Integer.class,
    primitive = boolean.class,
    array = String[].class,
    innerClass = AnnotatedClass.InnerClass.class
)
@EnumAnnotation(
    value = TestEnum.THREE,
    values = {TestEnum.TWO, TestEnum.THREE},
    elementType = ElementType.FIELD
)
@ConstantAnnotation(
    value = Constants.STRING_CONSTANT,
    number = Constants.INT_CONSTANT,
    decimal = Constants.DOUBLE_CONSTANT
)
public class AnnotatedClass {

    @BasicAnnotation
    private String field;

    @StringAnnotation
    public void method() {
        // Method body
    }

    public static class InnerClass {
        // Inner class for testing class references
    }
}
