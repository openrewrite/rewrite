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
package org.openrewrite.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates that the element should be removed after a certain point.
 * Intended to be used with a matching recipe to clear out elements on a schedule.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface RemoveAfter {
    /**
     * Expects a date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03.
     * @return The date after which this element should be removed.
     */
    String date();
}
