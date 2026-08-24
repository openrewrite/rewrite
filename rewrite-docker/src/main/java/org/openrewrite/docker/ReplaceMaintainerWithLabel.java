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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.ParserDirectives;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMaintainerWithLabel extends Recipe {

    private static final String AUTHORS = "org.opencontainers.image.authors";

    String displayName = "Replace `MAINTAINER` with an `org.opencontainers.image.authors` label";

    String description = "BuildKit's `MaintainerDeprecated` check reports the `MAINTAINER` instruction, whose place has been " +
            "taken by the `org.opencontainers.image.authors` label. A stage that already carries that label " +
            "has its `MAINTAINER` dropped rather than gaining a second one. A file whose `# escape=` directive " +
            "names the backtick is left alone, because a backslash escapes nothing there.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                return ParserDirectives.escapeChar(file) == '\\' ? super.visitFile(file, ctx) : file;
            }

            @Override
            public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
                Docker.Stage s = super.visitStage(stage, ctx);
                int last = lastMaintainer(s);
                if (last < 0) {
                    return s;
                }
                boolean authored = hasAuthorsLabel(s);
                Docker.Label label = authored ? null :
                        asLabel((Docker.Maintainer) s.getInstructions().get(last));
                if (!authored && label == null) {
                    return s;
                }
                return s.withInstructions(ListUtils.map(s.getInstructions(), (i, instruction) ->
                        instruction instanceof Docker.Maintainer ? (i == last ? label : null) : instruction));
            }
        };
    }

    /// The last `MAINTAINER` of the stage is the one whose author the image ends up with.
    private static int lastMaintainer(Docker.Stage stage) {
        List<Docker.Instruction> instructions = stage.getInstructions();
        for (int i = instructions.size() - 1; i >= 0; i--) {
            if (instructions.get(i) instanceof Docker.Maintainer) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasAuthorsLabel(Docker.Stage stage) {
        for (Docker.Instruction instruction : stage.getInstructions()) {
            if (instruction instanceof Docker.Label) {
                for (Docker.Label.LabelPair pair : ((Docker.Label) instruction).getPairs()) {
                    if (AUTHORS.equals(ArgumentContents.textWithVariables(pair.getKey()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Docker.@Nullable Label asLabel(Docker.Maintainer maintainer) {
        String text = ArgumentContents.textWithVariables(maintainer.getText());
        if (text.isEmpty() || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return null;
        }
        Docker.Label.LabelPair pair = new Docker.Label.LabelPair(
                randomId(),
                Space.SINGLE_SPACE,
                Markers.EMPTY,
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, ArgumentContents.of(AUTHORS, null)),
                true,
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, value(text))
        );
        return new Docker.Label(randomId(), maintainer.getPrefix(), maintainer.getMarkers(),
                labelKeyword(maintainer.getKeyword()), singletonList(pair));
    }

    /// A `MAINTAINER` is taken as it stands while a label value is read by the shell, so one the shell would
    /// not give back unchanged is quoted and escaped.
    private static List<Docker.ArgumentContent> value(String text) {
        StringBuilder escaped = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '"' || c == '$') {
                escaped.append('\\');
                quote = true;
            } else if (c == '\'' || c == '=' || Character.isWhitespace(c)) {
                quote = true;
            }
            escaped.append(c);
        }
        return quote ?
                singletonList(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, escaped.toString(),
                        Docker.Literal.QuoteStyle.DOUBLE)) :
                ArgumentContents.of(text, null);
    }

    /// `LABEL` in the casing the `MAINTAINER` it replaces was written in.
    private static String labelKeyword(String maintainer) {
        return maintainer.equals(maintainer.toLowerCase(Locale.ROOT)) ? "label" : "LABEL";
    }
}
