package org.openrewrite.java.style;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
public class PreserveImportLayoutStyle extends ImportLayoutStyle {

    public PreserveImportLayoutStyle() {
        super(0, 0, Collections.emptyList(), Collections.emptyList());
    }
    @Override
    public List<JRightPadded<J.Import>> addImport(List<JRightPadded<J.Import>> originalImports, J.Import toAdd,
                                                  J.@Nullable Package pkg,
                                                  Collection<JavaType.FullyQualified> classpath) {
        JRightPadded<J.Import> paddedImport = new JRightPadded<>(toAdd, Space.EMPTY, Markers.EMPTY);
        if (!originalImports.isEmpty()) {
            paddedImport = paddedImport.withElement(paddedImport.getElement().withPrefix(Space.format("\n")));
        }
        final List<JRightPadded<J.Import>> newImports = new ArrayList<>(originalImports);
        newImports.add(paddedImport);
        return newImports;
    }
}

