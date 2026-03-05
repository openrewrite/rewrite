public class DebugSpans {
    public static void main(String[] args) {
        String code = "object Test {\n  var (a, b) = (1, 2)\n  (a, b) = (3, 4)\n}";
        System.out.println("Source code:");
        System.out.println(code);
        System.out.println("\nLine 3: '(a, b) = (3, 4)'");
        
        // Find the position of the second tuple
        int lineStart = code.indexOf("  (a, b)");
        System.out.println("Start of line 3: position " + lineStart);
        
        for (int i = lineStart; i < lineStart + 20 && i < code.length(); i++) {
            char c = code.charAt(i);
            System.out.printf("  pos %2d: '%s'\n", i, c == '\n' ? "\\n" : String.valueOf(c));
        }
    }
}
