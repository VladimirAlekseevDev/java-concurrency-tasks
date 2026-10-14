# Java Concurrency Tasks

Small Java concurrency challenges. Each Gradle module contains one task, its solution, and tests.

## Stack

- Java 25 LTS
- Gradle 9.7.1 (Wrapper included)
- JUnit Jupiter 6.1.3 and Mockito 5.24.0 for testing

Solutions use only the Java standard library.

## Structure

```text
├── build.gradle          # Shared Java and test configuration
├── settings.gradle       # Module registration
└── task-01/
    ├── README.md         # Task description
    ├── build.gradle
    └── src/
        ├── main/java/    # Solution
        └── test/java/    # Tests
```

## Run

Install JDK 25 and set `JAVA_HOME` to its installation directory. The Gradle Wrapper downloads Gradle automatically.

```powershell
.\gradlew.bat build          # Build and test all modules
.\gradlew.bat test           # Run all tests
.\gradlew.bat :task-01:test   # Run one module's tests
```

On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat` (run `chmod +x gradlew` if needed).

Test reports: `<module>/build/reports/tests/test/index.html`.
