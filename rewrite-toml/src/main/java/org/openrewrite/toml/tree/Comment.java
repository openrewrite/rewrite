package org.openrewrite.toml.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Markers;

@Value
@With
public class Comment {
    String text;
    String suffix;
    Markers markers;
}