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
package org.openrewrite;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Describes an option that can be set on a recipe, declared on a field of the recipe class itself, typically
/// one whose getter and constructor Lombok generates. Options on a superclass's fields are not discovered, so a
/// subclass declares each of its options as a field of its own.
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
    @Language("markdown") @NlsRewrite.DisplayName String displayName() default "";

    @Language("markdown") @NlsRewrite.Description String description() default "";

    String example() default "";

    String[] valid() default "";

    boolean required() default true;

    /**
     * Whether the option's value is a secret, such as a password or token. A secret's value is still given
     * to the recipe, but never shown in its instance name or recorded with what it changed, and tools that
     * display, store or export a recipe's options mask it.
     */
    boolean secret() default false;
}
