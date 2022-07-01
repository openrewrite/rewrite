package org.openrewrite.cobol.tree;

import org.openrewrite.marker.Markers;

public class DottedKeyword extends CobolRightPadded<Space> {
    public DottedKeyword(Space element, Space after, Markers markers) {
        super(element, after, markers);
    }
}
