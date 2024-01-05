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
package org.openrewrite.protobuf;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoParserTest implements RewriteTest {
    @Test
    void noNullsForProto3Files() {
        List<SourceFile> sources = ProtoParser.builder().build().parse("syntax = \"proto3\";").toList();
        assertThat(sources).singleElement().isInstanceOf(PlainText.class);
    }
}
