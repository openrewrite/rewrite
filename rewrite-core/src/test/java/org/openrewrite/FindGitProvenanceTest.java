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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.marker.GitProvenance.AutoCRLF.False;
import static org.openrewrite.marker.GitProvenance.EOL.Native;
import static org.openrewrite.test.SourceSpecs.text;

class FindGitProvenanceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindGitProvenance());
    }

    @Test
    void showGitProvenance() {
        rewriteRun(
          text("Hello, World!", """
              ~~(GitProvenance:
                  origin: https://github.com/openrewrite/rewrite
                  branch: main
                  changeset: 1234567
                  autocrlf: False
                  eol: Native)~~>Hello, World!""",
            spec -> spec.markers(new GitProvenance(Tree.randomId(), "https://github.com/openrewrite/rewrite", "main", "1234567", False, Native))
          )
        );
    }
}
