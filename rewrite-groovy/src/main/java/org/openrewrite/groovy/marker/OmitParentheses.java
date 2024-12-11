/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * See the Groovy style guide on <a href="http://www.groovy-lang.org/style-guide.html#_omitting_parentheses">Omitting parentheses</a>.
 *
 * @deprecated Use {@link org.openrewrite.java.marker.OmitParentheses} instead.
 */
@Value
@With
@Deprecated
public class OmitParentheses implements Marker {
    UUID id;
}
