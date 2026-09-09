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
import org.openrewrite.Tree;
import org.openrewrite.csharp.CsDocCommentPrinter;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.csharp.tree.CsDocComment;
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
 * A structured {@link CsDocComment.DocComment} is decomposed over RPC by {@link CSharpSender}
 * (mirroring {@code Javadoc.DocComment} on the Java side) and reconstructed by
 * {@link CSharpReceiver}, so the whole tree survives a round trip with full fidelity — the raw
 * flattened form no longer exists on either side.
 */
class CSharpSenderDocCommentTest {

    private static final String SOURCE_FILE_TYPE = Cs.CompilationUnit.class.getName();

    @Test
    void visitSpaceWithDocCommentDoesNotThrow() {
        Space space = spaceWithDocComment();

        CSharpSender sender = new CSharpSender();
        RpcSendQueue queue = new RpcSendQueue(64, batch -> {}, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);

        assertThatNoException().isThrownBy(() -> sender.visitSpace(space, queue));
    }

    @Test
    void docCommentIsDecomposedAsStructuredTree() {
        Space space = spaceWithDocComment();

        List<RpcObjectData> emitted = new ArrayList<>();
        CSharpSender sender = new CSharpSender();
        RpcSendQueue queue = new RpcSendQueue(1024, emitted::addAll, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);

        sender.visitSpace(space, queue);
        queue.flush();

        // The structured DocComment tree must travel over the wire as decomposed nodes.
        boolean addedDoc = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> d.getValueType() != null && d.getValueType().endsWith("$DocComment"));
        boolean addedElement = emitted.stream()
                .filter(d -> d.getState() == RpcObjectData.State.ADD)
                .anyMatch(d -> d.getValueType() != null && d.getValueType().endsWith("$XmlElement"));
        assertThat(addedDoc).as("Structured DocComment should be decomposed over the wire").isTrue();
        assertThat(addedElement).as("Nested XmlElement should be decomposed over the wire").isTrue();
    }

    @Test
    void docCommentRoundTripsThroughSenderAndReceiver() {
        Space space = spaceWithDocComment();
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

    /**
     * Builds a structured doc comment covering every node type, equivalent to:
     * <pre>
     * /// &lt;summary&gt;
     * /// Adds &lt;paramref name="a"/&gt;.
     * /// &lt;/summary&gt;
     * </pre>
     */
    private static Space spaceWithDocComment() {
        CsDocComment paramrefName = new CsDocComment.XmlNameAttribute(Tree.randomId(), Markers.EMPTY,
                null, List.of(text("\"a\"")), null);
        CsDocComment paramref = new CsDocComment.XmlEmptyElement(Tree.randomId(), Markers.EMPTY, "paramref",
                List.of(text(" "), paramrefName), List.of());

        List<CsDocComment> summaryContent = List.of(
                lineBreak("\n///"), text(" Adds "), paramref, text("."),
                lineBreak("\n///"), text(" "));
        CsDocComment summary = new CsDocComment.XmlElement(Tree.randomId(), Markers.EMPTY, "summary",
                List.of(), List.of(), summaryContent, List.of());

        CsDocComment.DocComment doc = new CsDocComment.DocComment(Tree.randomId(), Markers.EMPTY,
                List.of(text(" "), lineBreak("\n///"), text(" "), summary), "\n");
        return Space.EMPTY.withComments(List.<Comment>of(doc));
    }

    private static CsDocComment.XmlText text(String value) {
        return new CsDocComment.XmlText(Tree.randomId(), Markers.EMPTY, value);
    }

    private static CsDocComment.LineBreak lineBreak(String margin) {
        return new CsDocComment.LineBreak(Tree.randomId(), margin, Markers.EMPTY);
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
