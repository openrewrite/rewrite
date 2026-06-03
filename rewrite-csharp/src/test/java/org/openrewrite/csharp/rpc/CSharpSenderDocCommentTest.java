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
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.csharp.CsDocCommentParser;
import org.openrewrite.csharp.CsDocCommentPrinter;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.CsDocCommentRawComment;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * A {@link CsDocComment.DocComment} can end up in {@link Space#getComments()} after a
 * {@code CSharpVisitor} pass parses a {@link CsDocCommentRawComment} into a structured tree.
 * Rather than flattening it back to raw text, {@link CSharpSender} decomposes the structured
 * tree over RPC (mirroring {@code Javadoc.DocComment} on the Java side) and {@link CSharpReceiver}
 * reconstructs it, so the tree survives a round trip with full fidelity.
 */
class CSharpSenderDocCommentTest {

    private static final String SOURCE_FILE_TYPE = Cs.CompilationUnit.class.getName();

    @Test
    void visitSpaceWithDocCommentDoesNotThrow() {
        Space space = spaceWithDocComment("/ <summary>doc</summary>");

        CSharpSender sender = new CSharpSender();
        RpcSendQueue queue = new RpcSendQueue(64, batch -> {}, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);

        assertThatNoException().isThrownBy(() -> sender.visitSpace(space, queue));
    }

    @Test
    void docCommentIsDecomposedAsStructuredTree() {
        Space space = spaceWithDocComment("/ <summary>doc</summary>");

        List<RpcObjectData> emitted = new ArrayList<>();
        CSharpSender sender = new CSharpSender();
        RpcSendQueue queue = new RpcSendQueue(1024, emitted::addAll, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);

        sender.visitSpace(space, queue);
        queue.flush();

        // The structured DocComment tree must travel over the wire, never the flattened raw form.
        boolean addedDoc = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> d.getValueType() != null && d.getValueType().endsWith("$DocComment"));
        boolean addedRaw = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> CsDocCommentRawComment.class.getName().equals(d.getValueType()));
        assertThat(addedDoc).as("Structured DocComment should be decomposed over the wire").isTrue();
        assertThat(addedRaw).as("DocComment should not be flattened to a raw comment").isFalse();
    }

    @Test
    void docCommentRoundTripsThroughSenderAndReceiver() {
        roundTripPreservesStructure("/ <summary>doc</summary>");
    }

    @Test
    void multilineDocCommentWithNestedElementsRoundTrips() {
        roundTripPreservesStructure(
                "/ <summary>\n" +
                "/// Adds <paramref name=\"a\"/> to <c>b</c>.\n" +
                "/// </summary>\n" +
                "/// <param name=\"a\">first</param>");
    }

    private void roundTripPreservesStructure(String rawText) {
        Space space = spaceWithDocComment(rawText);
        CsDocComment.DocComment original = (CsDocComment.DocComment) space.getComments().get(0);

        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, SOURCE_FILE_TYPE, null);

        new CSharpSender().visitSpace(space, sq);
        sq.flush();
        Space received = new CSharpReceiver().visitSpace(Space.EMPTY, rq);

        assertThat(received.getComments()).hasSize(1);
        Comment receivedComment = received.getComments().get(0);
        assertThat(receivedComment).isInstanceOf(CsDocComment.DocComment.class);

        CsDocComment.DocComment roundTripped = (CsDocComment.DocComment) receivedComment;
        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getSuffix()).isEqualTo(original.getSuffix());
        // Printing exercises the entire reconstructed subtree, so identical output proves
        // the structure (and ids) survived the round trip.
        assertThat(print(roundTripped)).isEqualTo(print(original));
    }

    private static Space spaceWithDocComment(String rawText) {
        CsDocCommentRawComment raw = new CsDocCommentRawComment(rawText, "\n", Markers.EMPTY);
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(raw);
        return Space.EMPTY.withComments(List.<Comment>of(parsed));
    }

    private static String print(CsDocComment.DocComment d) {
        PrintOutputCapture<Integer> out = new PrintOutputCapture<>(0);
        new CsDocCommentPrinter<Integer>().visit(d, out, new Cursor(null, Cursor.ROOT_VALUE));
        return out.getOut();
    }

    private static List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
