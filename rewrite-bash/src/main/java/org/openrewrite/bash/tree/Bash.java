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
package org.openrewrite.bash.tree;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.bash.BashVisitor;
import org.openrewrite.bash.internal.BashPrinter;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Bash extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptBash(v.adapt(BashVisitor.class), p);
    }

    default <P> @Nullable Bash acceptBash(BashVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(BashVisitor.class);
    }

    Space getPrefix();

    <B extends Bash> B withPrefix(Space prefix);

    /**
     * Root node representing a complete bash script file
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Script implements Bash, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;
        Space prefix;
        Markers markers;

        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        @Nullable
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return Charset.forName(charsetName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Script withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Nullable
        Shebang shebang;

        List<Statement> statements;

        Space eof;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitScript(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new BashPrinter<>();
        }
    }

    /**
     * Shebang line: #!/bin/bash
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Shebang implements Bash {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The whole shebang text including #! (e.g., "#!/bin/bash")
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitShebang(this, p);
        }
    }

    /**
     * Base interface for all statements in a bash script
     */
    interface Statement extends Bash {
    }

    /**
     * A simple command: name followed by arguments and optional redirections.
     * This is the workhorse node for most bash commands.
     * Examples: echo "hello", curl -s "$url", mod --version
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Command implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Prefix assignments (e.g., VAR=value command args)
         */
        List<Assignment> assignments;

        /**
         * The command word(s) and arguments, including redirections,
         * as a flat list preserving original order and whitespace.
         */
        List<Expression> arguments;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCommand(this, p);
        }
    }

    /**
     * A pipeline: command1 | command2 | command3
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Pipeline implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        boolean negated;

        List<Statement> commands;

        /**
         * The pipe operators between commands (|, |&).
         * Length is commands.size() - 1.
         */
        List<PipeEntry> pipeOperators;

        public enum PipeOp {
            PIPE,
            PIPE_AND
        }

        @Value
        @With
        public static class PipeEntry {
            Space prefix;
            PipeOp operator;
        }

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitPipeline(this, p);
        }
    }

    /**
     * A command list connected by && or ||: cmd1 && cmd2 || cmd3
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CommandList implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Statement> commands;

        /**
         * The operators between commands (&& or ||).
         * Length is commands.size() - 1.
         */
        List<OperatorEntry> operators;

        public enum Operator {
            AND,
            OR
        }

        @Value
        @With
        public static class OperatorEntry {
            Space prefix;
            Operator operator;
        }

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCommandList(this, p);
        }
    }

    /**
     * Variable assignment: VAR=value, VAR+=value, VAR=(array elements)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Assignment implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The variable name
         */
        Literal name;

        /**
         * The operator (= or +=)
         */
        String operator;

        /**
         * The value being assigned (null for empty assignment like VAR=)
         */
        @Nullable
        Expression value;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitAssignment(this, p);
        }
    }

    /**
     * if/elif/else/fi construct
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class IfStatement implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The condition (between if/elif and then)
         */
        Literal ifKeyword;
        List<Statement> condition;
        Literal thenKeyword;
        List<Statement> thenBody;

        List<Elif> elifs;

        @Nullable
        Else elseClause;

        Literal fiKeyword;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitIfStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Elif implements Bash {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Literal elifKeyword;
        List<Statement> condition;
        Literal thenKeyword;
        List<Statement> body;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitElif(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Else implements Bash {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Literal elseKeyword;
        List<Statement> body;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitElse(this, p);
        }
    }

    /**
     * for var in list; do ... done
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ForLoop implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Literal forKeyword;
        Literal variable;

        @Nullable
        Literal inKeyword;

        /**
         * The words in the for list (empty for implicit "$@")
         */
        List<Expression> iterables;

        Literal separator; // ; or newline before do

        Literal doKeyword;
        List<Statement> body;
        Literal doneKeyword;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitForLoop(this, p);
        }
    }

    /**
     * C-style for loop: for ((init; cond; update)); do ... done
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CStyleForLoop implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The entire for ((...)) header as literal text for lossless printing
         */
        Literal header;

        Literal separator; // ; or newline before do

        Literal doKeyword;
        List<Statement> body;
        Literal doneKeyword;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCStyleForLoop(this, p);
        }
    }

    /**
     * while/until condition; do ... done
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WhileLoop implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Literal keyword; // "while" or "until"
        List<Statement> condition;
        Literal doKeyword;
        List<Statement> body;
        Literal doneKeyword;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitWhileLoop(this, p);
        }
    }

    /**
     * case word in pattern) ... ;; ... esac
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CaseStatement implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Literal caseKeyword;
        Expression word;
        Literal inKeyword;
        List<CaseItem> items;
        Literal esacKeyword;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCaseStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CaseItem implements Bash {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The pattern(s) as literal text (e.g., "o)", "-o|--org)", "*)")
         */
        Literal pattern;

        List<Statement> body;

        /**
         * The separator (;;, ;&, ;;&) or null for the last item without separator
         */
        @Nullable
        Literal separator;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCaseItem(this, p);
        }
    }

    /**
     * Function definition: function name { } or name() { }
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Function implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The function header text (e.g., "function name()" or "name()")
         */
        Literal header;

        /**
         * The function body (compound command)
         */
        Statement body;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitFunction(this, p);
        }
    }

    /**
     * Subshell: (commands)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Subshell implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Statement> body;
        @Nullable
        Space closingParen;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitSubshell(this, p);
        }
    }

    /**
     * Brace group: { commands; }
     * When closingBrace is null, the closing } was synthetic (ANTLR error recovery)
     * and should not be emitted by the printer.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class BraceGroup implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Statement> body;
        @Nullable
        Space closingBrace;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitBraceGroup(this, p);
        }
    }

    /**
     * Redirection: > file, >> file, 2>&1, etc.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Redirect implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The complete redirection as text (e.g., "> /dev/null", "2>&1", ">> file")
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitRedirect(this, p);
        }
    }

    /**
     * Here document
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class HereDoc implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The opening (e.g., "<<EOF", "<<-EOF", "<<'EOF'")
         */
        String opening;

        /**
         * Content lines between opening and closing markers
         */
        List<String> contentLines;

        /**
         * The closing marker (e.g., "EOF")
         */
        String closing;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitHereDoc(this, p);
        }
    }

    /**
     * Here string: <<< word
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class HereString implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Expression word;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitHereString(this, p);
        }
    }

    /**
     * Base interface for expressions (things that produce values)
     */
    interface Expression extends Bash {
    }

    /**
     * A word composed of one or more parts (concatenated without spaces).
     * Example: prefix${var}suffix, "hello $world", etc.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Word implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Expression> parts;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitWord(this, p);
        }
    }

    /**
     * A literal text fragment. This is the most basic building block.
     * Used for unquoted text, keywords, operators, numbers, etc.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The literal text as it appears in the source
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }
    }

    /**
     * A single-quoted string: 'text'
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SingleQuoted implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The text between the quotes (without the quotes themselves)
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitSingleQuoted(this, p);
        }
    }

    /**
     * A double-quoted string: "text with $interpolation"
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DoubleQuoted implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Parts of the double-quoted string (literals, variable expansions, command substitutions)
         */
        List<Expression> parts;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitDoubleQuoted(this, p);
        }
    }

    /**
     * Dollar single-quoted string: $'text with \n escapes'
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DollarSingleQuoted implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The full text including $' and closing '
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitDollarSingleQuoted(this, p);
        }
    }

    /**
     * Variable expansion: $VAR, ${VAR}, ${VAR:-default}, ${#VAR}, etc.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class VariableExpansion implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The complete variable expansion text (e.g., "$VAR", "${VAR:-default}")
         */
        String text;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitVariableExpansion(this, p);
        }
    }

    /**
     * Command substitution: $(command) or `command`
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CommandSubstitution implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Whether this uses $() or backtick syntax
         */
        boolean dollar;

        List<Statement> body;

        Space closingDelimiter;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitCommandSubstitution(this, p);
        }
    }

    /**
     * Arithmetic expansion: $((expr)) or ((expr))
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ArithmeticExpansion implements Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Whether this is a $((expr)) expansion or a ((expr)) statement
         */
        boolean dollar;

        /**
         * The expression text between the delimiters
         */
        String expression;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitArithmeticExpansion(this, p);
        }
    }

    /**
     * Process substitution: <(command) or >(command)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcessSubstitution implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Whether this is <() (input) or >() (output)
         */
        boolean input;

        List<Statement> body;

        /**
         * Whitespace before closing paren. Null when ANTLR error recovery
         * inserted a synthetic RPAREN (skip ) emission in printer).
         */
        @Nullable
        Space closingParen;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitProcessSubstitution(this, p);
        }
    }

    /**
     * Conditional expression: [[ expr ]]
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ConditionalExpression implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The complete expression text between [[ and ]]
         */
        Literal expression;

        Literal closingBracket;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitConditionalExpression(this, p);
        }
    }

    /**
     * A compound command with trailing redirections: while ...; do ...; done < input.txt
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Redirected implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Statement command;
        List<Expression> redirections;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitRedirected(this, p);
        }
    }

    /**
     * A command run in the background: cmd &
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Background implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Statement command;

        /**
         * Whitespace before the & operator
         */
        Space ampersandPrefix;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitBackground(this, p);
        }
    }

    /**
     * Array literal: (elem1 elem2 elem3)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ArrayLiteral implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Expression> elements;

        Space closingParen;

        @Override
        public <P> Bash acceptBash(BashVisitor<P> v, P p) {
            return v.visitArrayLiteral(this, p);
        }
    }
}
