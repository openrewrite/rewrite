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
package org.openrewrite.text;

import org.openrewrite.*;

import java.util.Map;
import java.util.UUID;

public class PlainText implements SourceFile, Tree {
    private final UUID id;
    private final String text;
    private final Formatting formatting;

    public PlainText(UUID id, String text, Formatting formatting) {
        this.id = id;
        this.text = text;
        this.formatting = formatting;
    }

    public Refactor<PlainText> refactor() {
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
    public String getTreeType() {
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
