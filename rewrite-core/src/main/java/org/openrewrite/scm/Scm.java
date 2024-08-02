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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public interface Scm extends Comparable<Scm> {
    String getOrigin();

    String cleanHostAndPath(String cloneUrl);

    default boolean belongsToScm(String cloneUrl) {
        return cleanHostAndPath(cloneUrl).startsWith(getOrigin());
    }

    default CloneUrl parseCloneUrl(String cloneUrl) {
        if (cloneUrl.length() < getOrigin().length() + 1) {
            return new SimpleCloneUrl(cloneUrl, getOrigin(), "");
        }
        String path = cleanHostAndPath(cloneUrl).substring(getOrigin().length() + 1);
        return new SimpleCloneUrl(cloneUrl, getOrigin(), path);
    }

    @Override
    default int compareTo(Scm o) {
        return getOrigin().compareTo(o.getOrigin());
    }

    Set<Scm> KNOWN_SCM = new LinkedHashSet<>(Arrays.asList(
            new SimpleScm("github.com"),
            new SimpleScm("bitbucket.org"),
            new GitLabScm(),
            new AzureDevOpsScm()
    ));

    static void registerScm(Scm scm) {
        KNOWN_SCM.add(scm);
    }

    static Scm findMatchingScm(String cloneUrl) {
        for (Scm scm : KNOWN_SCM) {
            if (scm.belongsToScm(cloneUrl)) {
                return scm;
            }
        }
        return new UnknownScm(cloneUrl);
    }
}
