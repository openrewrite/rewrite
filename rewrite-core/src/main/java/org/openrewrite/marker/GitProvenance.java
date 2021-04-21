/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.marker;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.lib.*;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.0.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class GitProvenance implements Marker {
    UUID id;
    @Nullable
    String origin;

    String branch;
    String change;

    @Nullable
    public String getOrganizationName() {
        if(origin == null) {
            return null;
        }
        if (origin.startsWith("git")) {
            return origin.substring(origin.indexOf(':') + 1, origin.indexOf('/'));
        } else {
            String path = URI.create(origin).getPath();
            return path.substring(1, path.indexOf('/', 1));
        }
    }

    @Nullable
    public String getRepositoryName() {
        if(origin == null) {
            return null;
        }
        if (origin.startsWith("git")) {
            return origin.substring(origin.lastIndexOf('/') + 1).replaceAll("\\.git$", "");
        } else {
            String path = URI.create(origin).getPath();
            return path.substring(path.lastIndexOf('/') + 1).replaceAll("\\.git$", "");
        }
    }

    public static GitProvenance fromProjectDirectory(Path projectDir) {
        try {
            Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build();
            return new GitProvenance(randomId(), getOrigin(repository), repository.getBranch(), getChangeset(repository));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nullable
    private static String getOrigin(Repository repository) {
        Config storedConfig = repository.getConfig();
        String url = storedConfig.getString("remote", "origin", "url");
        if (url == null) {
            return null;
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            url = hideSensitiveInformation(url);
        }
        return url;
    }

    @Nullable
    private static String getChangeset(Repository repository) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return null;
        }
        return head.getName();
    }

    private static String hideSensitiveInformation(String url) {
        try {
            String credentials = URI.create(url).toURL().getUserInfo();
            if (credentials != null) {
                return url.replaceFirst(credentials, credentials.replaceFirst(":.*", ""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to remove credentials from repository URL. {0}", e);
        }
        return url;
    }
}
