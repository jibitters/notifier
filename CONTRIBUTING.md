## Begin your Contribution
- Clone the repository :)
- Create a branch with a short and self explanatory name in your clone.
- Checkout your new branch and add commits with detailed and descriptive commit logs.
- When your are ready, first make sure that you're in sync with the upstream repository. Then 
  create a pull (merge) request from the repository page.
- Follow up with the automated and manual code review process to merge your request in the master.

## Caveats
If the master is updated after you have crated your pull-request, please rebase your branch with all the latest commits on 
top of master before submitting your pull-request again.
 
## General Guidelines
- Organize your codes in appropriate packages.
- Document your code thoroughly.
- Write lots and lots of tests.
- Test everything before creating the pull request.
- Pull changes from master before submitting your pull request.
- As a rule of thumb, **always** do a `rebase` unless someone else is simultaneously working on your branch. 

## Workflow Guidelines
 - Create branch based on the latest version of `master` with an appropriate name.
 - Develop!
 - When you think the branch is ready for a pull request, please rebase your branch with the latest `master`.
 - Create a pull request with appropriate title and description.
 - When a pull request resolves one specific issue, mention it using the famous `Resolves {task_id}` format.

## Documentation Guidelines
 - Document everything!
 - Learn how to write a good javadoc:
    - [Javadoc coding standards](https://blog.joda.org/2012/11/javadoc-coding-standards.html)
    - [How to Write Doc Comments for the Javadoc Tool](https://www.oracle.com/technetwork/java/javase/tech/index-137868.html)
 - Learn how to write good [Kotlin documentations](https://kotlinlang.org/docs/reference/kotlin-doc.html).

## Testing Guidelines
- Suppose you're gonna write a test for class `com.example.Foo`, then your unit test should be named `com.example.FooTest`
and your integration test should be named `com.example.FooIT`. Basically, test should be organized in the same package
as *Subject Under Test* and have `Test` and `IT` suffixes for unit and integration tests, respectively.
- Checkout JUnit 5's [documentation](https://junit.org/junit5/docs/current/user-guide/)
- Learn how to write integration tests for Spring:
  - [Spring Boot Test Support](https://bit.ly/2JEO9Ed)
  - [Spring Framework Test Support](https://bit.ly/2F1DUuL)
  - [Spring Security Test Support](https://bit.ly/2yLESpg)
- Checkout [XUnit Test Patterns](http://xunitpatterns.com)

## Kotlin and WebFlux Integration
Kotlin Coroutines are Kotlin lightweight threads allowing to write non-blocking code in an imperative way. On language 
side, suspending functions provides an abstraction for asynchronous operations while on library side kotlinx.coroutines 
provides functions like async { } and types like Flow. Read the [official doc](https://docs.spring.io/spring/docs/5.2.1.RELEASE/spring-framework-reference/languages.html#coroutines) for more detailed information.
