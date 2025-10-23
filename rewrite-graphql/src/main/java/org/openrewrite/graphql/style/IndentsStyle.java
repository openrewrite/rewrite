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
package org.openrewrite.graphql.style;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.style.Style;

@AllArgsConstructor
@Getter
@With
public class IndentsStyle implements Style {
    private final int indentSize;
    private final boolean useTab;

    @Override
    public Style applyDefaults() {
        return this;
    }

    public static IndentsStyle autodetect(GraphQl.Document document) {
        return Autodetect.autodetectIndentStyle(document);
    }
}