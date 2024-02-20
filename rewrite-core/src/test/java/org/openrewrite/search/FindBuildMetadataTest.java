/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.test.RewriteTest;

import java.util.Map;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;

class FindBuildMetadataTest implements RewriteTest {

    @Test
    void findBuildMetadata() {
        rewriteRun(
          spec -> spec.recipe(new FindBuildMetadata("lstFormatVersion", "2")),
          text(
            "hello world",
            "~~(Found build metadata)~~>hello world",
            spec -> spec.mapBeforeRecipe(pt -> pt.withMarkers(pt.getMarkers().add(new BuildMetadata(randomId(),
              Map.of("lstFormatVersion", "2")))))
          )
        );
    }
}
