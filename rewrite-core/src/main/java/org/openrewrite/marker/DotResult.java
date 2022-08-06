/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Incubating;

import java.util.UUID;

/**
 * A Search Result matching a <link href="https://en.wikipedia.org/wiki/DOT_(graph_description_language)">DOT Format</link>.
 */
@Incubating(since = "7.27.2")
public class DotResult extends SearchResult {
    public DotResult(UUID id, String dotResult) {
        super(id, dotResult);
    }
}
