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
package org.openrewrite.bash.internal;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.bash.internal.grammar.BashLexer;
import org.openrewrite.bash.internal.grammar.BashParser;
import org.openrewrite.bash.tree.Bash;
import org.openrewrite.bash.tree.Space;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openrewrite.Tree.randomId;

public class BashParserVisitor {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    // ANTLR's CodePointCharStream uses code point indices, but Java String uses
    // UTF-16 char unit indices. When the source contains supplementary characters
    // (emojis, etc.), these differ. This array maps code point index → char index.
    private final int @Nullable [] cpToChar;

    private int cursor = 0;

    public BashParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();

        int cpCount = this.source.codePointCount(0, this.source.length());
        if (cpCount != this.source.length()) {
            cpToChar = new int[cpCount + 1];
            int charPos = 0;
            for (int i = 0; i < cpCount; i++) {
                cpToChar[i] = charPos;
                charPos += Character.charCount(this.source.codePointAt(charPos));
            }
            cpToChar[cpCount] = this.source.length();
        } else {
            cpToChar = null;
        }
    }

    private int toCharIndex(int codePointIndex) {
        if (codePointIndex < 0) {
            return codePointIndex;
        }
        return cpToChar != null ? cpToChar[Math.min(codePointIndex, cpToChar.length - 1)] : codePointIndex;
    }

    public Bash.Script parse() {
        BashLexer lexer = new BashLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        BashParser parser = new BashParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();

        BashParser.ProgramContext program = parser.program();

        Space prefix = Space.EMPTY;

        Bash.@Nullable Shebang shebang = null;
        if (program.shebang() != null) {
            shebang = visitShebang(program.shebang());
        }

        List<Bash.Statement> statements = new ArrayList<>();
        if (program.completeCommands() != null) {
            statements = visitCompleteCommands(program.completeCommands());
        }

        Space eof = Space.format(source, cursor, source.length());

        return new Bash.Script(
                randomId(),
                path,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                shebang,
                statements,
                eof
        );
    }

    private Bash.Shebang visitShebang(BashParser.ShebangContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String text = ctx.SHEBANG().getText();
        skip(ctx.SHEBANG().getSymbol());
        // Don't skip linebreak — it will be captured as prefix of the next element
        return new Bash.Shebang(randomId(), prefix, Markers.EMPTY, text);
    }

    // ================================================================
    // Command structure: completeCommands → completeCommand → list → andOr → pipeline → command
    // ================================================================

    private List<Bash.Statement> visitCompleteCommands(BashParser.CompleteCommandsContext ctx) {
        List<Bash.Statement> statements = new ArrayList<>();
        for (BashParser.CompleteCommandContext cc : ctx.completeCommand()) {
            statements.addAll(visitCompleteCommand(cc));
        }
        return statements;
    }

    private List<Bash.Statement> visitCompleteCommand(BashParser.CompleteCommandContext ctx) {
        List<Bash.Statement> stmts = visitList(ctx.list());
        if (ctx.separator() != null) {
            stmts = attachSeparator(stmts, ctx.separator());
        }
        return stmts;
    }

    private List<Bash.Statement> visitList(BashParser.ListContext ctx) {
        List<BashParser.AndOrContext> andOrs = ctx.andOr();
        List<BashParser.ListSepContext> seps = ctx.listSep();

        if (andOrs.size() == 1 && seps.isEmpty()) {
            return Collections.singletonList(visitAndOr(andOrs.get(0)));
        }

        // Multiple items separated by ; or &
        List<Bash.Statement> results = new ArrayList<>();
        Bash.Statement current = visitAndOr(andOrs.get(0));

        for (int i = 0; i < seps.size(); i++) {
            BashParser.ListSepContext sep = seps.get(i);
            if (sep.AMP() != null) {
                // & — wrap in Background
                Space ampPrefix = prefix(sep.AMP().getSymbol());
                skip(sep.AMP().getSymbol());
                current = new Bash.Background(randomId(), current.getPrefix(), Markers.EMPTY,
                        current.withPrefix(Space.EMPTY), ampPrefix);
            }
            // For SEMI, don't skip — ; is absorbed into the prefix of the next element

            results.add(current);

            if (i + 1 < andOrs.size()) {
                current = visitAndOr(andOrs.get(i + 1));
            }
        }

        // Add the last statement if it wasn't added yet
        if (results.size() < andOrs.size()) {
            results.add(current);
        }

        return results;
    }

    private Bash.Statement visitAndOr(BashParser.AndOrContext ctx) {
        List<BashParser.PipelineContext> pipelines = ctx.pipeline();
        List<BashParser.AndOrOpContext> ops = ctx.andOrOp();

        if (pipelines.size() == 1 && ops.isEmpty()) {
            return visitPipeline(pipelines.get(0));
        }

        // Multiple pipelines connected by && or ||
        Space prefix = prefix(pipelines.get(0).getStart());
        List<Bash.Statement> commands = new ArrayList<>();
        List<Bash.CommandList.OperatorEntry> operators = new ArrayList<>();

        Bash.Statement first = visitPipeline(pipelines.get(0));
        commands.add(first.withPrefix(Space.EMPTY));

        for (int i = 0; i < ops.size(); i++) {
            BashParser.AndOrOpContext op = ops.get(i);
            Space opPrefix = prefix(op.getStart());
            Bash.CommandList.Operator opType = op.AND() != null ?
                    Bash.CommandList.Operator.AND : Bash.CommandList.Operator.OR;
            skip(op.getStart());
            operators.add(new Bash.CommandList.OperatorEntry(opPrefix, opType));

            if (i + 1 < pipelines.size()) {
                commands.add(visitPipeline(pipelines.get(i + 1)));
            }
        }

        return new Bash.CommandList(randomId(), prefix, Markers.EMPTY, commands, operators);
    }

    private Bash.Statement visitPipeline(BashParser.PipelineContext ctx) {
        if (ctx.pipeSequence() == null) {
            return fallbackCommand(ctx);
        }
        boolean negated = ctx.bangOpt() != null;
        if (negated) {
            // Capture prefix before ! and skip the ! token itself
            Space negationPrefix = prefix(ctx.bangOpt().getStart());
            skip(ctx.bangOpt().getStart());
            return visitPipeSequence(ctx.pipeSequence(), true, negationPrefix);
        }
        return visitPipeSequence(ctx.pipeSequence(), false, null);
    }

    private Bash.Statement visitPipeSequence(BashParser.PipeSequenceContext ctx, boolean negated, @Nullable Space negationPrefix) {
        List<BashParser.CommandContext> commands = ctx.command();
        List<BashParser.PipeOpContext> pipes = ctx.pipeOp();

        if (commands.size() == 1 && !negated) {
            return visitCommand(commands.get(0));
        }

        Space prefix;
        if (negated) {
            prefix = negationPrefix != null ? negationPrefix : Space.EMPTY;
        } else {
            prefix = prefix(ctx.getStart());
        }
        List<Bash.Statement> cmdList = new ArrayList<>();
        List<Bash.Pipeline.PipeEntry> pipeOps = new ArrayList<>();

        Bash.Statement first = visitCommand(commands.get(0));
        if (negated) {
            cmdList.add(first);
        } else {
            cmdList.add(first.withPrefix(Space.EMPTY));
        }

        for (int i = 0; i < pipes.size(); i++) {
            BashParser.PipeOpContext pipe = pipes.get(i);
            Space opPrefix = prefix(pipe.getStart());
            Bash.Pipeline.PipeOp pipeOp = pipe.PIPE_AND() != null ?
                    Bash.Pipeline.PipeOp.PIPE_AND : Bash.Pipeline.PipeOp.PIPE;
            skip(pipe.getStart());
            pipeOps.add(new Bash.Pipeline.PipeEntry(opPrefix, pipeOp));

            if (i + 1 < commands.size()) {
                cmdList.add(visitCommand(commands.get(i + 1)));
            }
        }

        return new Bash.Pipeline(randomId(), prefix, Markers.EMPTY, negated, cmdList, pipeOps);
    }

    private Bash.Statement visitCommand(BashParser.CommandContext ctx) {
        if (ctx.compoundCommand() != null) {
            Bash.Statement stmt = visitCompoundCommand(ctx.compoundCommand());
            if (ctx.redirectionList() != null) {
                // Attach redirections to the compound command
                stmt = attachRedirections(stmt, ctx.redirectionList());
            }
            return stmt;
        }
        if (ctx.functionDefinition() != null) {
            return visitFunctionDefinition(ctx.functionDefinition());
        }
        if (ctx.simpleCommand() == null) {
            // ANTLR error recovery produced no valid alternative
            Bash.Literal literal = fallbackLiteral(ctx);
            return new Bash.Command(randomId(), literal.getPrefix(), Markers.EMPTY,
                    Collections.emptyList(),
                    Collections.singletonList(literal.withPrefix(Space.EMPTY)));
        }
        return visitSimpleCommand(ctx.simpleCommand());
    }

    // ================================================================
    // Simple commands
    // ================================================================

    private Bash.Statement visitSimpleCommand(BashParser.SimpleCommandContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<Bash.Assignment> assignments = new ArrayList<>();
        List<Bash.Expression> arguments = new ArrayList<>();

        // cmdPrefix: assignments and redirections before the command word
        if (ctx.cmdPrefix() != null) {
            for (ParseTree child : ctx.cmdPrefix().children) {
                if (child instanceof BashParser.AssignmentContext) {
                    Bash.Assignment a = visitAssignment((BashParser.AssignmentContext) child);
                    assignments.add(a);
                } else if (child instanceof BashParser.RedirectionContext) {
                    arguments.add(visitRedirection((BashParser.RedirectionContext) child));
                }
            }
        }

        // cmdWord
        if (ctx.cmdWord() != null) {
            arguments.add(visitWord(ctx.cmdWord().word()));
        }

        // cmdSuffix: words, assignments, and redirections after the command word
        // Suffix assignments go into arguments (not assignments) to preserve source order
        if (ctx.cmdSuffix() != null) {
            for (ParseTree child : ctx.cmdSuffix().children) {
                if (child instanceof BashParser.AssignmentContext) {
                    Bash.Assignment a = visitAssignment((BashParser.AssignmentContext) child);
                    arguments.add(a);
                } else if (child instanceof BashParser.WordContext) {
                    arguments.add(visitWord((BashParser.WordContext) child));
                } else if (child instanceof BashParser.RedirectionContext) {
                    arguments.add(visitRedirection((BashParser.RedirectionContext) child));
                }
            }
        }

        // The first element (assignment or argument) has its prefix moved to the Command
        if (!assignments.isEmpty()) {
            assignments.set(0, assignments.get(0).withPrefix(Space.EMPTY));
        }
        if (!arguments.isEmpty() && assignments.isEmpty()) {
            // Only clear first argument prefix when there are no assignments,
            // otherwise the space between the last assignment and first argument is genuine
            arguments.set(0, arguments.get(0).withPrefix(Space.EMPTY));
        }

        return new Bash.Command(randomId(), prefix, Markers.EMPTY, assignments, arguments);
    }

    // ================================================================
    // Words
    // ================================================================

    private Bash.Expression visitWord(BashParser.WordContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<Bash.Expression> parts = new ArrayList<>();
        for (BashParser.WordPartContext wp : ctx.wordPart()) {
            parts.add(visitWordPart(wp));
        }
        // Unwrap single-part words to avoid unnecessary nesting
        if (parts.size() == 1) {
            Bash.Expression only = parts.get(0);
            return only.withPrefix(prefix);
        }
        // Strip prefix from first part (it's on the Word)
        if (!parts.isEmpty()) {
            parts.set(0, parts.get(0).withPrefix(Space.EMPTY));
        }
        return new Bash.Word(randomId(), prefix, Markers.EMPTY, parts);
    }

    private Bash.Expression visitWordPart(BashParser.WordPartContext ctx) {
        // Handle each word part type
        if (ctx.WORD() != null) {
            return visitTerminal(ctx.WORD());
        }
        if (ctx.NUMBER() != null) {
            return visitTerminal(ctx.NUMBER());
        }
        if (ctx.SINGLE_QUOTED_STRING() != null) {
            Space prefix = prefix(ctx.SINGLE_QUOTED_STRING().getSymbol());
            String text = ctx.SINGLE_QUOTED_STRING().getText();
            skip(ctx.SINGLE_QUOTED_STRING().getSymbol());
            // Remove surrounding quotes
            String inner = text.substring(1, text.length() - 1);
            return new Bash.SingleQuoted(randomId(), prefix, Markers.EMPTY, inner);
        }
        if (ctx.DOLLAR_SINGLE_QUOTED() != null) {
            Space prefix = prefix(ctx.DOLLAR_SINGLE_QUOTED().getSymbol());
            String text = ctx.DOLLAR_SINGLE_QUOTED().getText();
            skip(ctx.DOLLAR_SINGLE_QUOTED().getSymbol());
            return new Bash.DollarSingleQuoted(randomId(), prefix, Markers.EMPTY, text);
        }
        if (ctx.doubleQuotedString() != null) {
            return visitDoubleQuotedString(ctx.doubleQuotedString());
        }
        if (ctx.DOLLAR_NAME() != null) {
            return visitVariableRef(ctx.DOLLAR_NAME());
        }
        if (ctx.SPECIAL_VAR() != null) {
            return visitVariableRef(ctx.SPECIAL_VAR());
        }
        if (ctx.DOLLAR_LBRACE() != null) {
            return visitBraceExpansion(ctx);
        }
        if (ctx.commandSubstitution() != null) {
            return visitCommandSubstitution(ctx.commandSubstitution());
        }
        if (ctx.BACKTICK() != null && ctx.BACKTICK().size() >= 2) {
            return visitBacktickSubstitution(ctx);
        }
        if (ctx.arithmeticSubstitution() != null) {
            return visitArithmeticSubstitution(ctx.arithmeticSubstitution());
        }
        if (ctx.processSubstitution() != null) {
            return visitProcessSubstitution(ctx.processSubstitution());
        }
        if (ctx.LBRACE() != null && ctx.RBRACE() != null) {
            // Brace expansion: {wordPart*}
            return visitBraceWord(ctx);
        }
        // Keywords used as words, glob chars, operators, etc.
        return visitTerminal(getFirstTerminal(ctx));
    }

    private Bash.Expression visitBraceWord(BashParser.WordPartContext ctx) {
        // {wordPart*} - capture as literal text from source
        Space prefix = prefix(ctx.getStart());
        String text = sourceText(ctx);
        skipContext(ctx);
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    // ================================================================
    // Double-quoted strings
    // ================================================================

    private Bash.Expression visitDoubleQuotedString(BashParser.DoubleQuotedStringContext ctx) {
        if (ctx.DOUBLE_QUOTE(1) == null) {
            return fallbackLiteral(ctx);
        }
        Space prefix = prefix(ctx.getStart());
        skip(ctx.DOUBLE_QUOTE(0).getSymbol()); // opening "

        List<Bash.Expression> parts = new ArrayList<>();
        for (BashParser.DoubleQuotedPartContext dqp : ctx.doubleQuotedPart()) {
            parts.add(visitDoubleQuotedPart(dqp));
        }

        skip(ctx.DOUBLE_QUOTE(1).getSymbol()); // closing "
        return new Bash.DoubleQuoted(randomId(), prefix, Markers.EMPTY, parts);
    }

    private Bash.Expression visitDoubleQuotedPart(BashParser.DoubleQuotedPartContext ctx) {
        if (ctx.DQ_TEXT() != null) {
            return visitTerminal(ctx.DQ_TEXT());
        }
        if (ctx.DQ_ESCAPE() != null) {
            return visitTerminal(ctx.DQ_ESCAPE());
        }
        if (ctx.DOLLAR_NAME() != null) {
            return visitVariableRef(ctx.DOLLAR_NAME());
        }
        if (ctx.SPECIAL_VAR() != null) {
            return visitVariableRef(ctx.SPECIAL_VAR());
        }
        if (ctx.DOLLAR_LBRACE() != null) {
            return visitBraceExpansionInDQ(ctx);
        }
        if (ctx.commandSubstitution() != null) {
            return visitCommandSubstitution(ctx.commandSubstitution());
        }
        if (ctx.arithmeticSubstitution() != null) {
            return visitArithmeticSubstitution(ctx.arithmeticSubstitution());
        }
        if (ctx.BACKTICK() != null && ctx.BACKTICK().size() >= 2) {
            return visitBacktickInDQ(ctx);
        }
        // Fallback
        return visitTerminal(getFirstTerminal(ctx));
    }

    // ================================================================
    // Variable references and expansions
    // ================================================================

    private Bash.VariableExpansion visitVariableRef(TerminalNode node) {
        Space prefix = prefix(node.getSymbol());
        String text = node.getText();
        skip(node.getSymbol());
        return new Bash.VariableExpansion(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.VariableExpansion visitBraceExpansion(BashParser.WordPartContext ctx) {
        // ${...} - capture all text from ${ to }
        Space prefix = prefix(ctx.DOLLAR_LBRACE().getSymbol());
        int start = toCharIndex(ctx.DOLLAR_LBRACE().getSymbol().getStartIndex());
        int stop = toCharIndex(ctx.RBRACE().getSymbol().getStopIndex() + 1);
        String text = source.substring(start, stop);
        advanceCursor(stop);
        return new Bash.VariableExpansion(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.Expression visitBraceExpansionInDQ(BashParser.DoubleQuotedPartContext ctx) {
        if (ctx.RBRACE() == null) {
            return fallbackLiteral(ctx);
        }
        // ${...} inside double quotes
        Space prefix = prefix(ctx.DOLLAR_LBRACE().getSymbol());
        int start = toCharIndex(ctx.DOLLAR_LBRACE().getSymbol().getStartIndex());
        int stop = toCharIndex(ctx.RBRACE().getSymbol().getStopIndex() + 1);
        String text = source.substring(start, stop);
        advanceCursor(stop);
        return new Bash.VariableExpansion(randomId(), prefix, Markers.EMPTY, text);
    }

    // ================================================================
    // Command substitution: $(...) and `...`
    // ================================================================

    private Bash.Expression visitCommandSubstitution(BashParser.CommandSubstitutionContext ctx) {
        if (ctx.RPAREN() == null) {
            return fallbackLiteral(ctx);
        }
        Space prefix = prefix(ctx.DOLLAR_LPAREN().getSymbol());
        skip(ctx.DOLLAR_LPAREN().getSymbol());

        List<Bash.Statement> body = new ArrayList<>();
        if (ctx.commandSubstitutionContent().completeCommands() != null) {
            body = visitCompleteCommands(ctx.commandSubstitutionContent().completeCommands());
        }

        Space closingDelimiter = prefix(ctx.RPAREN().getSymbol());
        skip(ctx.RPAREN().getSymbol());

        return new Bash.CommandSubstitution(randomId(), prefix, Markers.EMPTY, true, body, closingDelimiter);
    }

    private Bash.Expression visitBacktickSubstitution(BashParser.WordPartContext ctx) {
        // `...` - capture as opaque text
        Space prefix = prefix(ctx.BACKTICK(0).getSymbol());
        int start = toCharIndex(ctx.BACKTICK(0).getSymbol().getStartIndex());
        int stop = toCharIndex(ctx.BACKTICK(1).getSymbol().getStopIndex() + 1);
        String text = source.substring(start, stop);
        advanceCursor(stop);
        // Store as Literal since backtick content is opaque for now
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.Expression visitBacktickInDQ(BashParser.DoubleQuotedPartContext ctx) {
        // `...` inside double quotes
        Space prefix = prefix(ctx.BACKTICK(0).getSymbol());
        int start = toCharIndex(ctx.BACKTICK(0).getSymbol().getStartIndex());
        int stop = toCharIndex(ctx.BACKTICK(1).getSymbol().getStopIndex() + 1);
        String text = source.substring(start, stop);
        advanceCursor(stop);
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    // ================================================================
    // Arithmetic: $((...))
    // ================================================================

    private Bash.Expression visitArithmeticSubstitution(BashParser.ArithmeticSubstitutionContext ctx) {
        if (ctx.DOUBLE_RPAREN() == null) {
            return fallbackLiteral(ctx);
        }
        int closeStart = toCharIndex(ctx.DOUBLE_RPAREN().getSymbol().getStartIndex());
        if (closeStart < 0 || closeStart < cursor) {
            // DOUBLE_RPAREN is a synthetic/missing token from error recovery
            return fallbackLiteral(ctx);
        }
        Space prefix = prefix(ctx.DOLLAR_DPAREN().getSymbol());
        skip(ctx.DOLLAR_DPAREN().getSymbol());

        // Capture all text between $(( and )) including leading/trailing whitespace
        String expr = source.substring(cursor, closeStart);
        advanceCursor(closeStart);

        skip(ctx.DOUBLE_RPAREN().getSymbol());
        return new Bash.ArithmeticExpansion(randomId(), prefix, Markers.EMPTY, true, expr);
    }

    // ================================================================
    // Process substitution: <(...) or >(...)
    // ================================================================

    private Bash.Expression visitProcessSubstitution(BashParser.ProcessSubstitutionContext ctx) {
        if (ctx.RPAREN() == null) {
            return fallbackLiteral(ctx);
        }
        boolean isInput = ctx.PROC_SUBST_IN() != null;
        TerminalNode opener = isInput ? ctx.PROC_SUBST_IN() : ctx.PROC_SUBST_OUT();
        Space prefix = prefix(opener.getSymbol());
        skip(opener.getSymbol());

        List<Bash.Statement> body = new ArrayList<>();
        if (ctx.commandSubstitutionContent().completeCommands() != null) {
            body = visitCompleteCommands(ctx.commandSubstitutionContent().completeCommands());
        }

        // Check for synthetic RPAREN (ANTLR error recovery)
        if (ctx.RPAREN().getSymbol().getStartIndex() < 0) {
            return new Bash.ProcessSubstitution(randomId(), prefix, Markers.EMPTY, isInput, body, null);
        }
        Space closingParen = prefix(ctx.RPAREN().getSymbol());
        skip(ctx.RPAREN().getSymbol());
        return new Bash.ProcessSubstitution(randomId(), prefix, Markers.EMPTY, isInput, body, closingParen);
    }

    // ================================================================
    // Assignments
    // ================================================================

    private Bash.Assignment visitAssignment(BashParser.AssignmentContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Name
        Space namePrefix = Space.EMPTY;
        String nameText = ctx.WORD().getText();
        skip(ctx.WORD().getSymbol());
        Bash.Literal name = new Bash.Literal(randomId(), namePrefix, Markers.EMPTY, nameText);

        // Operator (= or +=)
        // Include any whitespace between WORD and operator in the operator text
        // (in valid bash, there's no space, but the parser allows it)
        Token opToken = ctx.PLUS_EQUALS() != null ?
                ctx.PLUS_EQUALS().getSymbol() : ctx.EQUALS().getSymbol();
        int opStart = toCharIndex(opToken.getStartIndex());
        String operator = source.substring(cursor, toCharIndex(opToken.getStopIndex() + 1));
        skip(opToken);

        // Value
        Bash.@Nullable Expression value = null;
        if (ctx.assignmentValue() != null) {
            value = visitAssignmentValue(ctx.assignmentValue());
        }

        return new Bash.Assignment(randomId(), prefix, Markers.EMPTY, name, operator, value);
    }

    private Bash.Expression visitAssignmentValue(BashParser.AssignmentValueContext ctx) {
        if (ctx.word() != null) {
            return visitWord(ctx.word());
        }
        if (ctx.RPAREN() == null) {
            return fallbackLiteral(ctx);
        }
        // Array: (elements)
        Space prefix = prefix(ctx.LPAREN().getSymbol());
        skip(ctx.LPAREN().getSymbol());

        List<Bash.Expression> elements = new ArrayList<>();
        if (ctx.arrayElements() != null) {
            for (BashParser.WordContext w : ctx.arrayElements().word()) {
                elements.add(visitWord(w));
                // Skip linebreak after each word
            }
        }

        Space closingParen = prefix(ctx.RPAREN().getSymbol());
        skip(ctx.RPAREN().getSymbol());

        return new Bash.ArrayLiteral(randomId(), prefix, Markers.EMPTY, elements, closingParen);
    }

    // ================================================================
    // Redirections
    // ================================================================

    private Bash.Expression visitRedirection(BashParser.RedirectionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        if (ctx.heredoc() != null) {
            return visitHeredoc(ctx, prefix);
        }

        if (ctx.HERESTRING() != null) {
            return visitHereString(ctx, prefix);
        }

        // Regular redirection: capture as opaque text
        int start = toCharIndex(ctx.getStart().getStartIndex());
        int stop = ctx.getStop() != null ? toCharIndex(ctx.getStop().getStopIndex() + 1) : source.length();
        if (stop < start) stop = start;
        String text = source.substring(start, stop);
        advanceCursor(stop);

        // We need to strip the prefix whitespace from the text since it's in the Space
        String trimmedText = text;
        int prefixLen = start - (cursor - (stop - start));
        // Actually, prefix() already advanced cursor to the token start.
        // The text from start to stop is the full redirection text.

        return new Bash.Redirect(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.HereDoc visitHeredoc(BashParser.RedirectionContext rctx, Space prefix) {
        BashParser.HeredocContext ctx = rctx.heredoc();

        // Capture opening including trailing tokens and newline
        // (<<EOF\n, <<-'EOF'\n, <<EOF >"file"\n, etc.)
        int openStart = toCharIndex(ctx.getStart().getStartIndex());

        // Include fd number if present
        if (rctx.NUMBER() != null) {
            openStart = toCharIndex(rctx.NUMBER().getSymbol().getStartIndex());
        }

        // Include the NEWLINE after the delimiter (and any trailing tokens) in the opening
        int openEnd;
        List<TerminalNode> newlines = ctx.NEWLINE();
        if (newlines != null && !newlines.isEmpty()) {
            openEnd = toCharIndex(newlines.get(0).getSymbol().getStopIndex() + 1);
        } else {
            openEnd = toCharIndex(ctx.heredocDelimiter().getStop().getStopIndex() + 1);
        }
        String opening = source.substring(openStart, openEnd);
        advanceCursor(openEnd);

        // Collect content lines — last line is the closing delimiter
        List<String> contentLines = new ArrayList<>();
        BashParser.HeredocBodyContext body = ctx.heredocBody();
        List<BashParser.HeredocLineContext> lines = body.heredocLine();

        for (int i = 0; i < lines.size() - 1; i++) {
            BashParser.HeredocLineContext line = lines.get(i);
            // Use cursor as line start — WS tokens are on the hidden channel
            // so line.getStart() skips leading whitespace
            int lineStop = line.getStop() != null ? toCharIndex(line.getStop().getStopIndex() + 1) : cursor;
            if (lineStop < cursor) lineStop = cursor;
            if (lineStop > source.length()) lineStop = source.length();
            contentLines.add(source.substring(cursor, lineStop));
            advanceCursor(lineStop);
        }

        // Last line is the closing delimiter
        BashParser.HeredocLineContext lastLine = lines.get(lines.size() - 1);
        int closeEnd = lastLine.getStop() != null ? toCharIndex(lastLine.getStop().getStopIndex() + 1) : cursor;
        if (closeEnd < cursor) closeEnd = cursor;
        if (closeEnd > source.length()) closeEnd = source.length();
        String closing = source.substring(cursor, closeEnd);
        advanceCursor(closeEnd);

        return new Bash.HereDoc(randomId(), prefix, Markers.EMPTY, opening, contentLines, closing);
    }

    private Bash.HereString visitHereString(BashParser.RedirectionContext ctx, Space prefix) {
        // Skip optional fd number
        if (ctx.NUMBER() != null) {
            skip(ctx.NUMBER().getSymbol());
        }
        skip(ctx.HERESTRING().getSymbol());
        Bash.Expression word = visitWord(ctx.word());
        return new Bash.HereString(randomId(), prefix, Markers.EMPTY, word);
    }

    // ================================================================
    // Compound commands
    // ================================================================

    private Bash.Statement visitCompoundCommand(BashParser.CompoundCommandContext ctx) {
        if (ctx.ifClause() != null) {
            return visitIfClause(ctx.ifClause());
        }
        if (ctx.forClause() != null) {
            return visitForClause(ctx.forClause());
        }
        if (ctx.cStyleForClause() != null) {
            return visitCStyleForClause(ctx.cStyleForClause());
        }
        if (ctx.whileClause() != null) {
            return visitWhileUntil(ctx.whileClause().WHILE(), ctx.whileClause().compoundList(), ctx.whileClause().doGroup());
        }
        if (ctx.untilClause() != null) {
            return visitWhileUntil(ctx.untilClause().UNTIL(), ctx.untilClause().compoundList(), ctx.untilClause().doGroup());
        }
        if (ctx.caseClause() != null) {
            return visitCaseClause(ctx.caseClause());
        }
        if (ctx.braceGroup() != null) {
            return visitBraceGroup(ctx.braceGroup());
        }
        if (ctx.subshell() != null) {
            return visitSubshell(ctx.subshell());
        }
        if (ctx.doubleParenExpr() != null) {
            return visitDoubleParenExpr(ctx.doubleParenExpr());
        }
        if (ctx.doubleBracketExpr() != null) {
            return visitDoubleBracketExpr(ctx.doubleBracketExpr());
        }
        if (ctx.selectClause() != null) {
            return visitSelectClause(ctx.selectClause());
        }
        throw new IllegalStateException("Unknown compound command type");
    }

    // ================================================================
    // If/elif/else/fi
    // ================================================================

    private Bash.Statement visitIfClause(BashParser.IfClauseContext ctx) {
        if (ctx.THEN() == null || ctx.FI() == null) {
            return fallbackCommand(ctx);
        }
        Space prefix = prefix(ctx.IF().getSymbol());

        Bash.Literal ifKeyword = visitKeyword(ctx.IF().getSymbol());
        List<Bash.Statement> condition = visitCompoundList(ctx.compoundList(0));
        Bash.Literal thenKeyword = visitKeyword(ctx.THEN().getSymbol());
        List<Bash.Statement> thenBody = visitCompoundList(ctx.compoundList(1));

        List<Bash.Elif> elifs = new ArrayList<>();
        for (BashParser.ElifClauseContext elifCtx : ctx.elifClause()) {
            elifs.add(visitElifClause(elifCtx));
        }

        Bash.@Nullable Else elseClause = null;
        if (ctx.elseClause() != null) {
            elseClause = visitElseClause(ctx.elseClause());
        }

        Bash.Literal fiKeyword = visitKeyword(ctx.FI().getSymbol());

        return new Bash.IfStatement(randomId(), prefix, Markers.EMPTY,
                ifKeyword, condition, thenKeyword, thenBody, elifs, elseClause, fiKeyword);
    }

    private Bash.Elif visitElifClause(BashParser.ElifClauseContext ctx) {
        if (ctx.THEN() == null) {
            // Fallback: capture entire elif clause as opaque text
            Space prefix = prefix(ctx.getStart());
            String text = sourceText(ctx);
            skipContext(ctx);
            Bash.Literal elifKeyword = new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text);
            return new Bash.Elif(randomId(), prefix, Markers.EMPTY, elifKeyword,
                    Collections.emptyList(),
                    new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, ""),
                    Collections.emptyList());
        }
        Space prefix = prefix(ctx.ELIF().getSymbol());
        Bash.Literal elifKeyword = visitKeyword(ctx.ELIF().getSymbol());
        List<Bash.Statement> condition = visitCompoundList(ctx.compoundList(0));
        Bash.Literal thenKeyword = visitKeyword(ctx.THEN().getSymbol());
        List<Bash.Statement> body = visitCompoundList(ctx.compoundList(1));
        return new Bash.Elif(randomId(), prefix, Markers.EMPTY, elifKeyword, condition, thenKeyword, body);
    }

    private Bash.Else visitElseClause(BashParser.ElseClauseContext ctx) {
        Space prefix = prefix(ctx.ELSE().getSymbol());
        Bash.Literal elseKeyword = visitKeyword(ctx.ELSE().getSymbol());
        List<Bash.Statement> body = visitCompoundList(ctx.compoundList());
        return new Bash.Else(randomId(), prefix, Markers.EMPTY, elseKeyword, body);
    }

    // ================================================================
    // For loops
    // ================================================================

    private Bash.ForLoop visitForClause(BashParser.ForClauseContext ctx) {
        Space prefix = prefix(ctx.FOR().getSymbol());
        Bash.Literal forKeyword = visitKeyword(ctx.FOR().getSymbol());

        Space varPrefix = prefix(ctx.WORD().getSymbol());
        String varName = ctx.WORD().getText();
        skip(ctx.WORD().getSymbol());
        Bash.Literal variable = new Bash.Literal(randomId(), varPrefix, Markers.EMPTY, varName);

        BashParser.ForBodyContext body = ctx.forBody();

        Bash.@Nullable Literal inKeyword = null;
        List<Bash.Expression> iterables = new ArrayList<>();
        Bash.Literal separator;

        if (body.inClause() != null) {
            BashParser.InClauseContext inCtx = body.inClause();
            inKeyword = visitKeyword(inCtx.IN().getSymbol());
            for (BashParser.WordContext w : inCtx.word()) {
                iterables.add(visitWord(w));
            }
            separator = visitSequentialSep(inCtx.sequentialSep());
        } else {
            // No in clause, separator before do
            separator = new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "");
        }

        BashParser.DoGroupContext doGroup = body.doGroup();
        Bash.Literal doKeyword = visitKeyword(doGroup.DO().getSymbol());
        List<Bash.Statement> loopBody = visitCompoundList(doGroup.compoundList());
        Bash.Literal doneKeyword = visitKeyword(doGroup.DONE().getSymbol());

        return new Bash.ForLoop(randomId(), prefix, Markers.EMPTY,
                forKeyword, variable, inKeyword, iterables, separator, doKeyword, loopBody, doneKeyword);
    }

    // ================================================================
    // Select clause (reuses ForLoop LST since structure is identical)
    // ================================================================

    private Bash.ForLoop visitSelectClause(BashParser.SelectClauseContext ctx) {
        Space prefix = prefix(ctx.SELECT().getSymbol());
        Bash.Literal selectKeyword = visitKeyword(ctx.SELECT().getSymbol());

        Space varPrefix = prefix(ctx.WORD().getSymbol());
        String varName = ctx.WORD().getText();
        skip(ctx.WORD().getSymbol());
        Bash.Literal variable = new Bash.Literal(randomId(), varPrefix, Markers.EMPTY, varName);

        BashParser.InClauseContext inCtx = ctx.inClause();
        Bash.Literal inKeyword = visitKeyword(inCtx.IN().getSymbol());
        List<Bash.Expression> iterables = new ArrayList<>();
        for (BashParser.WordContext w : inCtx.word()) {
            iterables.add(visitWord(w));
        }
        Bash.Literal separator = visitSequentialSep(inCtx.sequentialSep());

        BashParser.DoGroupContext doGroup = ctx.doGroup();
        Bash.Literal doKeyword = visitKeyword(doGroup.DO().getSymbol());
        List<Bash.Statement> body = visitCompoundList(doGroup.compoundList());
        Bash.Literal doneKeyword = visitKeyword(doGroup.DONE().getSymbol());

        return new Bash.ForLoop(randomId(), prefix, Markers.EMPTY,
                selectKeyword, variable, inKeyword, iterables, separator, doKeyword, body, doneKeyword);
    }

    // ================================================================
    // C-style for loops: for ((init; cond; update))
    // ================================================================

    private Bash.Statement visitCStyleForClause(BashParser.CStyleForClauseContext ctx) {
        BashParser.DoGroupContext doGroup = ctx.doGroup();
        if (ctx.DOUBLE_RPAREN() == null || doGroup == null || doGroup.DO() == null || doGroup.DONE() == null) {
            return fallbackCommand(ctx);
        }
        Space prefix = prefix(ctx.FOR().getSymbol());

        // Capture the entire "for ((...))' header as opaque text including the for keyword
        int headerStart = toCharIndex(ctx.FOR().getSymbol().getStartIndex());
        int headerEnd = toCharIndex(ctx.DOUBLE_RPAREN().getSymbol().getStopIndex() + 1);
        String headerText = source.substring(headerStart, headerEnd);
        advanceCursor(headerEnd);
        Bash.Literal header = new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, headerText);

        // Separator before do
        Bash.Literal separator;
        if (ctx.sequentialSep() != null) {
            separator = visitSequentialSep(ctx.sequentialSep());
        } else {
            separator = new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "");
        }

        Bash.Literal doKeyword = visitKeyword(doGroup.DO().getSymbol());
        List<Bash.Statement> body = visitCompoundList(doGroup.compoundList());
        Bash.Literal doneKeyword = visitKeyword(doGroup.DONE().getSymbol());

        return new Bash.CStyleForLoop(randomId(), prefix, Markers.EMPTY,
                header, separator, doKeyword, body, doneKeyword);
    }

    // ================================================================
    // While/Until loops
    // ================================================================

    private Bash.Statement visitWhileUntil(TerminalNode keywordNode,
                                           BashParser.CompoundListContext condCtx,
                                           BashParser.DoGroupContext doGroup) {
        if (doGroup == null || doGroup.DO() == null || doGroup.DONE() == null) {
            // Fallback: capture the whole while/until clause parent as opaque text
            return fallbackCommand(keywordNode.getParent() instanceof ParserRuleContext ?
                    (ParserRuleContext) keywordNode.getParent() : condCtx);
        }
        Space prefix = prefix(keywordNode.getSymbol());
        Bash.Literal keyword = visitKeyword(keywordNode.getSymbol());
        List<Bash.Statement> condition = visitCompoundList(condCtx);
        Bash.Literal doKeyword = visitKeyword(doGroup.DO().getSymbol());
        List<Bash.Statement> body = visitCompoundList(doGroup.compoundList());
        Bash.Literal doneKeyword = visitKeyword(doGroup.DONE().getSymbol());

        return new Bash.WhileLoop(randomId(), prefix, Markers.EMPTY,
                keyword, condition, doKeyword, body, doneKeyword);
    }

    // ================================================================
    // Case statements
    // ================================================================

    private Bash.CaseStatement visitCaseClause(BashParser.CaseClauseContext ctx) {
        Space prefix = prefix(ctx.CASE().getSymbol());
        Bash.Literal caseKeyword = visitKeyword(ctx.CASE().getSymbol());
        Bash.Expression word = visitWord(ctx.word());
        Bash.Literal inKeyword = visitKeyword(ctx.IN().getSymbol());

        List<Bash.CaseItem> items = new ArrayList<>();
        for (BashParser.CaseItemContext item : ctx.caseItem()) {
            items.add(visitCaseItem(item));
        }

        Bash.Literal esacKeyword = visitKeyword(ctx.ESAC().getSymbol());

        return new Bash.CaseStatement(randomId(), prefix, Markers.EMPTY,
                caseKeyword, word, inKeyword, items, esacKeyword);
    }

    private Bash.CaseItem visitCaseItem(BashParser.CaseItemContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Optional leading ( before pattern
        if (ctx.LPAREN() != null) {
            skip(ctx.LPAREN().getSymbol());
        }

        // Pattern - capture as opaque text including ) delimiter
        BashParser.PatternContext patCtx = ctx.pattern();
        Space patPrefix = prefix(patCtx.getStart());
        int patStart = toCharIndex(patCtx.getStart().getStartIndex());
        // Capture everything from pattern start through the closing )
        int rparenEnd = toCharIndex(ctx.RPAREN().getSymbol().getStopIndex() + 1);
        String patText = source.substring(patStart, rparenEnd);
        advanceCursor(rparenEnd);
        if (ctx.LPAREN() != null) {
            patText = "(" + patText;
        }
        Bash.Literal pattern = new Bash.Literal(randomId(), patPrefix, Markers.EMPTY, patText);

        // Body
        List<Bash.Statement> body = new ArrayList<>();
        if (ctx.compoundList() != null) {
            body = visitCompoundList(ctx.compoundList());
        }

        // Separator (;; or ;& or ;;&)
        Bash.@Nullable Literal separator = null;
        if (ctx.caseSeparator() != null) {
            separator = visitCaseSeparator(ctx.caseSeparator());
        }

        return new Bash.CaseItem(randomId(), prefix, Markers.EMPTY, pattern, body, separator);
    }

    private Bash.Literal visitCaseSeparator(BashParser.CaseSeparatorContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String text = ctx.getStart().getText();
        skip(ctx.getStart());
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    // ================================================================
    // Functions
    // ================================================================

    private Bash.Function visitFunctionDefinition(BashParser.FunctionDefinitionContext ctx) {
        Space prefix = prefix(ctx.getStart());

        // Capture the function header (everything before the body)
        BashParser.FunctionBodyContext bodyCtx = ctx.functionBody();
        int headerStart = toCharIndex(ctx.getStart().getStartIndex());
        int headerEnd;

        // Find where the body starts
        BashParser.CompoundCommandContext compoundCmd = bodyCtx.compoundCommand();
        headerEnd = toCharIndex(compoundCmd.getStart().getStartIndex());

        Space headerPrefix = Space.EMPTY;
        String headerText = source.substring(headerStart, headerEnd);
        // Trim trailing whitespace from header - it will be the prefix of body
        int trimEnd = headerText.length();
        while (trimEnd > 0 && Character.isWhitespace(headerText.charAt(trimEnd - 1))) {
            trimEnd--;
        }
        String trimmedHeader = headerText.substring(0, trimEnd);
        advanceCursor(headerStart + trimmedHeader.length());
        Bash.Literal header = new Bash.Literal(randomId(), headerPrefix, Markers.EMPTY, trimmedHeader);

        // Body
        Bash.Statement body = visitCompoundCommand(compoundCmd);

        // Redirections after function body
        if (bodyCtx.redirectionList() != null) {
            body = attachRedirections(body, bodyCtx.redirectionList());
        }

        return new Bash.Function(randomId(), prefix, Markers.EMPTY, header, body);
    }

    // ================================================================
    // Brace group and subshell
    // ================================================================

    private Bash.Statement visitBraceGroup(BashParser.BraceGroupContext ctx) {
        if (ctx.RBRACE() == null) {
            return fallbackCommand(ctx);
        }
        Space prefix = prefix(ctx.LBRACE().getSymbol());
        skip(ctx.LBRACE().getSymbol());
        List<Bash.Statement> body = visitCompoundList(ctx.compoundList());
        // Check for synthetic RBRACE (ANTLR error recovery inserted a fake })
        // In this case, don't emit } — it will be captured naturally by
        // subsequent cursor tracking as the real } appears later in the source.
        if (ctx.RBRACE().getSymbol().getStartIndex() < 0) {
            return new Bash.BraceGroup(randomId(), prefix, Markers.EMPTY, body, null);
        }
        Space closingBrace = prefix(ctx.RBRACE().getSymbol());
        skip(ctx.RBRACE().getSymbol());
        return new Bash.BraceGroup(randomId(), prefix, Markers.EMPTY, body, closingBrace);
    }

    private Bash.Subshell visitSubshell(BashParser.SubshellContext ctx) {
        if (ctx.RPAREN() == null) {
            // ANTLR error recovery — missing RPAREN, fall back
            Space prefix = prefix(ctx.LPAREN().getSymbol());
            skip(ctx.LPAREN().getSymbol());
            List<Bash.Statement> body = visitCompoundList(ctx.compoundList());
            return new Bash.Subshell(randomId(), prefix, Markers.EMPTY, body, null);
        }
        Space prefix = prefix(ctx.LPAREN().getSymbol());
        skip(ctx.LPAREN().getSymbol());
        List<Bash.Statement> body = visitCompoundList(ctx.compoundList());
        // Check for synthetic RPAREN
        if (ctx.RPAREN().getSymbol().getStartIndex() < 0) {
            return new Bash.Subshell(randomId(), prefix, Markers.EMPTY, body, null);
        }
        Space closingParen = prefix(ctx.RPAREN().getSymbol());
        skip(ctx.RPAREN().getSymbol());
        return new Bash.Subshell(randomId(), prefix, Markers.EMPTY, body, closingParen);
    }

    // ================================================================
    // (( expr )) and [[ expr ]]
    // ================================================================

    private Bash.Statement visitDoubleParenExpr(BashParser.DoubleParenExprContext ctx) {
        if (ctx.DOUBLE_RPAREN() == null) {
            return fallbackCommand(ctx);
        }
        int closeStart = toCharIndex(ctx.DOUBLE_RPAREN().getSymbol().getStartIndex());
        if (closeStart < 0 || closeStart < cursor) {
            return fallbackCommand(ctx);
        }
        Space prefix = prefix(ctx.DOUBLE_LPAREN().getSymbol());
        skip(ctx.DOUBLE_LPAREN().getSymbol());

        // Capture all text between (( and )) including leading/trailing whitespace
        String expr = source.substring(cursor, closeStart);
        advanceCursor(closeStart);

        skip(ctx.DOUBLE_RPAREN().getSymbol());
        return new Bash.ArithmeticExpansion(randomId(), prefix, Markers.EMPTY, false, expr);
    }

    private Bash.Statement visitDoubleBracketExpr(BashParser.DoubleBracketExprContext ctx) {
        if (ctx.DOUBLE_RBRACKET() == null) {
            return fallbackCommand(ctx);
        }
        Space prefix = prefix(ctx.DOUBLE_LBRACKET().getSymbol());
        skip(ctx.DOUBLE_LBRACKET().getSymbol());

        // Capture expression between [[ and ]]
        String exprText = sourceText(ctx.conditionExpr());
        Space exprPrefix = prefix(ctx.conditionExpr().getStart());
        skipContext(ctx.conditionExpr());
        Bash.Literal expression = new Bash.Literal(randomId(), exprPrefix, Markers.EMPTY, exprText);

        Space closingPrefix = prefix(ctx.DOUBLE_RBRACKET().getSymbol());
        String closingText = "]]";
        skip(ctx.DOUBLE_RBRACKET().getSymbol());
        Bash.Literal closingBracket = new Bash.Literal(randomId(), closingPrefix, Markers.EMPTY, closingText);

        return new Bash.ConditionalExpression(randomId(), prefix, Markers.EMPTY, expression, closingBracket);
    }

    // ================================================================
    // Compound list (used inside braces, if bodies, etc.)
    // ================================================================

    private List<Bash.Statement> visitCompoundList(BashParser.CompoundListContext ctx) {
        // linebreak completeCommands linebreak
        // Don't skip linebreaks — they will be captured as prefix of subsequent elements
        return visitCompleteCommands(ctx.completeCommands());
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private Bash.Literal visitKeyword(Token token) {
        Space prefix = prefix(token);
        // Synthetic tokens from ANTLR error recovery have startIndex < 0
        // and getText() returns "<missing 'TYPE'>" — suppress that text
        String text = token.getStartIndex() >= 0 ? token.getText() : "";
        skip(token);
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.Literal visitTerminal(TerminalNode node) {
        Space prefix = prefix(node.getSymbol());
        // Synthetic tokens from ANTLR error recovery have startIndex < 0
        // and getText() returns "<missing 'TYPE'>" — suppress that text
        String text = node.getSymbol().getStartIndex() >= 0 ? node.getText() : "";
        skip(node.getSymbol());
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    private Bash.Literal visitSequentialSep(BashParser.SequentialSepContext ctx) {
        if (ctx.SEMI() != null) {
            Space prefix = prefix(ctx.SEMI().getSymbol());
            skip(ctx.SEMI().getSymbol());
            // Don't skip linebreak — it will be captured as prefix of the next keyword
            return new Bash.Literal(randomId(), prefix, Markers.EMPTY, ";");
        }
        // newlineList — don't skip, it will be captured as prefix of the next keyword
        return new Bash.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "");
    }

    private List<Bash.Statement> attachSeparator(List<Bash.Statement> stmts, BashParser.SeparatorContext ctx) {
        if (ctx.AMP() != null) {
            // Wrap the last statement in Background
            Space ampPrefix = prefix(ctx.AMP().getSymbol());
            skip(ctx.AMP().getSymbol());
            Bash.Statement last = stmts.get(stmts.size() - 1);
            Bash.Background bg = new Bash.Background(randomId(), last.getPrefix(), Markers.EMPTY,
                    last.withPrefix(Space.EMPTY), ampPrefix);
            List<Bash.Statement> result = new ArrayList<>(stmts.subList(0, stmts.size() - 1));
            result.add(bg);
            return result;
        }
        // For SEMI (;), don't skip — the separator text will be captured
        // as part of the prefix of the next element
        return stmts;
    }

    private Bash.Statement attachRedirections(Bash.Statement stmt, BashParser.RedirectionListContext ctx) {
        List<Bash.Expression> redirections = new ArrayList<>();
        for (BashParser.RedirectionContext redir : ctx.redirection()) {
            redirections.add(visitRedirection(redir));
        }

        return new Bash.Redirected(randomId(), stmt.getPrefix(), Markers.EMPTY,
                stmt.withPrefix(Space.EMPTY), redirections);
    }

    private TerminalNode getFirstTerminal(ParserRuleContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                return (TerminalNode) child;
            }
        }
        throw new IllegalStateException("No terminal node found in " + ctx.getClass().getSimpleName());
    }

    private String sourceText(ParserRuleContext ctx) {
        if (ctx.getStop() == null) {
            int start = toCharIndex(ctx.getStart().getStartIndex());
            if (start < source.length()) {
                return source.substring(start, source.length());
            }
            return "";
        }
        int start = toCharIndex(ctx.getStart().getStartIndex());
        int stop = toCharIndex(ctx.getStop().getStopIndex() + 1);
        if (stop > source.length()) {
            stop = source.length();
        }
        if (start > stop || start < 0 || stop < 0) {
            return "";
        }
        return source.substring(start, stop);
    }

    /**
     * Fallback for when ANTLR error recovery produces incomplete parse trees.
     * Captures the entire context as opaque literal text (Expression).
     */
    private Bash.Literal fallbackLiteral(ParserRuleContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String text = sourceText(ctx);
        skipContext(ctx);
        return new Bash.Literal(randomId(), prefix, Markers.EMPTY, text);
    }

    /**
     * Fallback for when ANTLR error recovery produces incomplete parse trees.
     * Wraps in a Command so it can be used where Statement is needed.
     */
    private Bash.Command fallbackCommand(ParserRuleContext ctx) {
        Bash.Literal literal = fallbackLiteral(ctx);
        return new Bash.Command(randomId(), literal.getPrefix(), Markers.EMPTY,
                Collections.emptyList(),
                Collections.singletonList(literal.withPrefix(Space.EMPTY)));
    }

    // ================================================================
    // Cursor management (same pattern as DockerParserVisitor)
    // ================================================================

    private Space prefix(Token token) {
        int startIndex = token.getStartIndex();
        if (startIndex < 0) {
            return Space.EMPTY;
        }
        int start = toCharIndex(startIndex);
        if (start < cursor || start > source.length()) {
            return Space.EMPTY;
        }
        String prefixText = source.substring(cursor, start);
        cursor = start;
        return Space.format(prefixText);
    }

    private void advanceCursor(int newIndex) {
        if (newIndex > cursor) {
            cursor = newIndex;
        }
    }

    private void skip(Token token) {
        if (token != null) {
            advanceCursor(toCharIndex(token.getStopIndex() + 1));
        }
    }

    private void skipContext(ParserRuleContext ctx) {
        if (ctx.getStop() != null) {
            advanceCursor(toCharIndex(ctx.getStop().getStopIndex() + 1));
        }
    }

}
