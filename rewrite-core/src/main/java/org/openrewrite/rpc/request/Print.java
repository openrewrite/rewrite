/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.request;

import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.objenesis.ObjenesisStd;
import org.openrewrite.*;

import java.nio.file.Path;
import java.util.function.BiFunction;

@Value
public class Print implements RpcRequest {
    String treeId;
    Path sourcePath;
    String sourceFileType;

    @Nullable
    MarkerPrinter markerPrinter;

    @RequiredArgsConstructor
    public static class Handler extends JsonRpcMethod<Print> {
        private final BiFunction<String, @Nullable String, ?> getObject;

        @Override
        protected Object handle(Print request) throws Exception {
            Tree tree = (Tree) getObject.apply(request.getTreeId(), request.sourceFileType);

            try {
                // Create an instance of the SourceFile type using Objenesis
                Class<?> sourceFileClass = Class.forName(request.getSourceFileType());
                SourceFile dummySourceFile = (SourceFile) new ObjenesisStd().newInstance(sourceFileClass);

                // Create the appropriate PrintOutputCapture
                PrintOutputCapture<Integer> outputCapture = request.getMarkerPrinter() != null ?
                        new PrintOutputCapture<>(0, request.getMarkerPrinter().toPrintOutputCapture()) :
                        new PrintOutputCapture<>(0);

                // Get the printer from the dummy SourceFile and use it to print the tree
                Cursor dummyCursor = new Cursor(null, dummySourceFile);
                TreeVisitor<?, PrintOutputCapture<Integer>> printer = dummySourceFile.printer(dummyCursor);
                printer.visit(tree, outputCapture);

                return outputCapture.getOut();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unknown source file type: " + request.getSourceFileType(), e);
            }
        }
    }

    public enum MarkerPrinter {
        DEFAULT,
        SEARCH_MARKERS_ONLY,
        FENCED,
        SANITIZED,
        ;

        public static MarkerPrinter from(PrintOutputCapture.MarkerPrinter markerPrinter) {
            if (markerPrinter == PrintOutputCapture.MarkerPrinter.DEFAULT) {
                return DEFAULT;
            } else if (markerPrinter == PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY) {
                return SEARCH_MARKERS_ONLY;
            } else if (markerPrinter == PrintOutputCapture.MarkerPrinter.FENCED) {
                return FENCED;
            } else if (markerPrinter == PrintOutputCapture.MarkerPrinter.SANITIZED) {
                return SANITIZED;
            }
            throw new IllegalArgumentException("Unknown marker printer " + markerPrinter);
        }

        private PrintOutputCapture.MarkerPrinter toPrintOutputCapture() {
            switch (this) {
                case DEFAULT:
                    return PrintOutputCapture.MarkerPrinter.DEFAULT;
                case SEARCH_MARKERS_ONLY:
                    return PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY;
                case FENCED:
                    return PrintOutputCapture.MarkerPrinter.FENCED;
                case SANITIZED:
                    return PrintOutputCapture.MarkerPrinter.SANITIZED;
                default:
                    throw new IllegalArgumentException("Unknown marker printer: " + this);
            }
        }
    }
}
