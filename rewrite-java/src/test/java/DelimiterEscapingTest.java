import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DelimiterEscapingTest {
    @Test
    void testEscapeDelimiters() {
        // Test with pipe character
        String input = "text|with|pipes";
        String escaped = escapeDelimiters(input);
        assertEquals("text\\|with\\|pipes", escaped);
        
        String unescaped = unescapeDelimiters(escaped);
        assertEquals(input, unescaped);
    }
    
    private String escapeDelimiters(String value) {
        return value.replace("\\", "\\\\")
                   .replace("|", "\\|")
                   .replace("\"", "\\\"");
    }
    
    private String unescapeDelimiters(String value) {
        return value.replace("\\|", "|")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");
    }
}
