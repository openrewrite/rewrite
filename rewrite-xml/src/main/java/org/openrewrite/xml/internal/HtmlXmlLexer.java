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
package org.openrewrite.xml.internal;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.jspecify.annotations.Nullable;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParserBase;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link XMLLexer} that tolerates the parts of HTML that XML does not allow, so that the XML grammar and
 * {@link XmlParserVisitor} can parse HTML without a second grammar. The strict {@link XMLLexer} remains in use for
 * everything else. It differs from it in that it:
 * <ul>
 *     <li>reads the content of {@code <script>}, {@code <style>} and {@code <textarea>} as text, since a {@code <}
 *     or {@code &} in it does not begin markup;</li>
 *     <li>reads {@code <} and {@code &} that cannot begin a tag or an entity as text;</li>
 *     <li>reads {@code <} in a quoted attribute value, and unquoted attribute values;</li>
 *     <li>reads any name that does not contain whitespace or one of {@code "'>/=} as an attribute name, which
 *     covers {@code *ngIf}, {@code [title]}, {@code (click)}, {@code @click} and {@code #ref};</li>
 *     <li>supplies the end tags HTML lets an author leave off, as zero-width tokens that
 *     {@link XmlParserVisitor} recognizes: an {@code <li>} ended by the next {@code <li>} or by the enclosing
 *     {@code </ul>}, a {@code <p>} ended by a block element, and so on, including {@code </body></html>} at the
 *     end of the file.</li>
 * </ul>
 * Attributes without a value are admitted by the parser grammar itself.
 */
public class HtmlXmlLexer extends XMLLexer {

    private static final Set<String> RAW_TEXT_ELEMENTS = new HashSet<>(Arrays.asList("script", "style", "textarea"));

    /**
     * Elements whose end tag HTML lets an author leave off, when what follows makes it clear where they end.
     */
    private static final Set<String> OPTIONAL_END_TAG = new HashSet<>(Arrays.asList(
            "html", "head", "body", "p", "li", "dt", "dd", "tr", "td", "th", "thead", "tbody", "tfoot",
            "option", "optgroup", "colgroup", "caption"));

    private static final Set<String> CLOSES_P = new HashSet<>(Arrays.asList(
            "address", "article", "aside", "blockquote", "details", "div", "dl", "fieldset", "figcaption",
            "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "main",
            "menu", "nav", "ol", "p", "pre", "section", "table", "ul"));

    /**
     * The start tags that implicitly end an open element, by the open element's name.
     */
    private static final Map<String, Set<String>> CLOSED_BY_START_TAG = new HashMap<>();

    static {
        CLOSED_BY_START_TAG.put("li", set("li"));
        CLOSED_BY_START_TAG.put("dt", set("dt", "dd"));
        CLOSED_BY_START_TAG.put("dd", set("dt", "dd"));
        CLOSED_BY_START_TAG.put("p", CLOSES_P);
        CLOSED_BY_START_TAG.put("tr", set("tr", "tbody", "thead", "tfoot"));
        CLOSED_BY_START_TAG.put("td", set("td", "th", "tr", "tbody", "thead", "tfoot"));
        CLOSED_BY_START_TAG.put("th", set("td", "th", "tr", "tbody", "thead", "tfoot"));
        CLOSED_BY_START_TAG.put("thead", set("tbody", "tfoot"));
        CLOSED_BY_START_TAG.put("tbody", set("tbody", "tfoot"));
        CLOSED_BY_START_TAG.put("option", set("option", "optgroup"));
        CLOSED_BY_START_TAG.put("optgroup", set("optgroup"));
        CLOSED_BY_START_TAG.put("head", set("body"));
    }

    private static final Pattern ENTITY_OR_CHAR_REF = Pattern.compile(
            "&(?:#[0-9]+|#x[0-9a-fA-F]+|[_:a-zA-Z\\u00C0-\\uFFFD][-_.:a-zA-Z0-9\\u00B7\\u00C0-\\uFFFD]*);");

    private static Set<String> set(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    // State of the scanner, which produces tokens as the XML lexer or this class's own rules see fit.
    private int previousType = -1;
    private boolean afterOpen;
    private boolean inStartTag;
    private @Nullable String pendingStartName;
    private @Nullable String rawTextElement;
    private int lastStop = -1;

    // State of the structure, which supplies the end tags that were left off.
    private final Deque<String> openElements = new ArrayDeque<>();
    private final Deque<Token> pending = new ArrayDeque<>();
    private @Nullable String startTagName;
    private boolean endOfFile;
    private boolean misnested;

    public HtmlXmlLexer(CharStream input) {
        super(input);
    }

    @Override
    public Token nextToken() {
        if (!pending.isEmpty()) {
            return pending.poll();
        }

        int end = lastStop + 1;
        Token token = scan();
        switch (token.getType()) {
            case OPEN: {
                Token next = scan();
                if (next.getType() == Name) {
                    closeForStartTag(next.getText(), end);
                    pending.add(token);
                    pending.add(next);
                } else if (next.getType() == SLASH) {
                    Token name = scan();
                    if (name.getType() == Name) {
                        closeForEndTag(name.getText(), end);
                    }
                    pending.add(token);
                    pending.add(next);
                    pending.add(name);
                } else {
                    pending.add(token);
                    pending.add(next);
                }
                break;
            }
            case CLOSE:
                if (startTagName != null && !XMLParserBase.isHtmlVoidElement(startTagName)) {
                    openElements.push(startTagName);
                }
                startTagName = null;
                pending.add(token);
                break;
            case SLASH_CLOSE:
                startTagName = null;
                pending.add(token);
                break;
            case Token.EOF:
                if (!endOfFile) {
                    endOfFile = true;
                    while (!misnested && !openElements.isEmpty() && isOptionalEndTag(openElements.peek())) {
                        virtualEndTag(openElements.pop(), end);
                    }
                }
                pending.add(token);
                break;
            default:
                pending.add(token);
        }
        return pending.poll();
    }

    private void closeForStartTag(String name, int end) {
        String lower = name.toLowerCase();
        while (!misnested && !openElements.isEmpty()) {
            Set<String> closers = CLOSED_BY_START_TAG.get(openElements.peek().toLowerCase());
            if (closers == null || !closers.contains(lower)) {
                break;
            }
            virtualEndTag(openElements.pop(), end);
        }
        startTagName = name;
    }

    private void closeForEndTag(String name, int end) {
        if (misnested) {
            if (!openElements.isEmpty()) {
                openElements.pop();
            }
            return;
        }
        // Only when every element in between may have its end tag left off does the end tag name an outer element
        int skipped = 0;
        boolean ends = false;
        for (String element : openElements) {
            if (element.equalsIgnoreCase(name)) {
                ends = true;
                break;
            }
            if (!isOptionalEndTag(element)) {
                break;
            }
            skipped++;
        }
        for (int i = 0; ends && i < skipped; i++) {
            virtualEndTag(openElements.pop(), end);
        }
        // Whatever it names, an end tag ends the innermost element, as it does for the parser
        if (!openElements.isEmpty()) {
            if (!ends) {
                // The end tag names no element that is open, or one with elements that need end tags in the way.
                // Nothing that follows can be told apart from a missing end tag, which is left to be reported.
                misnested = true;
            }
            openElements.pop();
        }
    }

    private static boolean isOptionalEndTag(String name) {
        return OPTIONAL_END_TAG.contains(name.toLowerCase());
    }

    /**
     * An end tag without any width, directly after the last thing in the source that belongs to the element.
     */
    private void virtualEndTag(String name, int position) {
        pending.add(virtual(OPEN, "<", position));
        pending.add(virtual(SLASH, "/", position));
        pending.add(virtual(Name, name, position));
        pending.add(virtual(CLOSE, ">", position));
    }

    private Token virtual(int type, String text, int position) {
        CommonToken token = new CommonToken(_tokenFactorySourcePair, type, Token.DEFAULT_CHANNEL, position, position - 1);
        token.setText(text);
        token.setLine(getLine());
        token.setCharPositionInLine(getCharPositionInLine());
        return token;
    }

    /**
     * @return Whether the token is an end tag's that {@link HtmlXmlLexer} supplied rather than found in the source.
     */
    public static boolean isVirtual(Token token) {
        return token.getStopIndex() < token.getStartIndex();
    }

    private Token scan() {
        Token token = scanHtml();
        if (token == null) {
            token = super.nextToken();
        }
        track(token);
        return token;
    }

    private void track(Token token) {
        int type = token.getType();
        if (type == Token.EOF) {
            return;
        }
        lastStop = token.getStopIndex();
        if (type == Name && afterOpen) {
            inStartTag = true;
            pendingStartName = token.getText();
        } else if (type == CLOSE && inStartTag) {
            if (RAW_TEXT_ELEMENTS.contains(pendingStartName.toLowerCase())) {
                rawTextElement = pendingStartName;
            }
            inStartTag = false;
        } else if (type == SLASH_CLOSE || type == SLASH || type == OPEN) {
            inStartTag = false;
        }
        afterOpen = type == OPEN;
        previousType = type;
    }

    private @Nullable Token scanHtml() {
        if (_mode == DEFAULT_MODE) {
            if (rawTextElement != null) {
                String element = rawTextElement;
                rawTextElement = null;
                Token rawText = rawText(element);
                if (rawText != null) {
                    return rawText;
                }
            }
            return strayMarkup();
        } else if (_mode == INSIDE && inStartTag) {
            return insideStartTag();
        }
        return null;
    }

    /**
     * The content of an element such as {@code <script>}, up to its end tag, if what is in it would trip up the XML
     * lexer. Content that the XML lexer reads just as well, such as a JSP scriptlet, is left to it.
     */
    private @Nullable Token rawText(String element) {
        int start = _input.index();
        String rest = _input.getText(Interval.of(start, _input.size() - 1));
        Matcher endTag = Pattern.compile("</" + Pattern.quote(element) + "(?=[\\s>/]|\\z)", Pattern.CASE_INSENSITIVE).matcher(rest);
        int end = endTag.find() ? endTag.start() : rest.length();
        String content = rest.substring(0, end);
        if (content.isEmpty() || !hasStrayMarkup(content)) {
            return null;
        }
        return textToken(content.codePointCount(0, content.length()));
    }

    private static boolean hasStrayMarkup(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '<' && !content.startsWith("<%", i)) {
                return true;
            } else if (c == '&' && !ENTITY_OR_CHAR_REF.matcher(content).region(i, content.length()).lookingAt()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A {@code <} that does not begin a tag, comment, or the like, or an {@code &} that does not begin an entity,
     * along with the text that follows it.
     */
    private @Nullable Token strayMarkup() {
        int offset = 1;
        while (isWhitespace(_input.LA(offset))) {
            offset++;
        }
        int c = _input.LA(offset);
        boolean stray;
        if (c == '&') {
            String ahead = _input.getText(Interval.of(_input.index() + offset - 1, Math.min(_input.size() - 1, _input.index() + offset + 80)));
            stray = !ENTITY_OR_CHAR_REF.matcher(ahead).lookingAt();
        } else if (c == '<') {
            int next = _input.LA(offset + 1);
            stray = next == Token.EOF || isWhitespace(next) || next == '=' || next == '<' ||
                    (next >= '0' && next <= '9');
        } else {
            return null;
        }
        if (!stray) {
            return null;
        }
        int length = offset;
        for (int c2 = _input.LA(length + 1); c2 != Token.EOF && c2 != '<' && c2 != '&'; c2 = _input.LA(length + 1)) {
            length++;
        }
        return textToken(length);
    }

    private Token insideStartTag() {
        skipWhitespace();
        int c = _input.LA(1);
        if (c == Token.EOF || c == '>' || c == '=' || c == '<') {
            return null;
        }
        if (previousType != EQUALS && (c == '/' || c == '%' || c == '?')) {
            return null;
        }
        if (c == '"' || c == '\'') {
            if (previousType != EQUALS) {
                return null;
            }
            int close = 2;
            while (_input.LA(close) != c) {
                if (_input.LA(close) == Token.EOF) {
                    return null;
                }
                close++;
            }
            return token(STRING, close);
        }
        int length = 1;
        if (previousType == EQUALS) {
            // An unquoted value ends at whitespace or at the end of the tag
            while (!isWhitespace(_input.LA(length + 1)) && _input.LA(length + 1) != '>' && _input.LA(length + 1) != Token.EOF) {
                length++;
            }
            return token(STRING, length);
        }
        while (!isWhitespace(_input.LA(length + 1)) && !isAttributeNameEnd(_input.LA(length + 1))) {
            length++;
        }
        return token(Name, length);
    }

    private static boolean isAttributeNameEnd(int c) {
        return c == Token.EOF || c == '>' || c == '/' || c == '=' || c == '"' || c == '\'';
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private void skipWhitespace() {
        while (isWhitespace(_input.LA(1))) {
            getInterpreter().consume(_input);
        }
    }

    private Token textToken(int length) {
        return token(TEXT, length);
    }

    /**
     * A token for the next characters of the input, which are consumed.
     */
    private Token token(int type, int length) {
        int start = _input.index();
        int line = getLine();
        int column = getCharPositionInLine();
        for (int i = 0; i < length; i++) {
            getInterpreter().consume(_input);
        }
        return _factory.create(_tokenFactorySourcePair, type, null, Token.DEFAULT_CHANNEL, start,
                start + length - 1, line, column);
    }
}
