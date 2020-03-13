package org.openrewrite.git;

import org.openrewrite.Metadata;

public enum GitMetadata implements Metadata {
    HEAD_COMMIT_ID,
    HEAD_TREE_ID,
    BRANCH,
    REMOTE
}
