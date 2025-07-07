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
package org.openrewrite.kotlin.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * The construction of new annotations does not require the `@` character.
 * Example:
 *  @Tags(
 *      value = [
 *          Tag(value = "Sample01"),
 *          Tag(value = "Sample02"),
 *      ]
 *  )
 */
@SuppressWarnings("JavadocDeclaration")
@Value
@With
public class AnnotationConstructor implements Marker {
    UUID id;

    public AnnotationConstructor(UUID id) {
        this.id = id;
    }
}
