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
package org.openrewrite.csharp.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Marker attached to C# CompilationUnits carrying detected or configured formatting style.
 * Populated from .editorconfig files when available, otherwise from Roslyn defaults.
 */
@Value
@With
public class CSharpFormatStyle implements Marker, RpcCodec<CSharpFormatStyle> {
    UUID id;
    boolean useTabs;
    int indentSize;
    int tabSize;
    String newLine;

    // Brace placement
    boolean newLinesForBracesInTypes;
    boolean newLinesForBracesInMethods;
    boolean newLinesForBracesInProperties;
    boolean newLinesForBracesInAccessors;
    boolean newLinesForBracesInAnonymousMethods;
    boolean newLinesForBracesInAnonymousTypes;
    boolean newLinesForBracesInControlBlocks;
    boolean newLinesForBracesInLambdaExpressionBody;
    boolean newLinesForBracesInObjectCollectionArrayInitializers;
    boolean newLinesForBracesInLocalFunctions;

    // New line before keywords
    boolean newLineBeforeElse;
    boolean newLineBeforeCatch;
    boolean newLineBeforeFinally;

    // Wrapping
    boolean wrappingPreserveSingleLine;
    boolean wrappingKeepStatementsOnSingleLine;

    @Override
    public void rpcSend(CSharpFormatStyle after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, m -> m.useTabs);
        q.getAndSend(after, m -> m.indentSize);
        q.getAndSend(after, m -> m.tabSize);
        q.getAndSend(after, m -> m.newLine);
        q.getAndSend(after, m -> m.newLinesForBracesInTypes);
        q.getAndSend(after, m -> m.newLinesForBracesInMethods);
        q.getAndSend(after, m -> m.newLinesForBracesInProperties);
        q.getAndSend(after, m -> m.newLinesForBracesInAccessors);
        q.getAndSend(after, m -> m.newLinesForBracesInAnonymousMethods);
        q.getAndSend(after, m -> m.newLinesForBracesInAnonymousTypes);
        q.getAndSend(after, m -> m.newLinesForBracesInControlBlocks);
        q.getAndSend(after, m -> m.newLinesForBracesInLambdaExpressionBody);
        q.getAndSend(after, m -> m.newLinesForBracesInObjectCollectionArrayInitializers);
        q.getAndSend(after, m -> m.newLinesForBracesInLocalFunctions);
        q.getAndSend(after, m -> m.newLineBeforeElse);
        q.getAndSend(after, m -> m.newLineBeforeCatch);
        q.getAndSend(after, m -> m.newLineBeforeFinally);
        q.getAndSend(after, m -> m.wrappingPreserveSingleLine);
        q.getAndSend(after, m -> m.wrappingKeepStatementsOnSingleLine);
    }

    @Override
    public CSharpFormatStyle rpcReceive(CSharpFormatStyle before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withUseTabs(q.receive(before.useTabs))
                .withIndentSize(q.receive(before.indentSize))
                .withTabSize(q.receive(before.tabSize))
                .withNewLine(q.<String, String>receiveAndGet(before.newLine, x -> x))
                .withNewLinesForBracesInTypes(q.receive(before.newLinesForBracesInTypes))
                .withNewLinesForBracesInMethods(q.receive(before.newLinesForBracesInMethods))
                .withNewLinesForBracesInProperties(q.receive(before.newLinesForBracesInProperties))
                .withNewLinesForBracesInAccessors(q.receive(before.newLinesForBracesInAccessors))
                .withNewLinesForBracesInAnonymousMethods(q.receive(before.newLinesForBracesInAnonymousMethods))
                .withNewLinesForBracesInAnonymousTypes(q.receive(before.newLinesForBracesInAnonymousTypes))
                .withNewLinesForBracesInControlBlocks(q.receive(before.newLinesForBracesInControlBlocks))
                .withNewLinesForBracesInLambdaExpressionBody(q.receive(before.newLinesForBracesInLambdaExpressionBody))
                .withNewLinesForBracesInObjectCollectionArrayInitializers(q.receive(before.newLinesForBracesInObjectCollectionArrayInitializers))
                .withNewLinesForBracesInLocalFunctions(q.receive(before.newLinesForBracesInLocalFunctions))
                .withNewLineBeforeElse(q.receive(before.newLineBeforeElse))
                .withNewLineBeforeCatch(q.receive(before.newLineBeforeCatch))
                .withNewLineBeforeFinally(q.receive(before.newLineBeforeFinally))
                .withWrappingPreserveSingleLine(q.receive(before.wrappingPreserveSingleLine))
                .withWrappingKeepStatementsOnSingleLine(q.receive(before.wrappingKeepStatementsOnSingleLine));
    }
}
