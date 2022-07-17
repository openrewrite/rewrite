package model;

/**
 * Either an {@link Identifier} or {@link Literal}.
 */
public interface Name extends Cobol {
    default String getSimpleName() {
        // will be implemented by @Getter in real model
        return null;
    }
}
