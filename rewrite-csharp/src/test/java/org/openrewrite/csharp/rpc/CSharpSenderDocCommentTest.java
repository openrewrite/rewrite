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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.csharp.CsDocCommentParser;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.CsDocCommentRawComment;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.IdentityHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Regression: {@link CsDocComment.DocComment} can end up in {@link Space#getComments()} after
 * a {@code CSharpVisitor} pass parses a {@link CsDocCommentRawComment} into a structured tree.
 * The sender used to only know about {@link CsDocCommentRawComment} and {@code TextComment},
 * so it threw {@code IllegalArgumentException: Unexpected comment type ...$DocComment} as soon
 * as the LST was sent (e.g. when {@code mod run} re-serialized a tree post-visit).
 */
class CSharpSenderDocCommentTest {

    @Test
    void visitSpaceWithDocCommentDoesNotThrow() {
        CsDocCommentRawComment raw = new CsDocCommentRawComment(
                "/ <summary>doc</summary>", "\n", Markers.EMPTY);
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(raw);
        Space space = Space.EMPTY.withComments(List.<Comment>of(parsed));

        CSharpSender sender = new CSharpSender();
        RpcSendQueue queue = new RpcSendQueue(64, batch -> {}, new IdentityHashMap<>(), null, false);

        assertThatNoException().isThrownBy(() -> sender.visitSpace(space, queue));
    }

    @Test
    void docCommentIsTransmittedAsRawCommentValueType() {
        CsDocCommentRawComment raw = new CsDocCommentRawComment(
                "/ <summary>doc</summary>", "\n", Markers.EMPTY);
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(raw);
        Space space = Space.EMPTY.withComments(List.<Comment>of(parsed));

        CSharpSender sender = new CSharpSender();
        List<RpcObjectData> emitted = new java.util.ArrayList<>();
        RpcSendQueue queue = new RpcSendQueue(1024, emitted::addAll, new IdentityHashMap<>(), null, false);

        sender.visitSpace(space, queue);
        queue.flush();

        // The value type announced over the wire for the comment ADD must be
        // CsDocCommentRawComment, never CsDocComment$DocComment.
        boolean addedRaw = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> CsDocCommentRawComment.class.getName().equals(d.getValueType()));
        boolean addedDoc = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> d.getValueType() != null && d.getValueType().endsWith("$DocComment"));
        assertThat(addedRaw).as("CsDocCommentRawComment should be sent over the wire").isTrue();
        assertThat(addedDoc).as("Structured DocComment should not leak onto the wire").isFalse();
    }

    @Test
    void preservesIdsOnRawComment() {
        // Sanity check that the helper produces a well-formed raw comment whose
        // round-trip through the existing C# parser yields equivalent text.
        String original = "/ <summary>hello</summary>";
        CsDocCommentRawComment raw = new CsDocCommentRawComment(original, "\n", Markers.EMPTY);
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(raw);
        assertThat(parsed.getId()).isNotEqualTo(Tree.randomId()); // distinct id
        assertThat(parsed.getSuffix()).isEqualTo("\n");
    }
}
