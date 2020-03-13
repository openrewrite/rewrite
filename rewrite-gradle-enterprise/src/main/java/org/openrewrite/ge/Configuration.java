package org.openrewrite.ge;

import lombok.Getter;
import lombok.Setter;
import org.openrewrite.internal.lang.Nullable;

@Getter
@Setter
public class Configuration {
    private GithubConfiguration github;
    private String gradleEnterpriseServer;
    private String extensionVersion;
}

@Getter
@Setter
class GithubConfiguration {
    /**
     * A GitHub Enterprise URL would look like http://ghe.acme.com/api/v3.
     */
    private String endpoint = "https://api.github.com";

    /**
     * Can be provided as a command-line option instead.
     */
    @Nullable
    private String username;

    /**
     * Can be provided as a command-line option instead.
     */
    @Nullable
    private String password;

    private String organization;
}
