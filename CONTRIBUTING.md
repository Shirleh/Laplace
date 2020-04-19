# Contributing to Laplace

First off, thanks for taking the time to contribute! :heart:

The following is a set of guidelines for contributing to Laplace. These are mostly guidelines, not rules. 
Use your best judgment, and feel free to propose changes to this document in a pull request!

#### Table Of Contents

[Code of Conduct](#code-of-conduct)  
[How Can I Contribute?](#how-can-i-contribute)

  * [Reporting bugs](#reporting-bugs)
  * [Suggesting features or other improvements](#suggesting-features-or-other-improvements)
  * [Contributing code](#contributing-code)

[Style guides](#style-guides)

  * [Kotlin style guide](#kotlin-style-guide)
  * [Git style guide](#git-style-guide)

## Code of conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). 
By participating, you are expected to uphold this code.

## How can I contribute?
If you wish to contribute changes not covered by the issue tracker, please discuss it with me personally or, ideally, 
through the issue tracker.

### Reporting bugs

- Please ensure it was not already reported.
- Open a new issue and fill out the "Bug" template.

### Suggesting features or other improvements

- Please ensure it was not already suggested.
- Open a new issue and fill out the appropriate template.

### Contributing code

#### Setting up your local development environment

- Please see the [installation section](README.md#installation) on how to install the project.
- It's highly recommended to use [IntelliJ IDEA](https://www.jetbrains.com/idea/download/index.html#section=windows) 
as your IDE for Kotlin projects.

#### Submitting changes

- Pick an issue from the issue tracker you want to work on.
- Create a feature branch from `master` in the format `feature/short-description/issue-number`, e.g.  
```git checkout -b feature/implement-fluffy-feature/#69```
- Ensure all tests pass with `./gradlew check`
- Make your changes.
- Include tests if applicable.
- Ensure all tests pass with `./gradlew check`.
- Submit a pull request!

## Style guides

### Kotlin style guide

IntelliJ IDEA should already pick up the [official Kotlin style rules](https://kotlinlang.org/docs/reference/coding-conventions.html) 
from `gradle.properties` and spacing from `.editorconfig`. Use `CTRL+ALT+L`/`⌥⌘L` and IntelliJ will auto-format your code according to these guidelines!

### Git style guide

For commit messages,
 
* use the present tense ("Add a feature" not "Added a feature")
* use the imperative mood ("Move cursor to..." not "Moves cursor to...")
