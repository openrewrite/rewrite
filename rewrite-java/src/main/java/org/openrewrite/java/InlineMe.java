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
package org.openrewrite.java;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that calls to the annotated method or constructor can be replaced with the provided replacement template.
 * <p/>
 * This annotation is typically used to mark methods or constructors that are deprecated and have a preferred alternative
 * implementation. The replacement template should provide a way to achieve the same functionality as the annotated
 * method or constructor, but using a different approach.
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface InlineMe {
    /**
     * A Java template that can be used to replace calls to the annotated method. Typically starts with {@code this.}
     * for method invocations in the same class, or {@code this(}, for constructor invocations in the same class.
     * <p/>
     * Replacement templates may refer to elements from other classes, provided the necessary imports are passed in.
     * <p/>
     * Replacement templates can reference method parameters by name, and can also reference {@code this}.
     *
     * @return a Java template that can be used to replace calls to the annotated method.
     */
    String replacement();

    /**
     * Optional imports required by the replacement template.
     *
     * @return imports to be used when compiling the replacement template, and added to the source file if necessary.
     */
    String[] imports() default {};

    /**
     * Optional static imports required by the replacement template.
     *
     * @return static imports to be used when compiling the replacement template, and added to the source file if necessary.
     */
    String[] staticImports() default {};
}
