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
package org.openrewrite.java.style;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
public class PreserveImportLayoutStyle extends ImportLayoutStyle {

    final List<JRightPadded<J.Import>> existingImports;

    public PreserveImportLayoutStyle(List<JRightPadded<J.Import>> imports) {
        super(0, 0, Collections.emptyList(), Collections.emptyList());
        this.existingImports = imports;
    }

    public List<JRightPadded<J.Import>> addImports(List<J.Import> toAdd) {
        List<JRightPadded<J.Import>> paddings = new ArrayList<>();
        Space prefix = Space.format("\n");
        if (existingImports.isEmpty()) {
            prefix = Space.EMPTY;
        }

        boolean first = true;
        for (J.Import anImport : toAdd) {
            if (!first) {
                prefix = Space.format("\n");
            }
            JRightPadded<J.Import> paddedImport = new JRightPadded<>(anImport, Space.EMPTY, Markers.EMPTY);
            paddedImport = paddedImport.withElement(paddedImport.getElement().withPrefix(prefix));
            paddings.add(paddedImport);
            first = false;

        }
        final List<JRightPadded<J.Import>> newImports = new ArrayList<>(existingImports);
        newImports.addAll(paddings);
        return newImports;
    }
}

