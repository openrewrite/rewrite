package org.openrewrite.cobol.internal.preprocessor.sub.line.reader;

public class CobolScanner {
    private final String text;
    private int index;

    private StringBuilder sb = new StringBuilder();

    public CobolScanner(String text) {
        this.text = text;
        this.index = 0;
    }

    public boolean hasNextLine() {
        return index < text.length();
    }

    public Line nextLine() {
        if(!(index < text.length())) {
            System.out.println();
        }

        int startIndex = index;

        sb.setLength(0);
        while(index < text.length() && !isNewLine(text.charAt(index))) {
            sb.append(text.charAt(index));
            index++;
        }
        String s = sb.toString();

        sb.setLength(0);
        while(index < text.length() && isNewLine(text.charAt(index))) {
            sb.append(text.charAt(index));
            index++;
        }
        String newLine = sb.toString();

        if(index == startIndex) {
            System.out.println();
        }

        return new Line(s, newLine);
    }

    private boolean isNewLine(char c) {
        return c == '\n' || c == '\r';
    }

    public void close() {
    }
}
