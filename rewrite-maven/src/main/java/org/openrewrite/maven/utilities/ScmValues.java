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
package org.openrewrite.maven.utilities;

public class ScmValues {

    private String url;
    private String connection;
    private String developerConnection;

    ScmValues(String url, String connection, String developerConnection) {
        this.url = url;
        this.connection = connection;
        this.developerConnection = developerConnection;
    }

    public static ScmValues fromOrigin(String origin) {
        String cleanOrigin = origin.replaceAll("\\.git$", "");

        String url;
        String connection;
        String developerConnection;

        if (origin.startsWith("git@")) {
            // SSH origin
            String hostAndPath = cleanOrigin.substring("git@".length()).replaceFirst(":", "/");
            url = "https://" + hostAndPath;
            connection = "scm:git:https://" + hostAndPath + ".git";
            developerConnection = "scm:git:" + origin;
        } else if (origin.startsWith("http://") || origin.startsWith("https://")) {
            // HTTPS origin
            url = cleanOrigin;
            connection = "scm:git:" + origin;
            String sshPath = cleanOrigin
                    .replaceFirst("^https?://", "") // github.com/user/repo
                    .replaceFirst("/", ":");        // github.com:user/repo
            developerConnection = "scm:git:git@" + sshPath + ".git";
        } else {
            url = cleanOrigin;
            connection = "scm:git:" + origin;
            developerConnection = "scm:git:" + origin;
        }

        return new ScmValues(url, connection, developerConnection);
    }

    public String getUrl() {
        return url;
    }

    public String getConnection() {
        return connection;
    }

    public String getDeveloperConnection() {
        return developerConnection;
    }
}
