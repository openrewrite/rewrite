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
package org.openrewrite.xml.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks an {@link org.openrewrite.xml.tree.Xml.Tag} that is an HTML
 * <a href="https://developer.mozilla.org/en-US/docs/Glossary/Void_element">void element</a>
 * written without a self-closing slash (e.g. {@code <br>} rather than {@code <br/>}).
 * <p>
 * Such a tag has no content and no closing tag, but unlike a self-closing tag it is printed
 * with a bare {@code >} delimiter rather than {@code />}. The marker lets the printer
 * distinguish {@code <br>} from {@code <br/>} while keeping both representable as a
 * {@code Tag} with {@code null} content and {@code null} closing.
 */
@Value
@With
public class HtmlVoidElement implements Marker {
    UUID id;
}
