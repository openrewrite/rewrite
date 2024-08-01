package org.openrewrite.scm;

import lombok.Builder;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

@Value
@Builder
public class ScmUrlComponents {

    @Nullable
    String origin;

    @Nullable
    String organization;

    @Nullable
    String groupPath;

    @Nullable
    String project;

    String repositoryName;

    public String getPath() {
        if (groupPath != null) {
            return groupPath + "/" + repositoryName;
        }
        if (organization != null) {
            if (project != null) {
                return organization + "/" + project + "/" + repositoryName;
            }
            return organization + "/" + repositoryName;
        }
        return repositoryName;
    }
}
