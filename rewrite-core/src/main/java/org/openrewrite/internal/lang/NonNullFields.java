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

import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.*;

/**
 * A common annotation to declare that fields are to be considered as
 * non-nullable by default for a given package.
 * <p>Leverages JSpecify meta-annotations to indicate nullability in Java to common
 * tools with JSpecify support and used by Kotlin to infer nullability of the API.
 * <p>Should be used at package level in association with {@link org.jspecify.annotations.Nullable}
 * annotations at field level.
 *
 * @see NonNullFields
 * @see org.jspecify.annotations.NonNull
 * @see org.jspecify.annotations.Nullable
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@org.jspecify.annotations.NonNull
@TypeQualifierDefault(ElementType.FIELD)
public @interface NonNullFields {
}
