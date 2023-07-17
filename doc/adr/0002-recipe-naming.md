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

### Recipe package naming

Recipes are currently organized hierarchically by package name. These package naming guidelines apply to both recipes defined in code as well as the name given to declarative recipes defined in YAML.

* **DO** start every OpenRewrite recipe package with `org.openrewrite.<LANGUAGE>`.
* **DO** place every general-purpose search recipe in `org.openrewrite.<LANGUAGE>.search`.
* **DO** place every general-purpose security recipe in `org.openrewrite.<LANGUAGE>.security`.
* **DO** place every general-purpose formatting recipe in `org.openrewrite.<LANGUAGE>.format`.
* **DO** place every general-purpose cleanup recipe in `org.openrewrite.<LANGUAGE>.cleanup`. Note that in IDEs, "Code cleanup" is typically a separate action from "Format code", which is why the two hygiene-related tasks of formatting and cleanup are in distinct packages.

### Display names

* **DO** provide a short name ideally consisting of less than 5 words, but definitely less than 10 words.
* **DO** use sentence-casing (i.e. capitalize only the first word). For a more precise definition, see the C-level heading description in the [O'Reilly style guide](http://oreillymedia.github.io/production-resources/styleguide/#headings).
* **DO NOT** end with a period.
* **DO** use markdown backticks to delimit words representing code symbols, as in "JUnit4 `@RunWith` to JUnit Jupiter `@ExtendWith`".
* **DO** describe what the end result is positively whenever possible.

### Description text

* **DO** use at least one full sentence to describe the recipe, ending in a period.
* **DO** use markdown backticks (`) to delimit words representing code symbols.
* **DO NOT** reference alternatives in other static analyzers or products.

### Tagging

Tags are independent of one another. A recipe containing a tag will show up in any categorical listing by that tag.

* **DO** use all lower-case tag names, e.g. "security", "testing".
* **DO NOT** tag with language name.

## Consequences

Various user experiences listing OpenRewrite recipes are able to present a clean view of available recipes by relying strictly on the metadata provided by the recipe.

![image](https://user-images.githubusercontent.com/1697736/115929956-80745500-a43d-11eb-8d48-551a5ba74ee4.png)
