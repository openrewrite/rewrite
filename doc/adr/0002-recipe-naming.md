# 2. Naming recipes

Date: 2021-04-23

## Status

Accepted

## Context

Provide guidelines for naming recipes such that they show up in user-facing experiences in a consistent manner. OpenRewrite's `Recipe` class contains methods for providing a display name and description text and `@Option` annotations for inputs providing similar text options. This convention applies to both.

## Decision

We'll start with an example of a properly named recipe:

```java
@Value
@EqualsAndHashCode(callSuper = true)
public class DeleteKey extends Recipe {
    @Option(displayName = "Key path",
            description = "An XPath expression to locate a YAML entry.",
            example = "subjects/kind")
    String keyPath;

    @Override
    public String getDisplayName() {
        return "Delete key";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML mapping entry key.";
    }
}
```

### Display names

* **DO** provide a short name ideally consisting of less than 5 words, but definitely less than 10 words.
* **DO** use initial capping (i.e. don't capitalize every word).
* **DO NOT** end with a period.
* **DO** use markdown backticks (`) to delimit words representing code symbols, as in "JUnit4 `@RunWith` to JUnit Jupiter `@ExtendWith`".
* **DO** describe what the end result is positively whenever possible.

### Description text

* **DO** use at least one full sentence to describe the recipe, ending in a period.
* **DO** use markdown backticks (`) to delimit words representing code symbols.
* **DO NOT** reference alternatives in other static analyzers or products.

## Consequences

Various user experiences listing OpenRewrite recipes are able to present a clean view of available recipes by relying strictly on the metadata provided by the recipe.

![image](https://user-images.githubusercontent.com/1697736/115926276-74859480-a437-11eb-9c50-2d2d30cc4c24.png)
