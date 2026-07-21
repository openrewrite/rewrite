/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers over {@link UvLockRequirement} shared by the metadata build and the diff:
 * the optional-dependency group a marker gates on, and a stable textual rendering used to
 * compare requirement runs as a multiset.
 */
final class UvLockRequirements {

    /**
     * Sentinel group for markers gating on more than one extra, which cannot be
     * attributed to a single optional-dependencies group.
     */
    static final String MULTI_EXTRA = "\0multi-extra";

    private static final Pattern EXTRA_CLAUSE = Pattern.compile("extra == '([^']+)'");

    private UvLockRequirements() {
    }

    static @Nullable String extraGroupOf(UvLockRequirement req) {
        if (req.getMarker() == null) {
            return null;
        }
        Matcher m = EXTRA_CLAUSE.matcher(req.getMarker());
        if (!m.find()) {
            return null;
        }
        String first = m.group(1);
        return m.find() ? MULTI_EXTRA : first;
    }

    static List<String> rendered(List<UvLockRequirement> reqs) {
        List<String> rendered = new ArrayList<>(reqs.size());
        for (UvLockRequirement req : reqs) {
            rendered.add(req.getName() + "|" + req.getExtras() + "|" + req.getEditable() + "|" +
                    req.getMarker() + "|" + req.getSpecifier() + "|" + req.getIndex() + "|" +
                    req.getUrl() + "|" + req.getGit() + "|" + req.getDirectory());
        }
        return rendered;
    }
}
