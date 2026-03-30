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
package org.openrewrite.yaml.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ToBeRemoved;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
public class IndentsStyle implements YamlStyle {
    int indentSize;

    @Getter(AccessLevel.NONE)
    @Nullable Boolean indentedSequences;

    @JsonCreator
    public IndentsStyle(int indentSize, @Nullable Boolean indentedSequences) {
        this.indentSize = indentSize;
        this.indentedSequences = indentedSequences;
    }

    @Deprecated
    @ToBeRemoved(after = "2026-06-01", reason = "All parent runtimes have had few weeks to update")
    public IndentsStyle(int indentSize) {
        this(indentSize, null);
    }

    public boolean isIndentedSequences() {
        return indentedSequences == null || indentedSequences;
    }
}
