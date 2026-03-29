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
package org.openrewrite.csharp.style;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Marker attached to C# CompilationUnits carrying detected or configured formatting style.
 * Populated from .editorconfig files when available, otherwise from Roslyn defaults.
 * <p>
 * Like {@link org.openrewrite.javascript.style.PrettierStyle}, this extends {@link NamedStyles}
 * but stores its configuration directly rather than in the {@code styles} collection, because
 * all formatting is delegated to a single external formatter (Roslyn).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CSharpFormatStyle extends NamedStyles implements RpcCodec<CSharpFormatStyle> {
    private static final String NAME = "org.openrewrite.csharp.CSharpFormatStyle";
    private static final String DISPLAY_NAME = "C# Format Style";
    private static final String DESCRIPTION = "C# formatting style from .editorconfig or Roslyn defaults.";

    private final boolean useTabs;
    private final int indentSize;
    private final int tabSize;
    private final String newLine;

    // Brace placement
    private final boolean newLinesForBracesInTypes;
    private final boolean newLinesForBracesInMethods;
    private final boolean newLinesForBracesInProperties;
    private final boolean newLinesForBracesInAccessors;
    private final boolean newLinesForBracesInAnonymousMethods;
    private final boolean newLinesForBracesInAnonymousTypes;
    private final boolean newLinesForBracesInControlBlocks;
    private final boolean newLinesForBracesInLambdaExpressionBody;
    private final boolean newLinesForBracesInObjectCollectionArrayInitializers;
    private final boolean newLinesForBracesInLocalFunctions;

    // New line before keywords
    private final boolean newLineBeforeElse;
    private final boolean newLineBeforeCatch;
    private final boolean newLineBeforeFinally;

    // Wrapping
    private final boolean wrappingPreserveSingleLine;
    private final boolean wrappingKeepStatementsOnSingleLine;

    public CSharpFormatStyle(UUID id, boolean useTabs, int indentSize, int tabSize, String newLine,
                             boolean newLinesForBracesInTypes, boolean newLinesForBracesInMethods,
                             boolean newLinesForBracesInProperties, boolean newLinesForBracesInAccessors,
                             boolean newLinesForBracesInAnonymousMethods, boolean newLinesForBracesInAnonymousTypes,
                             boolean newLinesForBracesInControlBlocks, boolean newLinesForBracesInLambdaExpressionBody,
                             boolean newLinesForBracesInObjectCollectionArrayInitializers, boolean newLinesForBracesInLocalFunctions,
                             boolean newLineBeforeElse, boolean newLineBeforeCatch, boolean newLineBeforeFinally,
                             boolean wrappingPreserveSingleLine, boolean wrappingKeepStatementsOnSingleLine) {
        super(id, NAME, DISPLAY_NAME, DESCRIPTION, emptySet(), emptyList());
        this.useTabs = useTabs;
        this.indentSize = indentSize;
        this.tabSize = tabSize;
        this.newLine = newLine;
        this.newLinesForBracesInTypes = newLinesForBracesInTypes;
        this.newLinesForBracesInMethods = newLinesForBracesInMethods;
        this.newLinesForBracesInProperties = newLinesForBracesInProperties;
        this.newLinesForBracesInAccessors = newLinesForBracesInAccessors;
        this.newLinesForBracesInAnonymousMethods = newLinesForBracesInAnonymousMethods;
        this.newLinesForBracesInAnonymousTypes = newLinesForBracesInAnonymousTypes;
        this.newLinesForBracesInControlBlocks = newLinesForBracesInControlBlocks;
        this.newLinesForBracesInLambdaExpressionBody = newLinesForBracesInLambdaExpressionBody;
        this.newLinesForBracesInObjectCollectionArrayInitializers = newLinesForBracesInObjectCollectionArrayInitializers;
        this.newLinesForBracesInLocalFunctions = newLinesForBracesInLocalFunctions;
        this.newLineBeforeElse = newLineBeforeElse;
        this.newLineBeforeCatch = newLineBeforeCatch;
        this.newLineBeforeFinally = newLineBeforeFinally;
        this.wrappingPreserveSingleLine = wrappingPreserveSingleLine;
        this.wrappingKeepStatementsOnSingleLine = wrappingKeepStatementsOnSingleLine;
    }

    @Override
    public CSharpFormatStyle withId(UUID id) {
        return id == getId() ? this : new CSharpFormatStyle(id, useTabs, indentSize, tabSize, newLine,
                newLinesForBracesInTypes, newLinesForBracesInMethods, newLinesForBracesInProperties,
                newLinesForBracesInAccessors, newLinesForBracesInAnonymousMethods, newLinesForBracesInAnonymousTypes,
                newLinesForBracesInControlBlocks, newLinesForBracesInLambdaExpressionBody,
                newLinesForBracesInObjectCollectionArrayInitializers, newLinesForBracesInLocalFunctions,
                newLineBeforeElse, newLineBeforeCatch, newLineBeforeFinally,
                wrappingPreserveSingleLine, wrappingKeepStatementsOnSingleLine);
    }

    @Override
    public CSharpFormatStyle withStyles(Collection<Style> styles) {
        // CSharpFormatStyle doesn't use the styles collection
        return this;
    }

    @Override
    public void rpcSend(CSharpFormatStyle after, RpcSendQueue q) {
        q.getAndSend(after, NamedStyles::getId);
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
        return new CSharpFormatStyle(
                q.receiveAndGet(before.getId(), UUID::fromString),
                q.receive(before.useTabs),
                q.receive(before.indentSize),
                q.receive(before.tabSize),
                q.<String, String>receiveAndGet(before.newLine, x -> x),
                q.receive(before.newLinesForBracesInTypes),
                q.receive(before.newLinesForBracesInMethods),
                q.receive(before.newLinesForBracesInProperties),
                q.receive(before.newLinesForBracesInAccessors),
                q.receive(before.newLinesForBracesInAnonymousMethods),
                q.receive(before.newLinesForBracesInAnonymousTypes),
                q.receive(before.newLinesForBracesInControlBlocks),
                q.receive(before.newLinesForBracesInLambdaExpressionBody),
                q.receive(before.newLinesForBracesInObjectCollectionArrayInitializers),
                q.receive(before.newLinesForBracesInLocalFunctions),
                q.receive(before.newLineBeforeElse),
                q.receive(before.newLineBeforeCatch),
                q.receive(before.newLineBeforeFinally),
                q.receive(before.wrappingPreserveSingleLine),
                q.receive(before.wrappingKeepStatementsOnSingleLine)
        );
    }
}
