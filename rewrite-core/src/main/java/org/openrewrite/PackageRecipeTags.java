/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.config.RecipeDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A package-level annotation that can be used to tag all {@link Recipe} within a given package and children
 * packages with a given tag.
 * <p>
 * This annotation is useful for tagging all recipes within a package with a given tag, without having to
 * add it manually to each overridden {@link Recipe#getTags()} method.
 * <p>
 * The tag will not be accessible via {@link Recipe#getTags()}, but will be accessible via {@link RecipeDescriptor#getTags()}
 * when the {@link RecipeDescriptor} is created via {@link Recipe#getDescriptor()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE})
public @interface PackageRecipeTags {
    String[] value() default {};
}
