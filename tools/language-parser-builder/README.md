## Language Parser Builder

This project is a tool to generate an OpenRewrite lossless semantic tree (LST) model for a language. The LST model is the form that is suitable for search, transformation, and even serialization.

This project is a sort of template meant to be modified while generating an LST model and then reverted back to its current state once the new language binding is complete.

### To use

Replace all occurrences of `Toml` with the language you intend to build a parser for. Rename the packages ending in `toml`, file names containing `Toml`, and references in the code referring to `Toml`.

Update the `Assertions` class with the correct `@Language` annotation for your language (if there is one, otherwise remove them altogether), and update the method names to your language name.

The class `src/model/Toml` is meant to help you write a simplified version of the LST model. `GenerateModel` takes this simplified form and writes out the full LST model and fills out basic visitor navigation and printing methods. The generated methods in `TomlPrinter` (which will of course be renamed to the language you are implementing) are meant as a starting point. There will be further printing modification necessary, but the generator takes care of a lot of the repetitive work.

You can proceed incrementally, adding model objects and running the generator, implemeting their print methods, etc. Or you can write out the entire model and generate once. The generator will not overwrite any existing methods, so iterative generation does not clobber your work.

### Completing the language parser

A partial series of steps needed to complete the language:

* Create a new directory `rewrite-LANGUAGE`
* Add your new language to the list of `allProjects` in `settings.gradle.kts`
* Add your new language to `IDE.properties.tmp`.
