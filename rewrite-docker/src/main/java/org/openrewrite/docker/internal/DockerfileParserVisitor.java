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
package org.openrewrite.docker.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.docker.internal.grammar.DockerfileLexer;
import org.openrewrite.docker.internal.grammar.DockerfileParser;
import org.openrewrite.docker.internal.grammar.DockerfileParserBaseVisitor;
import org.openrewrite.docker.tree.Dockerfile;
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
import static org.openrewrite.Tree.randomId;

public class DockerfileParserVisitor extends DockerfileParserBaseVisitor<Dockerfile> {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    private int cursor = 0;
    private int codePointCursor = 0;

    public DockerfileParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    public Dockerfile.Document visitDockerfile(DockerfileParser.DockerfileContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Parse global ARG instructions (before first FROM)
        List<Dockerfile.Arg> globalArgs = new ArrayList<>();
        if (ctx.globalArgs() != null) {
            for (DockerfileParser.ArgInstructionContext argCtx : ctx.globalArgs().argInstruction()) {
                Dockerfile.Arg arg = (Dockerfile.Arg) visit(argCtx);
                if (arg != null) {
                    globalArgs.add(arg);
                }
            }
        }

        // Parse build stages
        List<Dockerfile.Stage> stages = new ArrayList<>();
        for (DockerfileParser.StageContext stageCtx : ctx.stage()) {
            Dockerfile.Stage stage = visitStage(stageCtx);
            if (stage != null) {
                stages.add(stage);
            }
        }

        return new Dockerfile.Document(
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
    public Dockerfile.Stage visitStage(DockerfileParser.StageContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Parse the FROM instruction that starts this stage
        Dockerfile.From from = (Dockerfile.From) visit(ctx.fromInstruction());

        // Parse stage instructions
        List<Dockerfile.Instruction> instructions = new ArrayList<>();
        for (DockerfileParser.StageInstructionContext instructionCtx : ctx.stageInstruction()) {
            Dockerfile.Instruction instruction = (Dockerfile.Instruction) visit(instructionCtx);
            if (instruction != null) {
                instructions.add(instruction);
            }
        }

        return new Dockerfile.Stage(
                randomId(),
                prefix,
                Markers.EMPTY,
                from,
                instructions
        );
    }

    @Override
    public Dockerfile visitFromInstruction(DockerfileParser.FromInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the FROM keyword
        String fromKeyword = ctx.FROM().getText();
        skip(ctx.FROM().getSymbol());

        List<Dockerfile.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;
        Dockerfile.Argument[] imageComponents = parseImageName(ctx.imageName());
        Dockerfile.Argument imageName = imageComponents[0];
        Dockerfile.Argument tag = imageComponents[1];
        Dockerfile.Argument digest = imageComponents[2];
        Dockerfile.From.As as = ctx.AS() != null ? visitFromAs(ctx) : null;

        // Cursor has already been advanced by parseImageName and other parsing methods
        // No additional advancement needed here

        return new Dockerfile.From(randomId(), prefix, Markers.EMPTY, fromKeyword, flags, imageName, tag, digest, as);
    }

    private Dockerfile.From.As visitFromAs(DockerfileParser.FromInstructionContext ctx) {
        Space asPrefix = prefix(ctx.AS().getSymbol());
        String asKeyword = ctx.AS().getText();
        skip(ctx.AS().getSymbol());
        return new Dockerfile.From.As(
                randomId(),
                asPrefix,
                Markers.EMPTY,
                asKeyword,
                visitArgument(ctx.stageName())
        );
    }

    private Dockerfile.Argument[] parseImageName(DockerfileParser.ImageNameContext ctx) {
        Space prefix = prefix(ctx);

        // Parse the text and split out environment variables
        List<Dockerfile.ArgumentContent> contents = parseText(ctx.text());

        // Advance cursor only to the last non-comment token to avoid consuming trailing comments
        Token lastToken = findLastNonCommentToken(ctx.text());
        if (lastToken != null) {
            advanceCursor(lastToken.getStopIndex() + 1);
        }

        // If the entire image is a single quoted string, don't split it
        if (contents.size() == 1 && contents.get(0) instanceof Dockerfile.QuotedString) {
            // Single quoted string - keep it as-is
            Dockerfile.Argument imageName = new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, contents);
            return new Dockerfile.Argument[]{imageName, null, null};
        }

        // Split contents into imageName, tag, and digest components
        List<Dockerfile.ArgumentContent> imageNameContents = new ArrayList<>();
        List<Dockerfile.ArgumentContent> tagContents = new ArrayList<>();
        List<Dockerfile.ArgumentContent> digestContents = new ArrayList<>();

        boolean foundColon = false;
        boolean foundAt = false;

        for (Dockerfile.ArgumentContent content : contents) {
            if (content instanceof Dockerfile.PlainText) {
                String text = ((Dockerfile.PlainText) content).getText();

                // Look for @ first (digest takes precedence over tag)
                int atIndex = text.indexOf('@');
                int colonIndex = text.indexOf(':');

                if (atIndex >= 0 && !foundAt) {
                    // Split at @
                    foundAt = true;
                    String imagePart = text.substring(0, atIndex);
                    String digestPart = text.substring(atIndex + 1);

                    if (!imagePart.isEmpty()) {
                        imageNameContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, imagePart));
                    }
                    if (!digestPart.isEmpty()) {
                        digestContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, digestPart));
                    }
                } else if (colonIndex >= 0 && !foundColon && !foundAt) {
                    // Split at :
                    foundColon = true;
                    String imagePart = text.substring(0, colonIndex);
                    String tagPart = text.substring(colonIndex + 1);

                    if (!imagePart.isEmpty()) {
                        imageNameContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, imagePart));
                    }
                    if (!tagPart.isEmpty()) {
                        tagContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, tagPart));
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

        Dockerfile.Argument imageName = new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, imageNameContents);
        Dockerfile.Argument tag = tagContents.isEmpty() ? null :
                new Dockerfile.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tagContents);
        Dockerfile.Argument digest = digestContents.isEmpty() ? null :
                new Dockerfile.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digestContents);

        return new Dockerfile.Argument[]{imageName, tag, digest};
    }

    private List<Dockerfile.ArgumentContent> parseText(DockerfileParser.TextContext textCtx) {
        List<Dockerfile.ArgumentContent> contents = new ArrayList<>();

        if (textCtx == null) {
            return contents;
        }

        // Check if text contains quoted strings, environment variables, or comments
        String fullText = textCtx.getText();
        boolean hasQuotedString = fullText.contains("\"") || fullText.contains("'");
        boolean hasEnvironmentVariable = fullText.contains("$");
        boolean hasComment = fullText.contains("#");

        if (!hasQuotedString && !hasEnvironmentVariable && !hasComment) {
            // Simple case: just plain text
            contents.add(new Dockerfile.PlainText(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    fullText
            ));
            return contents;
        }

        // Complex case: parse token by token
        boolean foundComment = false;
        for (int i = textCtx.getChildCount() - 1; i >= 0; i--) {
            ParseTree child = textCtx.getChild(i);
            if (child instanceof DockerfileParser.TextElementContext) {
                DockerfileParser.TextElementContext textElement = (DockerfileParser.TextElementContext) child;
                if (textElement.getChildCount() > 0 && textElement.getChild(0) instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) textElement.getChild(0);
                    if (terminal.getSymbol().getType() == DockerfileLexer.COMMENT) {
                        foundComment = true;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < textCtx.getChildCount(); i++) {
            ParseTree child = textCtx.getChild(i);

            if (child instanceof DockerfileParser.TextElementContext) {
                DockerfileParser.TextElementContext textElement = (DockerfileParser.TextElementContext) child;
                if (textElement.getChildCount() > 0 && textElement.getChild(0) instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) textElement.getChild(0);
                    Token token = terminal.getSymbol();
                    String tokenText = token.getText();

                    if (token.getType() == DockerfileLexer.COMMENT) {
                        // COMMENT tokens are ignored - they will be part of next element's prefix
                        break; // Stop processing tokens once we hit a comment
                    } else if (token.getType() == DockerfileLexer.DOUBLE_QUOTED_STRING) {
                        String value = tokenText.substring(1, tokenText.length() - 1);
                        contents.add(new Dockerfile.QuotedString(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                value,
                                Dockerfile.QuotedString.QuoteStyle.DOUBLE
                        ));
                    } else if (token.getType() == DockerfileLexer.SINGLE_QUOTED_STRING) {
                        String value = tokenText.substring(1, tokenText.length() - 1);
                        contents.add(new Dockerfile.QuotedString(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                value,
                                Dockerfile.QuotedString.QuoteStyle.SINGLE
                        ));
                    } else if (token.getType() == DockerfileLexer.ENV_VAR) {
                        boolean braced = tokenText.startsWith("${");
                        String varName = braced ? tokenText.substring(2, tokenText.length() - 1) : tokenText.substring(1);
                        contents.add(new Dockerfile.EnvironmentVariable(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                varName,
                                braced
                        ));
                    } else if (token.getType() == DockerfileLexer.WS && foundComment) {
                        // Skip whitespace before comments - it should be part of Space/prefix
                        // But keep processing in case there's more content after
                    } else {
                        // Plain text for other tokens
                        contents.add(new Dockerfile.PlainText(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                tokenText
                        ));
                    }
                }
            }
        }

        // Remove trailing whitespace PlainText entries that were added before we knew about the comment
        while (!contents.isEmpty() && contents.get(contents.size() - 1) instanceof Dockerfile.PlainText) {
            Dockerfile.PlainText last = (Dockerfile.PlainText) contents.get(contents.size() - 1);
            if (last.getText().trim().isEmpty()) {
                contents.remove(contents.size() - 1);
            } else {
                break;
            }
        }

        return contents;
    }

    @Override
    public Dockerfile visitRunInstruction(DockerfileParser.RunInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the RUN keyword
        String runKeyword = ctx.RUN().getText();
        skip(ctx.RUN().getSymbol());

        List<Dockerfile.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;
        Dockerfile.CommandLine commandLine = visitCommandLine(ctx);

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                // Don't advance past the trailing comment
                if (ctx.execForm() != null) {
                    stopToken = ctx.execForm().getStop();
                } else if (ctx.shellForm() != null) {
                    stopToken = ctx.shellForm().getStop();
                } else if (ctx.heredoc() != null) {
                    stopToken = ctx.heredoc().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Run(randomId(), prefix, Markers.EMPTY, runKeyword, flags, commandLine);
    }

    @Override
    public Dockerfile visitAddInstruction(DockerfileParser.AddInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ADD keyword
        String addKeyword = ctx.ADD().getText();
        skip(ctx.ADD().getSymbol());

        List<Dockerfile.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;

        Dockerfile.HeredocForm heredoc = null;
        List<Dockerfile.Argument> sources = null;
        Dockerfile.Argument destination = null;

        // Check if heredoc or sourceList is present
        if (ctx.heredoc() != null) {
            heredoc = visitHeredocContext(ctx.heredoc());
            // For heredoc, destination is part of the heredoc (if present)
            // No separate destination to parse
        } else if (ctx.sourceList() != null) {
            sources = new ArrayList<>();
            for (DockerfileParser.SourceContext sourceCtx : ctx.sourceList().source()) {
                sources.add(visitArgument(sourceCtx.path()));
            }
            // For sourceList, destination is separate
            destination = visitArgument(ctx.destination().path());
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                // Don't advance past the trailing comment
                if (ctx.heredoc() != null) {
                    stopToken = ctx.heredoc().getStop();
                } else if (ctx.destination() != null) {
                    stopToken = ctx.destination().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Add(randomId(), prefix, Markers.EMPTY, addKeyword, flags, heredoc, sources, destination);
    }

    @Override
    public Dockerfile visitCopyInstruction(DockerfileParser.CopyInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the COPY keyword
        String copyKeyword = ctx.COPY().getText();
        skip(ctx.COPY().getSymbol());

        List<Dockerfile.Flag> flags = ctx.flags() != null ? convertFlags(ctx.flags()) : null;

        Dockerfile.HeredocForm heredoc = null;
        List<Dockerfile.Argument> sources = null;
        Dockerfile.Argument destination = null;

        // Check if heredoc or sourceList is present
        if (ctx.heredoc() != null) {
            heredoc = visitHeredocContext(ctx.heredoc());
            // For heredoc, destination is part of the heredoc (if present)
            // No separate destination to parse
        } else if (ctx.sourceList() != null) {
            sources = new ArrayList<>();
            for (DockerfileParser.SourceContext sourceCtx : ctx.sourceList().source()) {
                sources.add(visitArgument(sourceCtx.path()));
            }
            // For sourceList, destination is separate
            destination = visitArgument(ctx.destination().path());
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                // Don't advance past the trailing comment
                if (ctx.heredoc() != null) {
                    stopToken = ctx.heredoc().getStop();
                } else if (ctx.destination() != null) {
                    stopToken = ctx.destination().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Copy(randomId(), prefix, Markers.EMPTY, copyKeyword, flags, heredoc, sources, destination);
    }

    @Override
    public Dockerfile visitArgInstruction(DockerfileParser.ArgInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ARG keyword
        String argKeyword = ctx.ARG().getText();
        skip(ctx.ARG().getSymbol());

        Dockerfile.Argument name = visitArgument(ctx.argName());

        Dockerfile.Argument value = null;
        if (ctx.EQUALS() != null) {
            skip(ctx.EQUALS().getSymbol());
            value = visitArgument(ctx.argValue());
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                // Don't advance past the trailing comment
                if (ctx.argValue() != null) {
                    stopToken = ctx.argValue().getStop();
                } else {
                    stopToken = ctx.argName().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Arg(randomId(), prefix, Markers.EMPTY, argKeyword, name, value);
    }

    @Override
    public Dockerfile visitEnvInstruction(DockerfileParser.EnvInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the ENV keyword
        String envKeyword = ctx.ENV().getText();
        skip(ctx.ENV().getSymbol());

        // Parse env pairs
        List<Dockerfile.Env.EnvPair> pairs = new ArrayList<>();
        for (DockerfileParser.EnvPairContext pairCtx : ctx.envPairs().envPair()) {
            Space pairPrefix = prefix(pairCtx.getStart());
            Dockerfile.Argument key = visitArgument(pairCtx.envKey());

            boolean hasEquals = pairCtx.EQUALS() != null;
            if (hasEquals) {
                skip(pairCtx.EQUALS().getSymbol());
            }

            Dockerfile.Argument value = visitArgument(pairCtx.envValue());

            pairs.add(new Dockerfile.Env.EnvPair(randomId(), pairPrefix, Markers.EMPTY, key, hasEquals, value));
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.envPairs().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Env(randomId(), prefix, Markers.EMPTY, envKeyword, pairs);
    }

    @Override
    public Dockerfile visitLabelInstruction(DockerfileParser.LabelInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the LABEL keyword
        String labelKeyword = ctx.LABEL().getText();
        skip(ctx.LABEL().getSymbol());

        // Parse label pairs
        List<Dockerfile.Label.LabelPair> pairs = new ArrayList<>();
        for (DockerfileParser.LabelPairContext pairCtx : ctx.labelPairs().labelPair()) {
            Space pairPrefix = prefix(pairCtx.getStart());
            Dockerfile.Argument key = visitLabelKeyOrValue(pairCtx.labelKey());

            // LABEL always has equals
            skip(pairCtx.EQUALS().getSymbol());

            Dockerfile.Argument value = visitLabelKeyOrValue(pairCtx.labelValue());

            pairs.add(new Dockerfile.Label.LabelPair(randomId(), pairPrefix, Markers.EMPTY, key, value));
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.labelPairs().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Label(randomId(), prefix, Markers.EMPTY, labelKeyword, pairs);
    }

    private Dockerfile.Argument visitLabelKeyOrValue(ParserRuleContext ctx) {
        // labelKey and labelValue can be UNQUOTED_TEXT, DOUBLE_QUOTED_STRING, or SINGLE_QUOTED_STRING
        Space prefix = prefix(ctx.getStart());
        List<Dockerfile.ArgumentContent> contents = new ArrayList<>();

        if (ctx.getChildCount() > 0) {
            ParseTree child = ctx.getChild(0);
            if (child instanceof TerminalNode) {
                TerminalNode terminal = (TerminalNode) child;
                Token token = terminal.getSymbol();
                String text = token.getText();

                if (token.getType() == DockerfileLexer.DOUBLE_QUOTED_STRING) {
                    // Remove quotes
                    String value = text.substring(1, text.length() - 1);
                    contents.add(new Dockerfile.QuotedString(randomId(), Space.EMPTY, Markers.EMPTY, value, Dockerfile.QuotedString.QuoteStyle.DOUBLE));
                    skip(token);
                } else if (token.getType() == DockerfileLexer.SINGLE_QUOTED_STRING) {
                    // Remove quotes
                    String value = text.substring(1, text.length() - 1);
                    contents.add(new Dockerfile.QuotedString(randomId(), Space.EMPTY, Markers.EMPTY, value, Dockerfile.QuotedString.QuoteStyle.SINGLE));
                    skip(token);
                } else {
                    // UNQUOTED_TEXT
                    contents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, text));
                    skip(token);
                }
            }
        }

        return new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    @Override
    public Dockerfile visitCmdInstruction(DockerfileParser.CmdInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String cmdKeyword = ctx.CMD().getText();
        skip(ctx.CMD().getSymbol());

        Dockerfile.CommandLine commandLine = visitCommandLineForCmd(ctx);

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                if (ctx.execForm() != null) {
                    stopToken = ctx.execForm().getStop();
                } else if (ctx.shellForm() != null) {
                    stopToken = ctx.shellForm().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Cmd(randomId(), prefix, Markers.EMPTY, cmdKeyword, commandLine);
    }

    @Override
    public Dockerfile visitEntrypointInstruction(DockerfileParser.EntrypointInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String entrypointKeyword = ctx.ENTRYPOINT().getText();
        skip(ctx.ENTRYPOINT().getSymbol());

        Dockerfile.CommandLine commandLine = visitCommandLineForEntrypoint(ctx);

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                if (ctx.execForm() != null) {
                    stopToken = ctx.execForm().getStop();
                } else if (ctx.shellForm() != null) {
                    stopToken = ctx.shellForm().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Entrypoint(randomId(), prefix, Markers.EMPTY, entrypointKeyword, commandLine);
    }

    @Override
    public Dockerfile visitExposeInstruction(DockerfileParser.ExposeInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the EXPOSE keyword
        String exposeKeyword = ctx.EXPOSE().getText();
        skip(ctx.EXPOSE().getSymbol());

        // Parse port list
        List<Dockerfile.Argument> ports = new ArrayList<>();
        for (DockerfileParser.PortContext portCtx : ctx.portList().port()) {
            ports.add(convertPort(portCtx));
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.portList().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Expose(randomId(), prefix, Markers.EMPTY, exposeKeyword, ports);
    }

    private Dockerfile.Argument convertPort(DockerfileParser.PortContext ctx) {
        Space prefix = prefix(ctx.getStart());
        Token token = ctx.UNQUOTED_TEXT().getSymbol();
        String text = token.getText();
        skip(token);

        List<Dockerfile.ArgumentContent> contents = new ArrayList<>();
        contents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, text));
        return new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    @Override
    public Dockerfile visitVolumeInstruction(DockerfileParser.VolumeInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the VOLUME keyword
        String volumeKeyword = ctx.VOLUME().getText();
        skip(ctx.VOLUME().getSymbol());

        boolean jsonForm = ctx.jsonArray() != null;
        List<Dockerfile.Argument> values = new ArrayList<>();

        if (jsonForm) {
            // Parse JSON array
            values = visitJsonArrayForVolume(ctx.jsonArray());
        } else {
            // Parse path list
            for (DockerfileParser.PathContext pathCtx : ctx.pathList().path()) {
                values.add(visitArgument(pathCtx));
            }
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                if (ctx.jsonArray() != null) {
                    stopToken = ctx.jsonArray().getStop();
                } else {
                    stopToken = ctx.pathList().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Volume(randomId(), prefix, Markers.EMPTY, volumeKeyword, jsonForm, values);
    }

    private List<Dockerfile.Argument> visitJsonArrayForVolume(DockerfileParser.JsonArrayContext ctx) {
        // Capture whitespace before opening bracket and store it as prefix of first argument
        Space bracketPrefix = prefix(ctx.LBRACKET().getSymbol());
        skip(ctx.LBRACKET().getSymbol());

        List<Dockerfile.Argument> arguments = new ArrayList<>();
        if (ctx.jsonArrayElements() != null) {
            DockerfileParser.JsonArrayElementsContext elementsCtx = ctx.jsonArrayElements();
            List<DockerfileParser.JsonStringContext> jsonStrings = elementsCtx.jsonString();

            for (int i = 0; i < jsonStrings.size(); i++) {
                Dockerfile.Argument arg = convertJsonString(jsonStrings.get(i));
                // Add bracket prefix to first argument
                if (i == 0 && !bracketPrefix.getWhitespace().isEmpty()) {
                    arg = arg.withPrefix(bracketPrefix);
                }
                arguments.add(arg);

                // Skip comma after this element if it's not the last one
                // The grammar is: jsonString ( JSON_WS? JSON_COMMA JSON_WS? jsonString )*
                // So we need to skip the JSON_COMMA tokens
                if (i < jsonStrings.size() - 1) {
                    // Find and skip the JSON_COMMA token between this element and the next
                    for (int j = 0; j < elementsCtx.getChildCount(); j++) {
                        if (elementsCtx.getChild(j) instanceof TerminalNode) {
                            TerminalNode terminal = (TerminalNode) elementsCtx.getChild(j);
                            if (terminal.getSymbol().getType() == DockerfileLexer.JSON_COMMA &&
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

        // Skip the closing bracket
        skip(ctx.JSON_RBRACKET().getSymbol());

        return arguments;
    }

    private Dockerfile.Argument convertJsonString(DockerfileParser.JsonStringContext ctx) {
        Space prefix = prefix(ctx.getStart());
        Token token = ctx.JSON_STRING().getSymbol();
        String text = token.getText();

        // Remove quotes
        String value = text.substring(1, text.length() - 1);
        skip(token);

        // Also need to skip any COMMA token that follows (if this is not the last element)
        // The COMMA is part of jsonArrayElements, so we need to handle it there
        // Actually, we'll handle commas in the calling method

        List<Dockerfile.ArgumentContent> contents = new ArrayList<>();
        contents.add(new Dockerfile.QuotedString(randomId(), Space.EMPTY, Markers.EMPTY, value, Dockerfile.QuotedString.QuoteStyle.DOUBLE));
        return new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, contents);
    }

    @Override
    public Dockerfile visitShellInstruction(DockerfileParser.ShellInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Extract and skip the SHELL keyword
        String shellKeyword = ctx.SHELL().getText();
        skip(ctx.SHELL().getSymbol());

        // Parse JSON array
        List<Dockerfile.Argument> arguments = visitJsonArrayForShell(ctx.jsonArray());

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.jsonArray().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Shell(randomId(), prefix, Markers.EMPTY, shellKeyword, arguments);
    }

    private List<Dockerfile.Argument> visitJsonArrayForShell(DockerfileParser.JsonArrayContext ctx) {
        // Capture whitespace before opening bracket and store it as prefix of first argument
        Space bracketPrefix = prefix(ctx.LBRACKET().getSymbol());
        skip(ctx.LBRACKET().getSymbol());

        List<Dockerfile.Argument> arguments = new ArrayList<>();
        if (ctx.jsonArrayElements() != null) {
            DockerfileParser.JsonArrayElementsContext elementsCtx = ctx.jsonArrayElements();
            List<DockerfileParser.JsonStringContext> jsonStrings = elementsCtx.jsonString();

            for (int i = 0; i < jsonStrings.size(); i++) {
                Dockerfile.Argument arg = convertJsonString(jsonStrings.get(i));
                // Add bracket prefix to first argument
                if (i == 0 && !bracketPrefix.getWhitespace().isEmpty()) {
                    arg = arg.withPrefix(bracketPrefix);
                }
                arguments.add(arg);

                // Skip comma after this element if it's not the last one
                if (i < jsonStrings.size() - 1) {
                    // Find and skip the JSON_COMMA token between this element and the next
                    for (int j = 0; j < elementsCtx.getChildCount(); j++) {
                        if (elementsCtx.getChild(j) instanceof TerminalNode) {
                            TerminalNode terminal = (TerminalNode) elementsCtx.getChild(j);
                            if (terminal.getSymbol().getType() == DockerfileLexer.JSON_COMMA &&
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

        // Skip the closing bracket
        skip(ctx.JSON_RBRACKET().getSymbol());

        return arguments;
    }

    @Override
    public Dockerfile visitWorkdirInstruction(DockerfileParser.WorkdirInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String workdirKeyword = ctx.WORKDIR().getText();
        skip(ctx.WORKDIR().getSymbol());

        Dockerfile.Argument path = visitArgument(ctx.path());

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.path().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Workdir(randomId(), prefix, Markers.EMPTY, workdirKeyword, path);
    }

    @Override
    public Dockerfile visitUserInstruction(DockerfileParser.UserInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String userKeyword = ctx.USER().getText();
        skip(ctx.USER().getSymbol());

        // Parse userSpec and split into user and optional group
        Dockerfile.Argument[] userAndGroup = parseUserSpec(ctx.userSpec());
        Dockerfile.Argument user = userAndGroup[0];
        Dockerfile.Argument group = userAndGroup[1];

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.userSpec().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.User(randomId(), prefix, Markers.EMPTY, userKeyword, user, group);
    }

    private Dockerfile.Argument[] parseUserSpec(DockerfileParser.UserSpecContext ctx) {
        Space prefix = prefix(ctx);

        // Parse the text
        List<Dockerfile.ArgumentContent> contents = parseText(ctx.text());

        // Advance cursor only to the last non-comment token to avoid consuming trailing comments
        Token lastToken = findLastNonCommentToken(ctx.text());
        if (lastToken != null) {
            advanceCursor(lastToken.getStopIndex() + 1);
        }

        // Find the colon separator to split user and group
        List<Dockerfile.ArgumentContent> userContents = new ArrayList<>();
        List<Dockerfile.ArgumentContent> groupContents = new ArrayList<>();
        boolean foundColon = false;

        for (Dockerfile.ArgumentContent content : contents) {
            if (content instanceof Dockerfile.PlainText) {
                String text = ((Dockerfile.PlainText) content).getText();
                int colonIndex = text.indexOf(':');

                if (colonIndex >= 0 && !foundColon) {
                    // Split at the colon
                    foundColon = true;
                    String userPart = text.substring(0, colonIndex);
                    String groupPart = text.substring(colonIndex + 1);

                    if (!userPart.isEmpty()) {
                        userContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, userPart));
                    }
                    if (!groupPart.isEmpty()) {
                        groupContents.add(new Dockerfile.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, groupPart));
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

        Dockerfile.Argument user = new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, userContents);
        Dockerfile.Argument group = groupContents.isEmpty() ? null :
                new Dockerfile.Argument(randomId(), Space.EMPTY, Markers.EMPTY, groupContents);

        return new Dockerfile.Argument[]{user, group};
    }

    @Override
    public Dockerfile visitStopsignalInstruction(DockerfileParser.StopsignalInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String stopsignalKeyword = ctx.STOPSIGNAL().getText();
        skip(ctx.STOPSIGNAL().getSymbol());

        Dockerfile.Argument signal = visitArgument(ctx.signal());

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.signal().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Stopsignal(randomId(), prefix, Markers.EMPTY, stopsignalKeyword, signal);
    }

    @Override
    public Dockerfile visitOnbuildInstruction(DockerfileParser.OnbuildInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String onbuildKeyword = ctx.ONBUILD().getText();
        skip(ctx.ONBUILD().getSymbol());

        // Visit the wrapped instruction
        Dockerfile.Instruction instruction = (Dockerfile.Instruction) visit(ctx.instruction());

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.instruction().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Onbuild(randomId(), prefix, Markers.EMPTY, onbuildKeyword, instruction);
    }

    @Override
    public Dockerfile visitHealthcheckInstruction(DockerfileParser.HealthcheckInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String healthcheckKeyword = ctx.HEALTHCHECK().getText();
        skip(ctx.HEALTHCHECK().getSymbol());

        // Check if UNQUOTED_TEXT is "NONE" (case-insensitive)
        boolean isNone = ctx.UNQUOTED_TEXT() != null &&
                "NONE".equalsIgnoreCase(ctx.UNQUOTED_TEXT().getText());
        List<Dockerfile.Flag> flags = null;
        Dockerfile.Cmd cmd = null;

        if (isNone) {
            // Capture the space before NONE and create a dummy CMD to hold it
            Space nonePrefix = prefix(ctx.UNQUOTED_TEXT().getSymbol());
            skip(ctx.UNQUOTED_TEXT().getSymbol());
            // Create a dummy CMD with the prefix to preserve whitespace
            cmd = new Dockerfile.Cmd(
                    randomId(),
                    nonePrefix,
                    Markers.EMPTY,
                    "",
                    new Dockerfile.CommandLine(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            new Dockerfile.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, emptyList())
                    )
            );
        } else {
            // Parse flags if present
            if (ctx.flags() != null) {
                flags = convertFlags(ctx.flags());
            }

            // Parse CMD instruction
            if (ctx.cmdInstruction() != null) {
                cmd = (Dockerfile.Cmd) visit(ctx.cmdInstruction());
            }
        }

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                if (isNone && ctx.UNQUOTED_TEXT() != null) {
                    stopToken = ctx.UNQUOTED_TEXT().getSymbol();
                } else if (ctx.cmdInstruction() != null) {
                    stopToken = ctx.cmdInstruction().getStop();
                }
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Healthcheck(randomId(), prefix, Markers.EMPTY, healthcheckKeyword, isNone, flags, cmd);
    }

    @Override
    public Dockerfile visitMaintainerInstruction(DockerfileParser.MaintainerInstructionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        String maintainerKeyword = ctx.MAINTAINER().getText();
        skip(ctx.MAINTAINER().getSymbol());

        Dockerfile.Argument text = visitArgument(ctx.text());

        // Advance cursor to end of instruction, but NOT past trailing comment
        if (ctx.getStop() != null) {
            Token stopToken = ctx.getStop();
            if (ctx.trailingComment() != null && stopToken == ctx.trailingComment().getStop()) {
                stopToken = ctx.text().getStop();
            }
            advanceCursor(stopToken.getStopIndex() + 1);
        }

        return new Dockerfile.Maintainer(randomId(), prefix, Markers.EMPTY, maintainerKeyword, text);
    }

    private Dockerfile.CommandLine visitCommandLine(DockerfileParser.RunInstructionContext ctx) {
        Dockerfile.CommandForm form;
        if (ctx.execForm() != null) {
            form = visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            form = visitShellFormContext(ctx.shellForm());
        } else if (ctx.heredoc() != null) {
            form = visitHeredocContext(ctx.heredoc());
        } else {
            // Fallback to empty shell form
            form = new Dockerfile.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        return new Dockerfile.CommandLine(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                form
        );
    }

    private Dockerfile.CommandLine visitCommandLineForCmd(DockerfileParser.CmdInstructionContext ctx) {
        Dockerfile.CommandForm form;
        if (ctx.execForm() != null) {
            form = visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            form = visitShellFormContext(ctx.shellForm());
        } else {
            form = new Dockerfile.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        return new Dockerfile.CommandLine(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                form
        );
    }

    private Dockerfile.CommandLine visitCommandLineForEntrypoint(DockerfileParser.EntrypointInstructionContext ctx) {
        Dockerfile.CommandForm form;
        if (ctx.execForm() != null) {
            form = visitExecFormContext(ctx.execForm());
        } else if (ctx.shellForm() != null) {
            form = visitShellFormContext(ctx.shellForm());
        } else {
            form = new Dockerfile.ShellForm(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        return new Dockerfile.CommandLine(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                form
        );
    }

    private List<Dockerfile.Flag> convertFlags(DockerfileParser.FlagsContext ctx) {
        List<Dockerfile.Flag> flags = new ArrayList<>();
        for (DockerfileParser.FlagContext flagCtx : ctx.flag()) {
            Space flagPrefix = prefix(flagCtx.getStart());
            skip(flagCtx.DASH_DASH().getSymbol());

            String flagName = flagCtx.flagName().getText();
            advanceCursor(flagCtx.flagName().getStop().getStopIndex() + 1);

            Dockerfile.Argument flagValue = null;
            if (flagCtx.EQUALS() != null) {
                skip(flagCtx.EQUALS().getSymbol());
                flagValue = visitArgument(flagCtx.flagValue());
            }

            flags.add(new Dockerfile.Flag(randomId(), flagPrefix, Markers.EMPTY, flagName, flagValue));
        }
        return flags;
    }

    private Dockerfile.ShellForm visitShellFormContext(DockerfileParser.ShellFormContext ctx) {
        return convert(ctx, (c, prefix) ->
                new Dockerfile.ShellForm(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        singletonList(visitArgument(c.text()))
                )
        );
    }

    private Dockerfile.ExecForm visitExecFormContext(DockerfileParser.ExecFormContext ctx) {
        return convert(ctx, (c, prefix) -> {
            List<Dockerfile.Argument> args = new ArrayList<>();
            DockerfileParser.JsonArrayContext jsonArray = c.jsonArray();

            skip(jsonArray.LBRACKET().getSymbol());

            if (jsonArray.jsonArrayElements() != null) {
                for (DockerfileParser.JsonStringContext jsonStr : jsonArray.jsonArrayElements().jsonString()) {
                    Space argPrefix = prefix(jsonStr.getStart());
                    String value = jsonStr.JSON_STRING().getText();
                    // Remove surrounding quotes
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    advanceCursor(jsonStr.JSON_STRING().getSymbol().getStopIndex() + 1);

                    Dockerfile.QuotedString qs = new Dockerfile.QuotedString(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            value,
                            Dockerfile.QuotedString.QuoteStyle.DOUBLE
                    );
                    args.add(new Dockerfile.Argument(
                            randomId(),
                            argPrefix,
                            Markers.EMPTY,
                            singletonList(qs)
                    ));
                }
            }

            skip(jsonArray.JSON_RBRACKET().getSymbol());

            return new Dockerfile.ExecForm(randomId(), prefix, Markers.EMPTY, args);
        });
    }

    private Dockerfile.HeredocForm visitHeredocContext(DockerfileParser.HeredocContext ctx) {
        return convert(ctx, (c, prefix) -> {
            // Get opening marker (<<EOF or <<-EOF)
            String opening = c.HEREDOC_START().getText();
            skip(c.HEREDOC_START().getSymbol());

            // Check for optional destination (for COPY/ADD with inline destination)
            Dockerfile.Argument destination = null;
            if (c.path() != null) {
                destination = visitArgument(c.path());
            }

            // Collect content lines (each heredocLine is text? + NEWLINE)
            List<String> contentLines = new ArrayList<>();

            // Add the opening newline first
            if (c.NEWLINE() != null) {
                String openingNewline = c.NEWLINE().getText();
                contentLines.add(openingNewline);
                skip(c.NEWLINE().getSymbol());
            }
            for (DockerfileParser.HeredocLineContext lineCtx : c.heredocLine()) {
                StringBuilder line = new StringBuilder();

                // Add text content if present
                if (lineCtx.text() != null) {
                    String lineText = lineCtx.text().getText();
                    line.append(lineText);
                    // Skip all tokens in the text
                    for (int i = 0; i < lineCtx.text().getChildCount(); i++) {
                        if (lineCtx.text().getChild(i) instanceof TerminalNode) {
                            skip(((TerminalNode) lineCtx.text().getChild(i)).getSymbol());
                        }
                    }
                }

                // Add newline
                if (lineCtx.NEWLINE() != null) {
                    line.append(lineCtx.NEWLINE().getText());
                    skip(lineCtx.NEWLINE().getSymbol());
                }

                contentLines.add(line.toString());
            }

            // Get closing marker (UNQUOTED_TEXT)
            String closing = c.heredocEnd().UNQUOTED_TEXT().getText();
            skip(c.heredocEnd().UNQUOTED_TEXT().getSymbol());

            return new Dockerfile.HeredocForm(randomId(), prefix, Markers.EMPTY, opening, destination, contentLines, closing);
        });
    }

    private Dockerfile.Argument visitArgument(@Nullable ParserRuleContext ctx) {
        if (ctx == null) {
            return new Dockerfile.Argument(randomId(), Space.EMPTY, Markers.EMPTY, emptyList());
        }

        return convert(ctx, (c, prefix) -> {
            // Simple implementation - just convert the entire context to plain text
            // This is used for stage names and other simple arguments
            String fullText = c.getText();
            Dockerfile.PlainText plainText = new Dockerfile.PlainText(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    fullText
            );
            return new Dockerfile.Argument(randomId(), prefix, Markers.EMPTY, singletonList(plainText));
        });
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

    private Token findLastNonCommentToken(DockerfileParser.TextContext textCtx) {
        if (textCtx == null) {
            return null;
        }

        Token lastNonCommentToken = null;
        for (int i = 0; i < textCtx.getChildCount(); i++) {
            ParseTree child = textCtx.getChild(i);
            if (child instanceof DockerfileParser.TextElementContext) {
                DockerfileParser.TextElementContext textElement = (DockerfileParser.TextElementContext) child;
                if (textElement.getChildCount() > 0 && textElement.getChild(0) instanceof TerminalNode) {
                    TerminalNode terminal = (TerminalNode) textElement.getChild(0);
                    Token token = terminal.getSymbol();
                    // Skip COMMENT and WS tokens - they should be part of the next element's prefix
                    if (token.getType() != DockerfileLexer.COMMENT && token.getType() != DockerfileLexer.WS) {
                        lastNonCommentToken = token;
                    }
                }
            }
        }

        return lastNonCommentToken != null ? lastNonCommentToken : textCtx.getStop();
    }

    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, Space, T> conversion) {
        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
        }
        return t;
    }
}
