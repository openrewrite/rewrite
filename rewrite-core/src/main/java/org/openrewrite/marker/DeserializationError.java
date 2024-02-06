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
package org.openrewrite.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.ExceptionUtils;

import java.util.UUID;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
@Getter
@ToString
public class DeserializationError implements Marker {
    @EqualsAndHashCode.Include
    UUID id;
    String message;
    String detail;

    @SuppressWarnings("unused")
    public DeserializationError(UUID id, String message, Throwable cause) {
        this.id = id;
        this.message = message;
        this.detail = ExceptionUtils.sanitizeStackTrace(cause, Object.class);
    }

    @JsonCreator
    DeserializationError(UUID id, String message, String detail) {
        this.id = id;
        this.message = message;
        this.detail = detail;
    }
}
