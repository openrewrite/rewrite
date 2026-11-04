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
package org.openrewrite.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.text.PlainTextParser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentsTest {

    @Test
    void errorsOnSourceFileWithoutCommentService() {
        SourceFile text = new PlainTextParser().parse("hello").findFirst().orElseThrow();
        Comments comments = Comments.of(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), text));
        assertThatThrownBy(comments::getComments)
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("CommentService");
        assertThatThrownBy(() -> comments.comment(" not supported here"))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("CommentService");
    }

    @Test
    void requiresCursorRootedAtSourceFile() {
        Comments comments = Comments.of(new Cursor(null, Cursor.ROOT_VALUE));
        assertThatThrownBy(() -> comments.comment(" x"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SourceFile");
    }
}
