/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be added to declarations which are to be removed after some given date in the future, which should, where
 * applicable, also account for LST models that were serialized in the past to not cause deserialization errors.
 * <p>
 * For code blocks or single lines of code which can't be annotated using this annotation, the code should instead
 * be commented like in this example:
 * <pre>
 * // TO-BE-REMOVED(2025-12-31): for unexplainable reasons this code is required in production
 * for (int i = 0; i != 42; i++) {
 *     System.out.println(i);
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface ToBeRemoved {
    /**
     * A date string in ISO format like "2025-12-31".
     */
    String after();

    String reason() default "";
}
