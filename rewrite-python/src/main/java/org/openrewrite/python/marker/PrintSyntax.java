/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.python.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker indicating that a {@code J.MethodInvocation} represents a Python 2 print statement
 * rather than a Python 3 print function call.
 * <p>
 * In Python 2, print is a statement:
 * <pre>
 * print "hello"           # simple print
 * print >> stderr, "err"  # print to file (hasDestination=true, first arg is destination)
 * print x,                # trailing comma suppresses newline
 * </pre>
 * <p>
 * The marker allows the printer to output the correct syntax and allows recipes
 * to distinguish between print statements and print function calls.
 */
@Value
@With
public class PrintSyntax implements Marker, RpcCodec<PrintSyntax> {
    UUID id;

    /**
     * True if the print statement uses the {@code >>} syntax to redirect output
     * to a file-like object. When true, the first argument of the MethodInvocation
     * is the destination, followed by the values to print.
     */
    boolean hasDestination;

    /**
     * True if the print statement has a trailing comma, which suppresses the
     * automatic newline at the end of the output in Python 2.
     */
    boolean trailingComma;

    @Override
    public void rpcSend(PrintSyntax after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, PrintSyntax::isHasDestination);
        q.getAndSend(after, PrintSyntax::isTrailingComma);
    }

    @Override
    public PrintSyntax rpcReceive(PrintSyntax before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withHasDestination(q.receive(before.isHasDestination()))
                .withTrailingComma(q.receive(before.isTrailingComma()));
    }
}
