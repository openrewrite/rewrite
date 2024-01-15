package org.openrewrite.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;

import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * A marker that indicates an extra message to be appended to the message of any commit generated from the recipe run.
 */
@Value
public class CommitMessage implements Marker {
    @With
    UUID id;
    String recipeName;
    String message;

    public static <T extends Tree> T message(Tree t, Recipe r, String message) {
        return t.withMarkers(t.getMarkers().add(new CommitMessage(randomId(), r.getName(), message)));
    }
}
