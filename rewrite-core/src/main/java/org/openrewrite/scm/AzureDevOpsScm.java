/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.scm;

import org.openrewrite.internal.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureDevOpsScm implements Scm {
    private static final Pattern AZURE_DEVOPS_HTTP_REMOTE =
            Pattern.compile("https://(?:[\\w-]+@)?dev\\.azure\\.com/([^/]+)/([^/]+)/_git/(.*)");
    private static final Pattern AZURE_DEVOPS_SSH_REMOTE =
            Pattern.compile("(?:ssh://)?git@ssh\\.dev\\.azure\\.com:v3/([^/]+)/([^/]+)/(.*)");

    @Override
    public String getOrigin() {
        return "dev.azure.com";
    }

    @Override
    public String cleanHostAndPath(String url) {
        Matcher matcher = getUrlMatcher(url);
        if (matcher != null) {
            return getOrigin() + '/' + matcher.group(1) + '/' + matcher.group(2) + '/' + matcher.group(3);
        }
        return url;
    }

    @Override
    public CloneUrl parseCloneUrl(String cloneUrl) {
        Matcher matcher = getUrlMatcher(cloneUrl);
        if (matcher == null) {
            throw new IllegalArgumentException("Unable to parse Azure DevOps repository URL: " + cloneUrl);
        }
        return new AzureDevOpsCloneUrl(cloneUrl, getOrigin(), matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private @Nullable Matcher getUrlMatcher(String cloneUrl) {
        Matcher matcher = AZURE_DEVOPS_HTTP_REMOTE.matcher(cloneUrl);
        if (!matcher.matches()) {
            matcher = AZURE_DEVOPS_SSH_REMOTE.matcher(cloneUrl);
        }
        return matcher.matches() ? matcher : null;
    }
}
