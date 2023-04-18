/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.nullability;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.util.Set;

public interface NullabilityAnnotation {

    enum Nullability {
        NULLABLE,
        NONNULL,
        UNKNOWN;
    }

    enum Target {
        TYPE,
        PACKAGE,
        MODULE,
        TYPE_USE,
        METHOD,
        PARAMETER,
        FIELD,
        LOCAL_FIELD;
    }

    enum Scope {
        FIELD,
        METHOD,
        PARAMETER;
    }

    /**
     * Fully qualified name of this annotation
     */
    String getFqn();

    default String getSimpleName() {
        return StringUtils.substringAfterLast(getFqn(), ".");
    }

    /**
     * Whether this annotation indicates whether an element can be null or not.
     */
    Nullability getNullability();

    /**
     * Defines on what elements this nullability annotation ca be used.
     * @see ElementType
     */
    Set<Target> getTargets();

    /**
     * Defines on what elements this nullability annotation applies.
     */
    Set<Scope> getScopes();
}
