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

public class AzureDevOpsScm implements Scm {

    @Override
    public String getOrigin() {
        return "dev.azure.com";
    }

    @Override
    public String cleanHostAndPath(String url) {
        UrlComponents uri = UrlComponents.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        if (uri.isSsh() && host.startsWith("ssh.")) {
            host = host.substring(4);
            path = path.replaceFirst("v3/", "");
        } else {
            path = path.replaceFirst("_git/", "");
        }
        String hostAndPath = host + "/" + path
                .replaceFirst("\\.git$", "");
        return hostAndPath.replaceFirst("/$", "");
    }

    @Override
    public CloneUrl parseCloneUrl(String cloneUrl) {
        CloneUrl parsed = Scm.super.parseCloneUrl(cloneUrl);
        return new AzureDevopsCloneUrl(parsed.getOrigin(), parsed.getPath());
    }
}
