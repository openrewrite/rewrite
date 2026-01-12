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
package org.openrewrite.docker.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.docker.internal.grammar.DockerLexer;
import org.openrewrite.docker.internal.grammar.DockerParser;
import org.openrewrite.docker.internal.grammar.DockerParserBaseVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public class DockerParserVisitor extends DockerParserBaseVisitor<Docker> {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    private int cursor = 0;
    private int codePointCursor = 0;

    public DockerParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    public Docker.File visitDockerfile(DockerParser.DockerfileContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Parse global ARG instructions (before first FROM)
        List<Docker.Arg> globalArgs = new ArrayList<>();
        if (ctx.globalArgs() != null) {
            for (DockerParser.ArgInstructionContext argCtx : ctx.globalArgs().argInstruction()) {
                Docker.Arg arg = (Docker.Arg) visit(argCtx);
                if (arg != null) {
                    globalArgs.add(arg);
                }
            }
        }

        // Parse build stages
        List<Docker.Stage> stages = new ArrayList<>();
        for (DockerParser.StageContext stageCtx : ctx.stage()) {
            Docker.Stage stage = visitStage(stageCtx);
            if (stage != null) {
                stages.add(stage);
            }
        }

        return new Docker.File(
                randomId(),
                path,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                globalArgs,
                stages,
                Space.format(source, cursor, source.length())
        );
    }

    @Override
    public Docker.Stage visitStage(DockerParser.StageContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Parse the FROM instruction that starts this stage
        Docker.From from = (Docker.From) visit(ctx.fromInstruction());

        // Parse stage instructions
        List<Docker.Instruction> instructions = new ArrayList<>();
        for (DockerParser.StageInstructionContext instructionCtx : ctx.stageInstruction()) {
            Docker.Instruction instruction = (Docker.Instruction) visit(instructionCtx);
            if (instruction != null) {
                instructions.add(instruction);
            }
        }

        return new Docker.Stage(
                randomId(),
                prefix,
                Markers.EMPTY,
                from,
                instructions
        );
    }

    @Override
    public Docker visitFromInstruction(DockerParser.FromInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the FROM keyword
        String fromKeyword = ctx.FROM().getText();
        skip(ctx.FROM().getSymbol());

        List<Docker.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;
        Docker.@Nullable Argument[] imageComponents = parseImageName(ctx.imageName());
        Docker.Argument imageName = requireNonNull(imageComponents[0]);
        Docker.Argument tag = imageComponents[1];
        Docker.Argument digest = imageComponents[2];
        Docker.From.As as = ctx.AS() != null ? visitFromAs(ctx) : null;

        // Cursor has already been advanced by parseImageName and other parsing methods
        // No additional advancement needed here

        return new Docker.From(randomId(), prefix, Markers.EMPTY, fromKeyword, flags, imageName, tag, digest, as);
    }

    private Docker.From.As visitFromAs(DockerParser.FromInstructionContext ctx) {
        Space asPrefix = prefix(ctx.AS().getSymbol());
        String asKeyword = ctx.AS().getText();
        skip(ctx.AS().getSymbol());

        // Stage name is always a simple identifier
        Space namePrefix = prefix(ctx.stageName().getStart());
        skip(ctx.stageName().getStop());
        Docker.Literal name = new Docker.Literal(
                randomId(),
                namePrefix,
                Markers.EMPTY,
                ctx.stageName().getText(),
                null
        );

        return new Docker.From.As(
                randomId(),
                asPrefix,
                Markers.EMPTY,
                asKeyword,
                name
        );
    }

    private Docker.@Nullable Argument[] parseImageName(DockerParser.ImageNameContext ctx) {
        Space prefix = prefix(ctx);

        // Parse the text and split out environment variables
        List<Docker.ArgumentContent> contents = parseText(ctx.text());

        // Advance cursor to end of text
        advanceCursor(ctx.text().getStop().getStopIndex() + 1);

        // If the entire image is a single quoted string, don't split it
        if (contents.size() == 1 && contents.get(0) instanceof Docker.Literal && ((Docker.Literal) contents.get(0)).isQuoted()) {
            // Single quoted string - keep it as-is
            Docker.Argument imageName = new Docker.Argument(randomId(), prefix, Markers.EMPTY, contents);
            return new Docker.@Nullable Argument[]{imageName, null, null};
        }

        // Split contents into imageName, tag, and digest components
        List<Docker.ArgumentContent> imageNameContents = new ArrayList<>();
        List<Docker.ArgumentContent> tagContents = new ArrayList<>();
        List<Docker.ArgumentContent> digestContents = new ArrayList<>();

        boolean foundColon = false;
        boolean foundAt = false;

        for (Docker.ArgumentContent content : contents) {
            if (content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted()) {
                String text = ((Docker.Literal) content).getText();

                // Look for @ first (digest takes precedence over tag)
                int atIndex = text.indexOf('@');
                int colonIndex = text.indexOf(':');

                if (atIndex >= 0 && !foundAt) {
                    // Split at @
                    foundAt = true;
                    String imagePart = text.substring(0, atIndex);
                    String digestPart = text.substring(atIndex + 1);

                    if (!imagePart.isEmpty()) {
                        imageNameContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, imagePart, null));
                    }
                    if (!digestPart.isEmpty()) {
                        digestContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, digestPart, null));
                    }
                } else if (colonIndex >= 0 && !foundColon && !foundAt) {
                    // Split at :
                    foundColon = true;
                    String imagePart = text.substring(0, colonIndex);
                    String tagPart = text.substring(colonIndex + 1);

                    if (!imagePart.isEmpty()) {
                        imageNameContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, imagePart, null));
                    }
                    if (!tagPart.isEmpty()) {
                        tagContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, tagPart, null));
                    }
                } else {
                    // Add to appropriate list
                    if (foundAt) {
                        digestContents.add(content);
                    } else if (foundColon) {
                        tagContents.add(content);
                    } else {
                        imageNameContents.add(content);
                    }
                }
            } else {
                // Environment variables or quoted strings
                if (foundAt) {
                    digestContents.add(content);
                } else if (foundColon) {
                    tagContents.add(content);
                } else {
                    imageNameContents.add(content);
                }
            }
        }

        Docker.Argument imageName = new Docker.Argument(randomId(), prefix, Markers.EMPTY, imageNameContents);
        Docker.Argument tag = tagContents.isEmpty() ? null :
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tagContents);
        Docker.Argument digest = digestContents.isEmpty() ? null :
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digestContents);

        return new Docker.@Nullable Argument[]{imageName, tag, digest};
    }

    private List<Docker.ArgumentContent> parseText(DockerParser.@Nullable TextContext textCtx) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();

        if (textCtx == null) {
            return contents;
        }

        // Get the actual text from source (including HIDDEN channel whitespace)
        int startIndex = textCtx.getStart().getStartIndex();
        int stopIndex = textCtx.getStop().getStopIndex();

        // Defensive check: ensure stopIndex >= startIndex
        if (stopIndex < startIndex) {
            // This can happen with certain edge cases; return empty contents
            return contents;
        }

        int startCharIndex = source.offsetByCodePoints(0, startIndex);
        int stopCharIndex = source.offsetByCodePoints(0, stopIndex + 1);

        // Another defensive check after offset calculation
        if (stopCharIndex < startCharIndex) {
            return contents;
        }

        String fullText = source.substring(startCharIndex, stopCharIndex);

        boolean hasQuotedString = fullText.contains("\"") || fullText.contains("'");
        boolean hasEnvironmentVariable = fullText.contains("$");
        boolean hasComment = fullText.contains("#");

        if (!hasQuotedString && !hasEnvironmentVariable && !hasComment) {
            // Simple case: just plain text
            contents.add(new Docker.Literal(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    fullText,
                    null
            ));
            return contents;
        }

        // Complex case: parse token by token
        boolean foundComment = false;
        for (int i = textCtx.getChildCount() - 1; i >= 0; i--) {
            ParseTree child = textCtx.getChild(i);
            if (child instanceof DockerParser.TextElementContext) {
                DockerParser.TextElementContext textElement = (DockerParser.TextElementContext) child;
                if (textElement.getChildCount() > 0 && textElement.getChild(0) instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) textElement.getChild(0);
                    if (terminal.getSymbol().getType() == DockerLexer.COMMENT) {
                        foundComment = true;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < textCtx.getChildCount(); i++) {
            ParseTree child = textCtx.getChild(i);

            if (child instanceof DockerParser.TextElementContext) {
                DockerParser.TextElementContext textElement = (DockerParser.TextElementContext) child;
                if (textElement.getChildCount() > 0 && textElement.getChild(0) instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) textElement.getChild(0);
                    Token token = terminal.getSymbol();
                    String tokenText = token.getText();

                    if (token.getType() == DockerLexer.COMMENT) {
                        // COMMENT tokens are ignored - they will be part of next element's prefix
                        break; // Stop processing tokens once we hit a comment
                    } else if (token.getType() == DockerLexer.DOUBLE_QUOTED_STRING) {
                        Space elementPrefix = prefix(token);
                        skip(token);
                        String value = tokenText.substring(1, tokenText.length() - 1);
                        contents.add(new Docker.Literal(
                                randomId(),
                                elementPrefix,
                                Markers.EMPTY,
                                value,
                                Docker.Literal.QuoteStyle.DOUBLE
                        ));
                    } else if (token.getType() == DockerLexer.SINGLE_QUOTED_STRING) {
                        Space elementPrefix = prefix(token);
                        skip(token);
                        String value = tokenText.substring(1, tokenText.length() - 1);
                        contents.add(new Docker.Literal(
                                randomId(),
                                elementPrefix,
                                Markers.EMPTY,
                                value,
                                Docker.Literal.QuoteStyle.SINGLE
                        ));
                    } else if (token.getType() == DockerLexer.ENV_VAR) {
                        Space elementPrefix = prefix(token);
                        skip(token);
                        boolean braced = tokenText.startsWith("${");
                        String varName = braced ? tokenText.substring(2, tokenText.length() - 1) : tokenText.substring(1);
                        contents.add(new Docker.EnvironmentVariable(
                                randomId(),
                                elementPrefix,
                                Markers.EMPTY,
                                varName,
                                braced
                        ));
                    } else {
                        // Plain text for other tokens
                        Space elementPrefix = prefix(token);
                        skip(token);
                        contents.add(new Docker.Literal(
                                randomId(),
                                elementPrefix,
                                Markers.EMPTY,
                                tokenText,
                                null
                        ));
                    }
                }
            }
        }

        return contents;
    }

    @Override
    public Docker visitRunInstruction(DockerParser.RunInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the RUN keyword
        String runKeyword = ctx.RUN().getText();
        skip(ctx.RUN().getSymbol());

        List<Docker.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;
        Docker.CommandForm command = visitCommandFormForRun(ctx);

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Run(randomId(), prefix, Markers.EMPTY, runKeyword, flags, command);
    }

    @Override
    public Docker visitAddInstruction(DockerParser.AddInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ADD keyword
        String addKeyword = ctx.ADD().getText();
        skip(ctx.ADD().getSymbol());

        List<Docker.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;

        Docker.HeredocForm heredoc = null;
        Docker.ExecForm execForm = null;
        List<Docker.Argument> sources = null;
        Docker.Argument destination = null;

        // Check if heredoc, jsonArray, or sourceList is present
        if (ctx.heredoc() != null) {
            heredoc = visitHeredocContext(ctx.heredoc());
            // For heredoc, destination is part of the heredoc (if present)
            // No separate destination to parse
        } else if (ctx.jsonArray() != null) {
            execForm = visitJsonArrayAsExecForm(ctx.jsonArray());
        } else if (ctx.sourceList() != null) {
            // Parse all paths (sources + destination) together
            // The grammar's flagValue may have greedily consumed some source tokens,
            // so we need to parse from the current cursor position
            List<Docker.Argument> allPaths = parseAllPaths(ctx.sourceList(), ctx.destination());
            if (allPaths.size() >= 2) {
                destination = allPaths.get(allPaths.size() - 1);
                sources = new ArrayList<>(allPaths.subList(0, allPaths.size() - 1));
            } else if (allPaths.size() == 1) {
                destination = allPaths.get(0);
            }
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Add(randomId(), prefix, Markers.EMPTY, addKeyword, flags, heredoc, execForm, sources, destination);
    }

    @Override
    public Docker visitCopyInstruction(DockerParser.CopyInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the COPY keyword
        String copyKeyword = ctx.COPY().getText();
        skip(ctx.COPY().getSymbol());

        List<Docker.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;

        Docker.HeredocForm heredoc = null;
        Docker.ExecForm execForm = null;
        List<Docker.Argument> sources = null;
        Docker.Argument destination = null;

        // Check if heredoc, jsonArray, or sourceList is present
        if (ctx.heredoc() != null) {
            heredoc = visitHeredocContext(ctx.heredoc());
            // For heredoc, destination is part of the heredoc (if present)
            // No separate destination to parse
        } else if (ctx.jsonArray() != null) {
            execForm = visitJsonArrayAsExecForm(ctx.jsonArray());
        } else if (ctx.sourceList() != null) {
            // Parse all paths (sources + destination) together
            // The grammar's flagValue may have greedily consumed some source tokens,
            // so we need to parse from the current cursor position
            List<Docker.Argument> allPaths = parseAllPaths(ctx.sourceList(), ctx.destination());
            if (allPaths.size() >= 2) {
                destination = allPaths.get(allPaths.size() - 1);
                sources = new ArrayList<>(allPaths.subList(0, allPaths.size() - 1));
            } else if (allPaths.size() == 1) {
                destination = allPaths.get(0);
            }
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Copy(randomId(), prefix, Markers.EMPTY, copyKeyword, flags, heredoc, execForm, sources, destination);
    }

    /**
     * Parse all paths (sources + destination) by scanning from the current cursor position.
     * This method ignores the grammar's token allocation because the greedy flagValue rule
     * may have consumed source tokens when flags are present.
     *
     * @param sourceListCtx the grammar's sourceList context (used only to find the end position)
     * @param destinationCtx the grammar's destination context (used only to find the end position)
     * @return a list of Arguments, where the last one is the destination
     */
    private List<Docker.Argument> parseAllPaths(DockerParser.SourceListContext sourceListCtx, DockerParser.DestinationContext destinationCtx) {
        List<Docker.Argument> allPaths = new ArrayList<>();

        // Find the end position of all paths (end of destination)
        int endCodePoint = destinationCtx.getStop().getStopIndex() + 1;
        int endCharIndex = source.offsetByCodePoints(0, endCodePoint);

        // Parse paths from current cursor position
        int currentPos = cursor;
        int currentCodePoint = codePointCursor;

        while (currentCodePoint < endCodePoint) {
            // Skip leading whitespace and capture it as prefix
            int prefixStart = currentPos;
            while (currentCodePoint < endCodePoint) {
                int charIndex = source.offsetByCodePoints(0, currentCodePoint);
                if (charIndex >= source.length()) break;
                char c = source.charAt(charIndex);
                if (!Character.isWhitespace(c)) break;
                currentCodePoint++;
                currentPos = source.offsetByCodePoints(0, currentCodePoint);
            }

            if (currentCodePoint >= endCodePoint) break;

            Space argPrefix = Space.format(source, prefixStart, currentPos);

            int tokenStart = currentPos;

            // Read until whitespace or end
            while (currentCodePoint < endCodePoint) {
                int charIndex = source.offsetByCodePoints(0, currentCodePoint);
                if (charIndex >= source.length()) break;
                char c = source.charAt(charIndex);

                // Handle quoted strings
                if (c == '"' || c == '\'') {
                    char quote = c;
                    currentCodePoint++;
                    currentPos = source.offsetByCodePoints(0, currentCodePoint);
                    // Find closing quote
                    while (currentCodePoint < endCodePoint) {
                        charIndex = source.offsetByCodePoints(0, currentCodePoint);
                        if (charIndex >= source.length()) break;
                        char innerC = source.charAt(charIndex);
                        if (innerC == '\\' && currentCodePoint + 1 < endCodePoint) {
                            // Skip escaped character
                            currentCodePoint += 2;
                            currentPos = source.offsetByCodePoints(0, currentCodePoint);
                        } else if (innerC == quote) {
                            currentCodePoint++;
                            currentPos = source.offsetByCodePoints(0, currentCodePoint);
                            break;
                        } else {
                            currentCodePoint++;
                            currentPos = source.offsetByCodePoints(0, currentCodePoint);
                        }
                    }
                } else if (Character.isWhitespace(c)) {
                    break;
                } else {
                    currentCodePoint++;
                    currentPos = source.offsetByCodePoints(0, currentCodePoint);
                }
            }

            // Extract the token text and store as a simple Literal
            // Don't try to parse env vars here - just preserve the raw text for lossless printing
            String tokenText = source.substring(tokenStart, currentPos);

            Docker.ArgumentContent content;
            if (tokenText.startsWith("\"") && tokenText.endsWith("\"") && tokenText.length() >= 2) {
                String value = tokenText.substring(1, tokenText.length() - 1);
                content = new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, Docker.Literal.QuoteStyle.DOUBLE);
            } else if (tokenText.startsWith("'") && tokenText.endsWith("'") && tokenText.length() >= 2) {
                String value = tokenText.substring(1, tokenText.length() - 1);
                content = new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, Docker.Literal.QuoteStyle.SINGLE);
            } else {
                // Store as plain literal - includes env vars like ${DEPENDENCY}/path
                content = new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, tokenText, null);
            }

            allPaths.add(new Docker.Argument(randomId(), argPrefix, Markers.EMPTY, singletonList(content)));
        }

        // Update cursor to end position
        cursor = endCharIndex;
        codePointCursor = endCodePoint;

        return allPaths;
    }

    @Override
    public Docker visitArgInstruction(DockerParser.ArgInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ARG keyword
        String argKeyword = ctx.ARG().getText();
        skip(ctx.ARG().getSymbol());

        // ARG name is always a simple identifier
        Space namePrefix = prefix(ctx.argName().getStart());
        skip(ctx.argName().getStop());
        Docker.Literal name = new Docker.Literal(
                randomId(),
                namePrefix,
                Markers.EMPTY,
                ctx.argName().getText(),
                null
        );

        Docker.Argument value = null;
        if (ctx.EQUALS() != null) {
            skip(ctx.EQUALS().getSymbol());
            value = visitArgument(ctx.argValue());
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Arg(randomId(), prefix, Markers.EMPTY, argKeyword, name, value);
    }

    @Override
    public Docker visitEnvInstruction(DockerParser.EnvInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ENV keyword
        String envKeyword = ctx.ENV().getText();
        skip(ctx.ENV().getSymbol());

        // Parse env pairs
        List<Docker.Env.EnvPair> pairs = new ArrayList<>();
        for (DockerParser.EnvPairContext pairCtx : ctx.envPairs().envPair()) {
            Space pairPrefix = prefix(pairCtx.getStart());

            // ENV key is always a simple identifier
            Space keyPrefix = prefix(pairCtx.envKey().getStart());
            skip(pairCtx.envKey().getStop());
            Docker.Literal key = new Docker.Literal(
                    randomId(),
                    keyPrefix,
                    Markers.EMPTY,
                    pairCtx.envKey().getText(),
                    null
            );

            boolean hasEquals = pairCtx.EQUALS() != null;
            if (hasEquals) {
                skip(pairCtx.EQUALS().getSymbol());
            }

            // Handle both forms: KEY=value (envValueEquals) or KEY value (envValueSpace)
            Docker.Argument value;
            if (pairCtx.envValueEquals() != null) {
                value = visitArgument(pairCtx.envValueEquals().envTextEquals());
            } else {
                value = visitArgument(pairCtx.envValueSpace());
            }

            pairs.add(new Docker.Env.EnvPair(randomId(), pairPrefix, Markers.EMPTY, key, hasEquals, value));
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Env(randomId(), prefix, Markers.EMPTY, envKeyword, pairs);
    }

    @Override
    public Docker visitLabelInstruction(DockerParser.LabelInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the LABEL keyword
        String labelKeyword = ctx.LABEL().getText();
        skip(ctx.LABEL().getSymbol());

        // Parse label pairs
        List<Docker.Label.LabelPair> pairs = new ArrayList<>();
        for (DockerParser.LabelPairContext pairCtx : ctx.labelPairs().labelPair()) {
            Space pairPrefix = prefix(pairCtx.getStart());
            boolean hasEquals = pairCtx.EQUALS() != null;
            Docker.Argument key;
            Docker.Argument value;

            if (hasEquals) {
                // New format: LABEL key=value
                key = visitLabelKeyOrValue(pairCtx.labelKey());
                skip(pairCtx.EQUALS().getSymbol());
                value = visitLabelKeyOrValue(pairCtx.labelValue());
            } else {
                // Old format: LABEL key value
                key = visitLabelKeyOrValue(pairCtx.labelKey());
                value = parseLabelOldValue(pairCtx.labelOldValue());
            }

            pairs.add(new Docker.Label.LabelPair(randomId(), pairPrefix, Markers.EMPTY, key, hasEquals, value));
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Label(randomId(), prefix, Markers.EMPTY, labelKeyword, pairs);
    }

    private Docker.Argument visitLabelKeyOrValue(ParserRuleContext ctx) {
        // labelKey and labelValue can be UNQUOTED_TEXT, DOUBLE_QUOTED_STRING, or SINGLE_QUOTED_STRING
        Space prefix = prefix(ctx.getStart());
        List<Docker.ArgumentContent> contents = new ArrayList<>();

        if (ctx.getChildCount() > 0) {
            ParseTree child = ctx.getChild(0);
            if (child instanceof TerminalNode) {
                TerminalNode terminal = (TerminalNode) child;
                Token token = terminal.getSymbol();
                String text = token.getText();

                if (token.getType() == DockerLexer.DOUBLE_QUOTED_STRING) {
                    // Remove quotes
                    String value = text.substring(1, text.length() - 1);
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, Docker.Literal.QuoteStyle.DOUBLE));
                    skip(token);
                } else if (token.getType() == DockerLexer.SINGLE_QUOTED_STRING) {
                    // Remove quotes
                    String value = text.substring(1, text.length() - 1);
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, Docker.Literal.QuoteStyle.SINGLE));
                    skip(token);
                } else {
                    // UNQUOTED_TEXT
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, null));
                    skip(token);
                }
            }
        }

        return new Docker.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    private Docker.Argument parseLabelOldValue(DockerParser.LabelOldValueContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<Docker.ArgumentContent> contents = new ArrayList<>();

        for (DockerParser.LabelOldValueElementContext elemCtx : ctx.labelOldValueElement()) {
            if (elemCtx.getChildCount() > 0) {
                ParseTree child = elemCtx.getChild(0);
                if (child instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) child;
                    Token token = terminal.getSymbol();
                    String text = token.getText();
                    Space elementPrefix = prefix(token);
                    skip(token);

                    if (token.getType() == DockerLexer.DOUBLE_QUOTED_STRING) {
                        String value = text.substring(1, text.length() - 1);
                        contents.add(new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, value, Docker.Literal.QuoteStyle.DOUBLE));
                    } else if (token.getType() == DockerLexer.SINGLE_QUOTED_STRING) {
                        String value = text.substring(1, text.length() - 1);
                        contents.add(new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, value, Docker.Literal.QuoteStyle.SINGLE));
                    } else if (token.getType() == DockerLexer.ENV_VAR) {
                        boolean braced = text.startsWith("${");
                        String varName = braced ?
                                text.substring(2, text.indexOf('}')) :
                                text.substring(1);
                        contents.add(new Docker.EnvironmentVariable(randomId(), elementPrefix, Markers.EMPTY, varName, braced));
                    } else {
                        // Plain text - includes UNQUOTED_TEXT, EQUALS, DASH_DASH, and instruction keywords
                        contents.add(new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, text, null));
                    }
                }
            }
        }

        advanceCursor(ctx.getStop().getStopIndex() + 1);
        return new Docker.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    @Override
    public Docker visitCmdInstruction(DockerParser.CmdInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String cmdKeyword = ctx.CMD().getText();
        skip(ctx.CMD().getSymbol());

        Docker.CommandForm command = visitCommandFormForCmd(ctx);

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Cmd(randomId(), prefix, Markers.EMPTY, cmdKeyword, command);
    }

    @Override
    public Docker visitEntrypointInstruction(DockerParser.EntrypointInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String entrypointKeyword = ctx.ENTRYPOINT().getText();
        skip(ctx.ENTRYPOINT().getSymbol());

        Docker.CommandForm command = visitCommandFormForEntrypoint(ctx);

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Entrypoint(randomId(), prefix, Markers.EMPTY, entrypointKeyword, command);
    }

    @Override
    public Docker visitExposeInstruction(DockerParser.ExposeInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the EXPOSE keyword
        String exposeKeyword = ctx.EXPOSE().getText();
        skip(ctx.EXPOSE().getSymbol());

        // Parse port list
        List<Docker.Port> ports = new ArrayList<>();
        for (DockerParser.PortContext portCtx : ctx.portList().port()) {
            ports.add(convertPort(portCtx));
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Expose(randomId(), prefix, Markers.EMPTY, exposeKeyword, ports);
    }

    private Docker.Port convertPort(DockerParser.PortContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String text;

        if (ctx.UNQUOTED_TEXT() != null) {
            Token token = ctx.UNQUOTED_TEXT().getSymbol();
            text = token.getText();
            skip(token);
        } else if (ctx.ENV_VAR() != null) {
            Token token = ctx.ENV_VAR().getSymbol();
            text = token.getText();
            skip(token);
        } else {
            // Handle other token types (COMMAND_SUBST, BACKTICK_SUBST, SPECIAL_VAR, AS)
            text = ctx.getText();
            if (ctx.getStop() != null) {
                advanceCursor(ctx.getStop().getStopIndex() + 1);
            }
        }

        return parsePort(prefix, text);
    }

    /**
     * Parses a port specification into a structured Port object.
     * Supports formats: 80, 80/tcp, 80/udp, 8000-9000, 8000-9000/tcp, ${PORT}
     */
    private Docker.Port parsePort(Space prefix, String text) {
        Integer start = null;
        Integer end = null;
        Docker.Port.Protocol protocol = Docker.Port.Protocol.TCP;

        String portPart = text;

        // Check for protocol suffix
        int slashIndex = text.lastIndexOf('/');
        if (slashIndex > 0) {
            String protocolStr = text.substring(slashIndex + 1).toLowerCase();
            if ("udp".equals(protocolStr)) {
                protocol = Docker.Port.Protocol.UDP;
            }
            portPart = text.substring(0, slashIndex);
        }

        // Check for range (e.g., 8000-9000)
        int dashIndex = portPart.indexOf('-');
        if (dashIndex > 0) {
            try {
                start = Integer.parseInt(portPart.substring(0, dashIndex));
                end = Integer.parseInt(portPart.substring(dashIndex + 1));
            } catch (NumberFormatException e) {
                // Contains variable or unparseable - leave start/end null
            }
        } else {
            // Single port
            try {
                start = Integer.parseInt(portPart);
            } catch (NumberFormatException e) {
                // Contains variable or unparseable - leave start null
            }
        }

        return new Docker.Port(randomId(), prefix, Markers.EMPTY, text, start, end, protocol);
    }

    @Override
    public Docker visitVolumeInstruction(DockerParser.VolumeInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the VOLUME keyword
        String volumeKeyword = ctx.VOLUME().getText();
        skip(ctx.VOLUME().getSymbol());

        boolean jsonForm = ctx.jsonArray() != null;
        List<Docker.Argument> values = new ArrayList<>();
        Space openingBracketPrefix = Space.EMPTY;
        Space closingBracketPrefix = Space.EMPTY;

        if (jsonForm && ctx.jsonArray() != null && ctx.jsonArray().LBRACKET() != null) {
            // Capture the whitespace before the opening bracket
            openingBracketPrefix = prefix(ctx.jsonArray().LBRACKET().getSymbol());
            // Parse JSON array
            JsonArrayParseResult result = visitJsonArrayForVolume(ctx.jsonArray());
            values = result.arguments;
            closingBracketPrefix = result.closingBracketPrefix;
        } else if (ctx.pathList() != null) {
            // Parse path list (space-separated paths)
            for (DockerParser.VolumePathContext pathCtx : ctx.pathList().volumePath()) {
                Space pathPrefix = prefix(pathCtx.getStart());
                Token token;
                String text;

                if (pathCtx.UNQUOTED_TEXT() != null) {
                    token = pathCtx.UNQUOTED_TEXT().getSymbol();
                    text = token.getText();
                    skip(token);
                    List<Docker.ArgumentContent> contents = new ArrayList<>();
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, null));
                    values.add(new Docker.Argument(randomId(), pathPrefix, Markers.EMPTY, contents));
                } else if (pathCtx.DOUBLE_QUOTED_STRING() != null) {
                    token = pathCtx.DOUBLE_QUOTED_STRING().getSymbol();
                    text = token.getText();
                    skip(token);
                    List<Docker.ArgumentContent> contents = new ArrayList<>();
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY,
                        text.substring(1, text.length() - 1), Docker.Literal.QuoteStyle.DOUBLE));
                    values.add(new Docker.Argument(randomId(), pathPrefix, Markers.EMPTY, contents));
                } else if (pathCtx.SINGLE_QUOTED_STRING() != null) {
                    token = pathCtx.SINGLE_QUOTED_STRING().getSymbol();
                    text = token.getText();
                    skip(token);
                    List<Docker.ArgumentContent> contents = new ArrayList<>();
                    contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY,
                        text.substring(1, text.length() - 1), Docker.Literal.QuoteStyle.SINGLE));
                    values.add(new Docker.Argument(randomId(), pathPrefix, Markers.EMPTY, contents));
                } else if (pathCtx.ENV_VAR() != null) {
                    token = pathCtx.ENV_VAR().getSymbol();
                    text = token.getText();
                    skip(token);
                    List<Docker.ArgumentContent> contents = new ArrayList<>();
                    contents.add(createEnvVar(text));
                    values.add(new Docker.Argument(randomId(), pathPrefix, Markers.EMPTY, contents));
                }
            }
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Volume(randomId(), prefix, Markers.EMPTY, volumeKeyword, jsonForm, openingBracketPrefix, values, closingBracketPrefix);
    }

    private JsonArrayParseResult visitJsonArrayForVolume(DockerParser.JsonArrayContext ctx) {
        // Skip the opening bracket - the space after VOLUME is handled by instruction prefix
        skip(ctx.LBRACKET().getSymbol());

        List<Docker.Argument> arguments = new ArrayList<>();
        if (ctx.jsonArrayElements() != null) {
            DockerParser.JsonArrayElementsContext elementsCtx = ctx.jsonArrayElements();
            List<DockerParser.JsonStringContext> jsonStrings = elementsCtx.jsonString();

            for (int i = 0; i < jsonStrings.size(); i++) {
                // convertJsonString captures the prefix correctly (space after [ or after ,)
                Docker.Argument arg = convertJsonString(jsonStrings.get(i));
                arguments.add(arg);

                // Skip comma after this element if it's not the last one
                // The grammar is: jsonString ( COMMA jsonString )*
                // So we need to skip the COMMA tokens
                if (i < jsonStrings.size() - 1) {
                    // Find and skip the COMMA token between this element and the next
                    for (int j = 0; j < elementsCtx.getChildCount(); j++) {
                        if (elementsCtx.getChild(j) instanceof TerminalNode) {
                            TerminalNode terminal = (TerminalNode) elementsCtx.getChild(j);
                            if (terminal.getSymbol().getType() == DockerLexer.COMMA &&
                                    terminal.getSymbol().getStartIndex() > jsonStrings.get(i).getStop().getStopIndex() &&
                                    terminal.getSymbol().getStartIndex() < jsonStrings.get(i + 1).getStart().getStartIndex()) {
                                skip(terminal.getSymbol());
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Capture whitespace before closing bracket to preserve " ]" vs "]"
        Space closingBracketPrefix = prefix(ctx.RBRACKET().getSymbol());
        skip(ctx.RBRACKET().getSymbol());

        return new JsonArrayParseResult(arguments, closingBracketPrefix);
    }

    private Docker.Argument convertJsonString(DockerParser.JsonStringContext ctx) {
        Space prefix = prefix(ctx.getStart());
        Token token = ctx.DOUBLE_QUOTED_STRING().getSymbol();
        String text = token.getText();

        // Remove quotes
        String value = text.substring(1, text.length() - 1);
        skip(token);

        // Also need to skip any COMMA token that follows (if this is not the last element)
        // The COMMA is part of jsonArrayElements, so we need to handle it there
        // Actually, we'll handle commas in the calling method

        List<Docker.ArgumentContent> contents = new ArrayList<>();
        contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, Docker.Literal.QuoteStyle.DOUBLE));
        return new Docker.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    @Override
    public Docker visitShellInstruction(DockerParser.ShellInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the SHELL keyword
        String shellKeyword = ctx.SHELL().getText();
        skip(ctx.SHELL().getSymbol());

        // Capture the whitespace before the opening bracket
        Space openingBracketPrefix = Space.EMPTY;

        // Parse JSON array (may be null or malformed in some edge cases)
        JsonArrayParseResult result;
        if (ctx.jsonArray() != null && ctx.jsonArray().LBRACKET() != null) {
            openingBracketPrefix = prefix(ctx.jsonArray().LBRACKET().getSymbol());
            result = visitJsonArrayForShell(ctx.jsonArray());
        } else {
            result = new JsonArrayParseResult(emptyList(), Space.EMPTY);
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Shell(randomId(), prefix, Markers.EMPTY, shellKeyword, openingBracketPrefix, result.arguments, result.closingBracketPrefix);
    }

    private JsonArrayParseResult visitJsonArrayForShell(DockerParser.JsonArrayContext ctx) {
        // Skip the opening bracket - the space after SHELL is handled by instruction prefix
        skip(ctx.LBRACKET().getSymbol());

        List<Docker.Argument> arguments = new ArrayList<>();
        if (ctx.jsonArrayElements() != null) {
            DockerParser.JsonArrayElementsContext elementsCtx = ctx.jsonArrayElements();
            List<DockerParser.JsonStringContext> jsonStrings = elementsCtx.jsonString();

            for (int i = 0; i < jsonStrings.size(); i++) {
                // convertJsonString captures the prefix correctly (space after [ or after ,)
                Docker.Argument arg = convertJsonString(jsonStrings.get(i));
                arguments.add(arg);

                // Skip comma after this element if it's not the last one
                if (i < jsonStrings.size() - 1) {
                    // Find and skip the COMMA token between this element and the next
                    for (int j = 0; j < elementsCtx.getChildCount(); j++) {
                        if (elementsCtx.getChild(j) instanceof TerminalNode) {
                            TerminalNode terminal = (TerminalNode) elementsCtx.getChild(j);
                            if (terminal.getSymbol().getType() == DockerLexer.COMMA &&
                                    terminal.getSymbol().getStartIndex() > jsonStrings.get(i).getStop().getStopIndex() &&
                                    terminal.getSymbol().getStartIndex() < jsonStrings.get(i + 1).getStart().getStartIndex()) {
                                skip(terminal.getSymbol());
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Capture whitespace before closing bracket to preserve " ]" vs "]"
        Space closingBracketPrefix = prefix(ctx.RBRACKET().getSymbol());
        skip(ctx.RBRACKET().getSymbol());

        return new JsonArrayParseResult(arguments, closingBracketPrefix);
    }

    @Override
    public Docker visitWorkdirInstruction(DockerParser.WorkdirInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String workdirKeyword = ctx.WORKDIR().getText();
        skip(ctx.WORKDIR().getSymbol());

        Docker.Argument path = visitArgument(ctx.path());

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Workdir(randomId(), prefix, Markers.EMPTY, workdirKeyword, path);
    }

    @Override
    public Docker visitUserInstruction(DockerParser.UserInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String userKeyword = ctx.USER().getText();
        skip(ctx.USER().getSymbol());

        // Parse userSpec and split into user and optional group
        Docker.@Nullable Argument[] userAndGroup = parseUserSpec(ctx.userSpec());
        Docker.Argument user = requireNonNull(userAndGroup[0]);
        Docker.Argument group = userAndGroup[1];

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.User(randomId(), prefix, Markers.EMPTY, userKeyword, user, group);
    }

    private Docker.@Nullable Argument[] parseUserSpec(DockerParser.UserSpecContext ctx) {
        Space prefix = prefix(ctx);

        // Parse the text
        List<Docker.ArgumentContent> contents = parseText(ctx.text());

        // Advance cursor to end of text
        advanceCursor(ctx.text().getStop().getStopIndex() + 1);

        // Find the colon separator to split user and group
        List<Docker.ArgumentContent> userContents = new ArrayList<>();
        List<Docker.ArgumentContent> groupContents = new ArrayList<>();
        boolean foundColon = false;

        for (Docker.ArgumentContent content : contents) {
            if (content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted()) {
                String text = ((Docker.Literal) content).getText();
                int colonIndex = text.indexOf(':');

                if (colonIndex >= 0 && !foundColon) {
                    // Split at the colon
                    foundColon = true;
                    String userPart = text.substring(0, colonIndex);
                    String groupPart = text.substring(colonIndex + 1);

                    if (!userPart.isEmpty()) {
                        userContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, userPart, null));
                    }
                    if (!groupPart.isEmpty()) {
                        groupContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, groupPart, null));
                    }
                } else {
                    // Add to the appropriate list
                    if (foundColon) {
                        groupContents.add(content);
                    } else {
                        userContents.add(content);
                    }
                }
            } else {
                // Environment variables or quoted strings
                if (foundColon) {
                    groupContents.add(content);
                } else {
                    userContents.add(content);
                }
            }
        }

        Docker.Argument user = new Docker.Argument(randomId(), prefix, Markers.EMPTY, userContents);
        Docker.Argument group = groupContents.isEmpty() ? null :
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, groupContents);

        return new Docker.@Nullable Argument[]{user, group};
    }

    @Override
    public Docker visitStopsignalInstruction(DockerParser.StopsignalInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String stopsignalKeyword = ctx.STOPSIGNAL().getText();
        skip(ctx.STOPSIGNAL().getSymbol());

        Docker.Argument signal = visitArgument(ctx.signal());

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Stopsignal(randomId(), prefix, Markers.EMPTY, stopsignalKeyword, signal);
    }

    @Override
    public Docker visitOnbuildInstruction(DockerParser.OnbuildInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String onbuildKeyword = ctx.ONBUILD().getText();
        skip(ctx.ONBUILD().getSymbol());

        // Visit the wrapped instruction
        Docker.Instruction instruction = (Docker.Instruction) visit(ctx.instruction());

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Onbuild(randomId(), prefix, Markers.EMPTY, onbuildKeyword, instruction);
    }

    @Override
    public Docker visitHealthcheckInstruction(DockerParser.HealthcheckInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String healthcheckKeyword = ctx.HEALTHCHECK().getText();
        skip(ctx.HEALTHCHECK().getSymbol());

        List<Docker.Flag> flags = null;
        Space nonePrefix = Space.EMPTY;
        Docker.@Nullable Cmd cmd = null;

        // Check if this is HEALTHCHECK NONE
        if (ctx.NONE() != null) {
            // HEALTHCHECK NONE - capture the whitespace before NONE, cmd stays null
            nonePrefix = prefix(ctx.NONE().getSymbol());
            skip(ctx.NONE().getSymbol());
        } else {
            // HEALTHCHECK [options] CMD (execForm | shellForm)
            if (ctx.healthcheckOptions() != null) {
                flags = convertHealthcheckOptions(ctx.healthcheckOptions());
            }

            // Parse CMD and its form
            Space cmdPrefix = prefix(ctx.CMD().getSymbol());
            String cmdKeywordText = ctx.CMD().getText();
            skip(ctx.CMD().getSymbol());

            Docker.CommandForm form;
            if (ctx.execForm() != null) {
                form = visitExecFormContext(ctx.execForm());
            } else if (ctx.shellForm() != null) {
                form = visitShellFormContext(ctx.shellForm());
            } else {
                // Should not happen, but provide a fallback
                form = new Docker.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY,
                        new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "", null));
            }

            cmd = new Docker.Cmd(randomId(), cmdPrefix, Markers.EMPTY, cmdKeywordText, form);
        }

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Healthcheck(randomId(), prefix, Markers.EMPTY, healthcheckKeyword, flags, nonePrefix, cmd);
    }

    private List<Docker.Flag> convertHealthcheckOptions(DockerParser.HealthcheckOptionsContext ctx) {
        List<Docker.Flag> flags = new ArrayList<>();
        for (DockerParser.HealthcheckOptionContext optCtx : ctx.healthcheckOption()) {
            Space flagPrefix = prefix(optCtx.getStart());
            skip(optCtx.DASH_DASH().getSymbol());

            String flagName = optCtx.flagName().getText();
            advanceCursor(optCtx.flagName().getStop().getStopIndex() + 1);

            skip(optCtx.EQUALS().getSymbol());

            // Parse the single-token flag value
            DockerParser.HealthcheckOptionValueContext valueCtx = optCtx.healthcheckOptionValue();
            Docker.Argument flagValue = parseHealthcheckOptionValue(valueCtx);

            flags.add(new Docker.Flag(randomId(), flagPrefix, Markers.EMPTY, flagName, flagValue));
        }
        return flags;
    }

    private Docker.Argument parseHealthcheckOptionValue(DockerParser.HealthcheckOptionValueContext ctx) {
        Space argPrefix = prefix(ctx);
        List<Docker.ArgumentContent> contents = new ArrayList<>();

        TerminalNode terminal = (TerminalNode) ctx.getChild(0);
        Token token = terminal.getSymbol();
        String tokenText = token.getText();
        Space elementPrefix = prefix(token);
        skip(token);

        Docker.ArgumentContent content;
        if (ctx.DOUBLE_QUOTED_STRING() != null) {
            String value = tokenText.substring(1, tokenText.length() - 1);
            content = new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, value, Docker.Literal.QuoteStyle.DOUBLE);
        } else if (ctx.SINGLE_QUOTED_STRING() != null) {
            String value = tokenText.substring(1, tokenText.length() - 1);
            content = new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, value, Docker.Literal.QuoteStyle.SINGLE);
        } else if (ctx.ENV_VAR() != null) {
            boolean braced = tokenText.startsWith("${");
            String varName = braced ? tokenText.substring(2, tokenText.length() - 1) : tokenText.substring(1);
            content = new Docker.EnvironmentVariable(randomId(), elementPrefix, Markers.EMPTY, varName, braced);
        } else {
            content = new Docker.Literal(randomId(), elementPrefix, Markers.EMPTY, tokenText, null);
        }
        contents.add(content);

        return new Docker.Argument(randomId(), argPrefix, Markers.EMPTY, contents);
    }

    @Override
    public Docker visitMaintainerInstruction(DockerParser.MaintainerInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String maintainerKeyword = ctx.MAINTAINER().getText();
        skip(ctx.MAINTAINER().getSymbol());

        Docker.Argument text = visitArgument(ctx.text());

        // Advance cursor to end of instruction
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }

        return new Docker.Maintainer(randomId(), prefix, Markers.EMPTY, maintainerKeyword, text);
    }

    private Docker.CommandForm visitCommandFormForRun(DockerParser.RunInstructionContext ctx) {
        if (ctx.execForm() != null) {
            return visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            return visitShellFormContext(ctx.shellForm());
        } else if (ctx.heredoc() != null) {
            return visitHeredocContext(ctx.heredoc());
        } else {
            // Fallback to empty shell form
            return new Docker.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "", null));
        }
    }

    private Docker.CommandForm visitCommandFormForCmd(DockerParser.CmdInstructionContext ctx) {
        if (ctx.execForm() != null) {
            return visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            return visitShellFormContext(ctx.shellForm());
        } else {
            return new Docker.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "", null));
        }
    }

    private Docker.CommandForm visitCommandFormForEntrypoint(DockerParser.EntrypointInstructionContext ctx) {
        if (ctx.execForm() != null) {
            return visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            return visitShellFormContext(ctx.shellForm());
        } else {
            return new Docker.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "", null));
        }
    }

    private List<Docker.Flag> convertFlags(DockerParser.FlagsContext ctx) {
        List<Docker.Flag> flags = new ArrayList<>();
        for (DockerParser.FlagContext flagCtx : ctx.flag()) {
            Space flagPrefix = prefix(flagCtx.getStart());
            skip(flagCtx.DASH_DASH().getSymbol());

            String flagName = flagCtx.flagName().getText();
            advanceCursor(flagCtx.flagName().getStop().getStopIndex() + 1);

            Docker.Argument flagValue = null;
            if (flagCtx.EQUALS() != null) {
                skip(flagCtx.EQUALS().getSymbol());
                flagValue = parseFlagValue(flagCtx.flagValue());
            }

            flags.add(new Docker.Flag(randomId(), flagPrefix, Markers.EMPTY, flagName, flagValue));
        }
        return flags;
    }

    private Docker.Argument parseFlagValue(DockerParser.FlagValueContext ctx) {
        if (ctx == null) {
            return new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        Space argPrefix = prefix(ctx);
        List<Docker.ArgumentContent> contents = new ArrayList<>();

        // Process flagValueToken+ with whitespace detection
        // Stop consuming tokens if there's whitespace between them (indicates new argument)
        for (DockerParser.FlagValueTokenContext tokenCtx : ctx.flagValueToken()) {
            if (tokenCtx.getChildCount() > 0 && tokenCtx.getChild(0) instanceof TerminalNode) {
                TerminalNode terminal = (TerminalNode) tokenCtx.getChild(0);
                Token token = terminal.getSymbol();

                // Check if there's whitespace before this token (gap in character positions)
                // If so, this token belongs to the next argument, not the flag value
                if (!contents.isEmpty() && token.getStartIndex() > codePointCursor) {
                    break;
                }

                String tokenText = token.getText();
                Space elementPrefix = prefix(token);
                skip(token);

                if (token.getType() == DockerLexer.DOUBLE_QUOTED_STRING) {
                    String value = tokenText.substring(1, tokenText.length() - 1);
                    contents.add(new Docker.Literal(
                            randomId(),
                            elementPrefix,
                            Markers.EMPTY,
                            value,
                            Docker.Literal.QuoteStyle.DOUBLE
                    ));
                } else if (token.getType() == DockerLexer.SINGLE_QUOTED_STRING) {
                    String value = tokenText.substring(1, tokenText.length() - 1);
                    contents.add(new Docker.Literal(
                            randomId(),
                            elementPrefix,
                            Markers.EMPTY,
                            value,
                            Docker.Literal.QuoteStyle.SINGLE
                    ));
                } else if (token.getType() == DockerLexer.ENV_VAR) {
                    boolean braced = tokenText.startsWith("${");
                    String varName = braced ? tokenText.substring(2, tokenText.length() - 1) : tokenText.substring(1);
                    contents.add(new Docker.EnvironmentVariable(
                            randomId(),
                            elementPrefix,
                            Markers.EMPTY,
                            varName,
                            braced
                    ));
                } else {
                    // Plain text for UNQUOTED_TEXT, EQUALS, etc.
                    contents.add(new Docker.Literal(
                            randomId(),
                            elementPrefix,
                            Markers.EMPTY,
                            tokenText,
                            null
                    ));
                }
            }
        }

        return new Docker.Argument(randomId(), argPrefix, Markers.EMPTY, contents);
    }

    private Docker.ShellForm visitShellFormContext(DockerParser.ShellFormContext ctx) {
        return convert(ctx, (c, prefix) -> {
            // Parse the shell form text as a single Literal
            int startIndex = c.shellFormText().getStart().getStartIndex();
            int stopIndex = c.shellFormText().getStop().getStopIndex();
            int startCharIndex = source.offsetByCodePoints(0, startIndex);
            int stopCharIndex = source.offsetByCodePoints(0, stopIndex + 1);
            String fullText = source.substring(startCharIndex, stopCharIndex);

            Docker.Literal literal = new Docker.Literal(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    fullText,
                    null
            );
            return new Docker.ShellForm(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    literal
            );
        });
    }

    private Docker.ExecForm visitExecFormContext(DockerParser.ExecFormContext ctx) {
        return convert(ctx, (c, prefix) -> {
            DockerParser.JsonArrayContext jsonArray = c.jsonArray();
            return visitJsonArrayAsExecFormInternal(jsonArray, prefix);
        });
    }

    /**
     * Parse a JSON array directly as an ExecForm (used by COPY/ADD instructions)
     */
    private Docker.ExecForm visitJsonArrayAsExecForm(DockerParser.JsonArrayContext ctx) {
        Space prefix = prefix(ctx.getStart());
        return visitJsonArrayAsExecFormInternal(ctx, prefix);
    }

    private Docker.ExecForm visitJsonArrayAsExecFormInternal(DockerParser.JsonArrayContext jsonArray, Space prefix) {
        List<Docker.Literal> args = new ArrayList<>();

        skip(jsonArray.LBRACKET().getSymbol());

        if (jsonArray.jsonArrayElements() != null) {
            DockerParser.JsonArrayElementsContext elementsCtx = jsonArray.jsonArrayElements();
            List<DockerParser.JsonStringContext> jsonStrings = elementsCtx.jsonString();

            for (int i = 0; i < jsonStrings.size(); i++) {
                DockerParser.JsonStringContext jsonStr = jsonStrings.get(i);

                // Capture the prefix including any comma before this element
                // For first element: captures whitespace after [
                // For subsequent elements: captures whitespace + comma + whitespace
                // This preserves the original formatting like ["-quic" ,"--conf"]
                Space argPrefix = prefix(jsonStr.getStart());
                String value = jsonStr.DOUBLE_QUOTED_STRING().getText();
                // Remove surrounding quotes
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                advanceCursor(jsonStr.DOUBLE_QUOTED_STRING().getSymbol().getStopIndex() + 1);

                args.add(new Docker.Literal(
                        randomId(),
                        argPrefix,
                        Markers.EMPTY,
                        value,
                        Docker.Literal.QuoteStyle.DOUBLE
                ));
            }
        }

        // Capture whitespace before closing bracket to preserve " ]" vs "]"
        Space closingBracketPrefix = prefix(jsonArray.RBRACKET().getSymbol());
        skip(jsonArray.RBRACKET().getSymbol());

        return new Docker.ExecForm(randomId(), prefix, Markers.EMPTY, args, closingBracketPrefix);
    }

    /**
     * Parse an exec form from a string like: {@code ["curl", "-f", "http://localhost/"]}
     * Used when the ANTLR grammar's greedy flagValue rule has consumed the exec form tokens.
     * Note: Commas are captured as part of the next element's prefix to match how the printer works.
     */
    private Docker.ExecForm parseExecFormFromText(String text) {
        List<Docker.Literal> args = new ArrayList<>();

        // Find leading whitespace before [
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        Space prefix = Space.format(text.substring(0, i));

        // Find opening bracket
        if (i >= text.length() || text.charAt(i) != '[') {
            // Fallback - return empty exec form
            return new Docker.ExecForm(randomId(), prefix, Markers.EMPTY, args, Space.EMPTY);
        }
        i++; // skip [

        boolean firstElement = true;

        // Parse elements: "string" separated by commas
        while (i < text.length()) {
            // Capture prefix: for first element it's whitespace after [
            // for subsequent elements it's comma + whitespace (comma included in prefix)
            int prefixStart = i;

            // Skip whitespace
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }

            if (i >= text.length()) break;

            // Check for closing bracket
            if (text.charAt(i) == ']') {
                // Closing bracket found - remaining whitespace is the closing bracket prefix
                return new Docker.ExecForm(randomId(), prefix, Markers.EMPTY, args,
                        Space.format(text.substring(prefixStart, i)));
            }

            // Handle comma for non-first elements
            if (text.charAt(i) == ',') {
                // Include comma in prefix for next element
                prefixStart = i; // start prefix at comma
                i++; // skip comma
                // Skip whitespace after comma
                while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
            }

            if (i >= text.length()) break;

            // Check for closing bracket again
            if (text.charAt(i) == ']') {
                return new Docker.ExecForm(randomId(), prefix, Markers.EMPTY, args,
                        Space.format(text.substring(prefixStart, i)));
            }

            String elemPrefixStr = text.substring(prefixStart, i);
            Space elemPrefix = Space.format(elemPrefixStr);

            // Parse quoted string
            if (text.charAt(i) == '"') {
                i++; // skip opening quote
                StringBuilder value = new StringBuilder();
                while (i < text.length() && text.charAt(i) != '"') {
                    // Handle escapes
                    if (text.charAt(i) == '\\' && i + 1 < text.length()) {
                        i++; // skip backslash
                        value.append(text.charAt(i));
                    } else {
                        value.append(text.charAt(i));
                    }
                    i++;
                }
                if (i < text.length() && text.charAt(i) == '"') {
                    i++; // skip closing quote
                }
                args.add(new Docker.Literal(randomId(), elemPrefix, Markers.EMPTY, value.toString(),
                        Docker.Literal.QuoteStyle.DOUBLE));
            } else {
                // Unexpected - skip to next comma or bracket
                while (i < text.length() && text.charAt(i) != ',' && text.charAt(i) != ']') {
                    i++;
                }
            }
        }

        return new Docker.ExecForm(randomId(), prefix, Markers.EMPTY, args, Space.EMPTY);
    }

    private Docker.HeredocForm visitHeredocContext(DockerParser.HeredocContext ctx) {
        return convert(ctx, (c, prefix) -> {
            // Get opening marker (<<EOF or <<-EOF)
            String opening = c.HEREDOC_START().getText();
            skip(c.HEREDOC_START().getSymbol());

            // Check for optional destination (for COPY/ADD with inline destination)
            Docker.Argument destination = null;
            if (c.path() != null) {
                destination = visitArgument(c.path());
            }

            // Collect content lines
            List<String> contentLines = new ArrayList<>();

            // Add the opening newline first (after HEREDOC_START and optional path)
            if (c.NEWLINE() != null) {
                String openingNewline = c.NEWLINE().getText();
                contentLines.add(openingNewline);
                skip(c.NEWLINE().getSymbol());
            }

            // Process heredocContent which is ( NEWLINE | HEREDOC_CONTENT )*
            if (c.heredocContent() != null) {
                DockerParser.HeredocContentContext contentCtx = c.heredocContent();
                StringBuilder currentLine = new StringBuilder();

                for (int i = 0; i < contentCtx.getChildCount(); i++) {
                    ParseTree child = contentCtx.getChild(i);

                    if (child instanceof TerminalNode) {
                        TerminalNode tn = (TerminalNode) child;
                        if (tn.getSymbol().getType() == DockerLexer.NEWLINE) {
                            // Newline - end current line and start new one
                            currentLine.append(tn.getText());
                            skip(tn.getSymbol());
                            contentLines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                        } else if (tn.getSymbol().getType() == DockerLexer.HEREDOC_CONTENT) {
                            // Heredoc content line
                            currentLine.append(tn.getText());
                            skip(tn.getSymbol());
                        }
                    }
                }

                // Add any remaining content as a line
                if (currentLine.length() > 0) {
                    contentLines.add(currentLine.toString());
                }
            }

            // Get closing marker (UNQUOTED_TEXT)
            String closing = c.heredocEnd().UNQUOTED_TEXT().getText();
            skip(c.heredocEnd().UNQUOTED_TEXT().getSymbol());

            return new Docker.HeredocForm(randomId(), prefix, Markers.EMPTY, opening, destination, contentLines, closing);
        });
    }

    private Docker.Argument visitArgument(@Nullable ParserRuleContext ctx) {
        if (ctx == null) {
            return new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        return convert(ctx, (c, prefix) -> {
            // Read from source to include HIDDEN channel tokens (whitespace, comments)
            int startIndex = c.getStart().getStartIndex();
            int stopIndex = c.getStop().getStopIndex();

            // Defensive check for invalid ranges
            if (stopIndex < startIndex) {
                return new Docker.Argument(randomId(), prefix, Markers.EMPTY, emptyList());
            }

            int startCharIndex = source.offsetByCodePoints(0, startIndex);
            int stopCharIndex = source.offsetByCodePoints(0, stopIndex + 1);

            if (stopCharIndex < startCharIndex) {
                return new Docker.Argument(randomId(), prefix, Markers.EMPTY, emptyList());
            }

            String fullText = source.substring(startCharIndex, stopCharIndex);

            Docker.Literal plainText = new Docker.Literal(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    fullText,
                    null
            );
            return new Docker.Argument(randomId(), prefix, Markers.EMPTY, singletonList(plainText));
        });
    }

    /**
     * Parse an ENV_VAR token text (e.g., "${VAR}" or "$VAR") into an EnvironmentVariable content
     */
    private Docker.EnvironmentVariable createEnvVar(String text) {
        boolean braced = text.startsWith("${");
        String varName = braced ? text.substring(2, text.length() - 1) : text.substring(1);
        return new Docker.EnvironmentVariable(randomId(), Space.EMPTY, Markers.EMPTY, varName, braced);
    }

    // Helper methods for cursor management

    private Space prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private Space prefix(Token token) {
        int start = token.getStartIndex();
        if (start < codePointCursor) {
            return Space.EMPTY;
        }
        return Space.format(source, cursor, advanceCursor(start));
    }

    private int advanceCursor(int newCodePointIndex) {
        if (newCodePointIndex <= codePointCursor) {
            return cursor;
        }
        cursor = source.offsetByCodePoints(cursor, newCodePointIndex - codePointCursor);
        codePointCursor = newCodePointIndex;
        return cursor;
    }

    private void skip(@Nullable Token token) {
        if (token != null) {
            advanceCursor(token.getStopIndex() + 1);
        }
    }

    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, Space, T> conversion) {
        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }
        return t;
    }

    /**
     * Helper class to hold both the arguments and closing bracket prefix when parsing JSON arrays.
     */
    private static class JsonArrayParseResult {
        final List<Docker.Argument> arguments;
        final Space closingBracketPrefix;

        JsonArrayParseResult(List<Docker.Argument> arguments, Space closingBracketPrefix) {
            this.arguments = arguments;
            this.closingBracketPrefix = closingBracketPrefix;
        }
    }
}
