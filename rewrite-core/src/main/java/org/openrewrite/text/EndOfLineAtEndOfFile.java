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
package org.openrewrite.text;

import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import static java.util.Objects.requireNonNull;

public class EndOfLineAtEndOfFile extends Recipe {

    @Override
    public String getDisplayName() {
        return "End of Line @ End of File (EOL @ EOF)";
    }

    @Override
    public String getDescription() {
        return "Ensure that the file ends with the newline character.\n\n" +
                "*Note*: If this recipe modifies a file, it converts the file into plain text. " +
                "As such, this recipe should be run after any recipe that modifies the language-specific LST.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                boolean whitespaceContainsCRLF = plainText.getText().contains("\r\n");
                if (!plainText.getText().endsWith("\n")) {
                    if (whitespaceContainsCRLF) {
                        return plainText.withText(plainText.getText() + "\r\n");
                    } else {
                        return plainText.withText(plainText.getText() + '\n');
                    }
                }
                return sourceFile;
            }
        };
    }
}
