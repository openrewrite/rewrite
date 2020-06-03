/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.AutoConfigure;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.config.MapConfigSource.mapConfig;

public class RefactorTest {
    @Test
    void scanAutoConfigurableRules() {
        PlainText fixed = new PlainText(randomId(), "Hello World!", EMPTY)
                .refactor()
                .scan(mapConfig("test.ChangeText.toText", "Hello Jon!"), "org.openrewrite")
                .fix().getFixed();

        assertThat(fixed.print()).isEqualTo("Hello Jon!");
    }
}

class ChangeText extends SourceVisitor<PlainText> implements RefactorVisitorSupport {
    private final String toText;

    public ChangeText(String toText) {
        super("test.ChangeText");
        this.toText = toText;
    }

    @AutoConfigure
    private static ChangeText configure(Config config) {
        return new ChangeText(config.getValue("test.ChangeText.toText", String.class));
    }

    @Override
    public PlainText visitTree(Tree tree) {
        PlainText text = (PlainText) tree;
        return text.withText(toText);
    }

    @Override
    public PlainText defaultTo(Tree t) {
        return (PlainText) t;
    }
}

class PlainText implements SourceFile, Tree {
    private final UUID id;
    private final String text;
    private final Formatting formatting;

    public PlainText(UUID id, String text, Formatting formatting) {
        this.id = id;
        this.text = text;
        this.formatting = formatting;
    }

    public Refactor<PlainText, PlainText> refactor() {
        return new Refactor<>(this);
    }

    @Override
    public String getSourcePath() {
        return null;
    }

    @Override
    public Map<Metadata, String> getMetadata() {
        return null;
    }

    @Override
    public String getFileType() {
        return "txt";
    }

    @Override
    public Formatting getFormatting() {
        return formatting;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public PlainText withText(String toText) {
        return new PlainText(id, toText, formatting);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Tree> T withFormatting(Formatting fmt) {
        return (T) new PlainText(id, text, fmt);
    }

    @Override
    public String print() {
        return formatting.getPrefix() + text + formatting.getSuffix();
    }
}


