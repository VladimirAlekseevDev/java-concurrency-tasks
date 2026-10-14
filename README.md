# Java Concurrency Tasks

Small Java concurrency challenges. Each Gradle module contains one task, its solution, and tests.

## Stack

- Java 25
- Gradle 9.8.0 (Wrapper included)
- JUnit Jupiter and Mockito for testing (versions in `gradle.properties`)

Solutions use only the Java standard library.

## Structure

```text
├── build.gradle          # Shared Java and test configuration
├── settings.gradle       # Module registration
└── task-**/
    ├── README.md         # Task description
    ├── build.gradle
    └── src/
        ├── main/java/    # Solution
        └── test/java/    # Tests
```

## Run

```powershell
.\gradlew.bat build          # Build and test all modules
.\gradlew.bat test           # Run all tests
.\gradlew.bat :task-01:test  # Run one module's tests
```

On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat` (run `chmod +x gradlew` if needed).

Test reports: `<module>/build/reports/tests/test/index.html`.
