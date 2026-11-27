/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Set;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.GitProvenance.AutoCRLF.False;
import static org.openrewrite.marker.GitProvenance.EOL.Native;
import static org.openrewrite.test.SourceSpecs.text;

class IsInRepositoryTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IsInRepository(Set.of("rewrite")));
    }

    @DocumentExample
    @Test
    void markFileInAllowedRepository() {
        rewriteRun(
          text(
            "hello",
            "~~>hello",
            spec -> spec.markers(gitProvenance("https://github.com/openrewrite/rewrite"))
          )
        );
    }

    @Test
    void ignoreFileInOtherRepository() {
        rewriteRun(
          text(
            "hello",
            spec -> spec.markers(gitProvenance("https://github.com/openrewrite/rewrite-spring"))
          )
        );
    }

    private static GitProvenance gitProvenance(String origin) {
        return new GitProvenance(randomId(), origin, "main", "1234567", False, Native, emptyList());
    }
}
