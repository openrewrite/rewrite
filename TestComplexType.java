import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import java.util.Collections;

public class TestComplexType {
    public static void main(String[] args) {
        // Create a simple identifier "Int"
        J.Identifier intId = new J.Identifier(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            Collections.emptyList(),
            "Int",
            null,
            null
        );
        
        System.out.println("Identifier simpleName: " + intId.getSimpleName());
        System.out.println("Identifier toString: " + intId);
    }
}