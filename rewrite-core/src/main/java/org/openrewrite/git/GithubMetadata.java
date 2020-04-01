package org.openrewrite.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.Metadata;

public enum GithubMetadata implements Metadata {
    REPOSITORY,
    ORGANIZATION
}
