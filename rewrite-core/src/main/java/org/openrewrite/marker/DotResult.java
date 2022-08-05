package org.openrewrite.marker;

import lombok.With;

import java.util.UUID;

/**
 * A Search Result matching a <link href="https://en.wikipedia.org/wiki/DOT_(graph_description_language)">DOT Format</link>.
 */
@With
public class DotResult extends SearchResult {
    public DotResult(UUID id, String dotResult) {
        super(id, dotResult);
    }
}
