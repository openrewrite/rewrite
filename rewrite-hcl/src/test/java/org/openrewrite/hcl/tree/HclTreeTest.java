/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.tree;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.TreeSerializer;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.internal.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

public interface HclTreeTest {
    default void assertParsePrintAndProcess(HclParser parser, String code) {
        Hcl.ConfigFile cf = parser.parse(
                new InMemoryExecutionContext(t -> {
                    throw new RuntimeException(t.getMessage(), t);
                }),
                code
        ).iterator().next();

        Hcl processed = new HclVisitor<>().visit(cf, new Object());
        assertThat(processed).as("Processing is idempotent").isSameAs(cf);

        TreeSerializer<Hcl.ConfigFile> treeSerializer = new TreeSerializer<>();
        Hcl.ConfigFile roundTripCf = treeSerializer.read(treeSerializer.write(cf));

        assertThat(roundTripCf.print())
                .as("Source code is printed the same after parsing")
                .isEqualTo(StringUtils.trimIndent(code));
    }
}
