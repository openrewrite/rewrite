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
package org.openrewrite.json.format;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.json.style.WrappingAndBracesStyle;
import org.openrewrite.style.GeneralFormatStyle;

public class WrappingAndBraces extends Recipe {
    private final WrappingAndBracesStyle wrappingAndBracesStyle;
    private final TabsAndIndentsStyle tabsAndIndentsStyle;
    private final GeneralFormatStyle generalFormatStyle;

    public WrappingAndBraces() {
        this.wrappingAndBracesStyle = WrappingAndBracesStyle.DEFAULT;
        this.tabsAndIndentsStyle = TabsAndIndentsStyle.DEFAULT;
        this.generalFormatStyle = GeneralFormatStyle.DEFAULT;
    }

    public WrappingAndBraces(WrappingAndBracesStyle wrappingAndBracesStyle, TabsAndIndentsStyle tabsAndIndentsStyle, GeneralFormatStyle generalFormatStyle) {
        this.wrappingAndBracesStyle = wrappingAndBracesStyle;
        this.tabsAndIndentsStyle = tabsAndIndentsStyle;
        this.generalFormatStyle = generalFormatStyle;
    }

    @Getter
    final String displayName = "JSON new lines";

    @Getter
    final String description = "Split members into separate lines in JSON.";

    @Override
    public WrappingAndBracesVisitor<ExecutionContext> getVisitor() {
        return new WrappingAndBracesVisitor<>(wrappingAndBracesStyle, tabsAndIndentsStyle, generalFormatStyle, null);
    }
}
