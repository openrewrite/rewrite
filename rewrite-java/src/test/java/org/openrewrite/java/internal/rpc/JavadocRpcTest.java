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
package org.openrewrite.java.internal.rpc;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@link Javadoc} node is {@code @EqualsAndHashCode(onlyExplicitlyIncluded = true)} on
 * {@code id} alone, so two doc comments compare equal no matter how badly their contents were
 * mangled in between. Every assertion here compares printed output instead.
 * <p>
 * The doc comment is round-tripped on its own rather than as part of its compilation unit: a whole
 * {@code J.CompilationUnit} does not survive this harness yet for an unrelated reason (enums are
 * still sent as objects, which is why {@code JavaSendReceiveTest.sendReceiveIdempotence} is
 * disabled), and that would mask what is under test here.
 */
class JavadocRpcTest {

    private Deque<List<RpcObjectData>> batches;
    private RpcSendQueue sq;
    private RpcReceiveQueue rq;

    @BeforeEach
    void setUp() {
        batches = new ArrayDeque<>();
        String sourceFileType = J.CompilationUnit.class.getName();
        sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), new IdentityHashMap<>(), sourceFileType, false);
        rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, sourceFileType, null);
    }

    @Test
    void blockTagsRoundTrip() {
        assertRoundTrip(
          """
            /**
             * A summary sentence.
             *
             * @param name the name
             * @return nothing
             */
            class Foo {
                void bar(String name) {
                }
            }
            """
        );
    }

    @Test
    void inlineTagsAndReferencesRoundTrip() {
        assertRoundTrip(
          """
            /**
             * See {@link Foo#bar(String)} and {@linkplain Foo plain}.
             * A {@code literal} and a {@literal <b>raw</b>} value.
             * {@inheritDoc}
             *
             * @see Foo#bar(String)
             * @since 1.0
             * @deprecated use something else
             */
            class Foo {
                void bar(String name) {
                }
            }
            """
        );
    }

    @Test
    void htmlElementsAndUnknownTagsRoundTrip() {
        assertRoundTrip(
          """
            /**
             * <p>A paragraph with <b>bold</b> and an <img src="x.png"/> element.</p>
             *
             * @author Jon
             * @version 2
             * @customTag whatever this is
             */
            class Foo {
            }
            """
        );
    }

    @Test
    void throwsTagsRoundTrip() {
        assertRoundTrip(
          """
            class Foo {
                /**
                 * @throws IllegalStateException when broken
                 * @exception RuntimeException also when broken
                 */
                void bar() {
                }
            }
            """
        );
    }

    private void assertRoundTrip(@Language("java") String source) {
        SourceFile cu = JavaParser.fromJavaVersion().build()
          .parse(new InMemoryExecutionContext(), source)
          .findFirst()
          .orElseThrow();

        Javadoc.DocComment before = findDocComment(cu);
        Cursor cursor = new Cursor(null, cu);
        String expected = before.printComment(cursor);
        assertThat(expected).as("the fixture must actually contain a doc comment").contains("@");

        JavaVisitor<RpcSendQueue> sender = new JavaSender();
        sq.send(before, null, () -> new JavadocSender(sender).visit(before, sq));
        sq.flush();

        JavaVisitor<RpcReceiveQueue> receiver = new JavaReceiver();
        Javadoc.DocComment after = rq.receive(null,
          d -> (Javadoc.DocComment) new JavadocReceiver(receiver).visit(d, rq));

        assertThat(after.printComment(cursor)).isEqualTo(expected);
    }

    private Javadoc.DocComment findDocComment(SourceFile cu) {
        List<Javadoc.DocComment> found = new ArrayList<>();
        new JavaVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                for (Comment comment : space.getComments()) {
                    if (comment instanceof Javadoc.DocComment) {
                        found.add((Javadoc.DocComment) comment);
                    }
                }
                return space;
            }
        }.visit(cu, 0);
        assertThat(found).as("fixture should parse to exactly one doc comment").hasSize(1);
        return found.get(0);
    }

    private List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            // The wire carries a UUID or Path as a string, as the transport's JSON encoding would.
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(),
                  data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
