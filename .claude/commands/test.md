---
description: Generate tests for the specified class, method, or module. Usage: /test [file-or-class-name]
---

Generate comprehensive tests for `$ARGUMENTS`. Read the target file first, identify the framework in use, then write tests following the project's existing test conventions.

## Discovery Steps
1. Read target file to understand public API and dependencies
2. Detect test framework from `pom.xml`/`build.gradle`/`requirements.txt`
3. Find an existing test file for the same module to match conventions
4. Identify all code paths: happy path, edge cases, failure paths

## Coverage Requirements

| Path type | Must cover |
|-----------|-----------|
| Happy path | All documented successful flows |
| Boundary | Min/max values, empty collections, zero, null optionals |
| Error path | Each exception type the method can throw or return |
| Concurrency | If method mutates shared state |
| Integration | DB/network calls (use Testcontainers or `local[*]` Spark) |

## Java Test Template (JUnit 5 + Mockito)
```java
@ExtendWith(MockitoExtension.class)
class [ClassName]Test {
    @Mock [DependencyType] dep;
    @InjectMocks [ClassName] sut;

    // naming: methodName_expectedBehavior_whenCondition
    @Test void processOrder_returnsConfirmed_whenAmountValid() { ... }
    @Test void processOrder_throwsValidation_whenAmountNegative() { ... }
    @Test void processOrder_retriesThrice_whenDbTransient() { ... }
}
```

## Reactive Test Template (Mutiny)
```java
var sub = sut.process(input)
    .subscribe().withSubscriber(UniAssertSubscriber.create());
sub.awaitItem().assertItem(expected);
// failure path:
sub.awaitFailure().assertFailedWith(ValidationException.class);
```

## Python Test Template (pytest)
```python
class TestClassName:
    def test_method_returns_x_when_y(self, fixture):
        result = sut.method(valid_input)
        assert result == expected

    def test_method_raises_when_invalid(self):
        with pytest.raises(ValueError, match="amount must be"):
            sut.method(invalid_input)
```

## PySpark Test Template
```python
@pytest.fixture(scope="session")
def spark():
    return SparkSession.builder.master("local[2]").appName("test").getOrCreate()

def test_transform_flags_high_value(spark):
    df = spark.createDataFrame([(1, 15000.0)], ["id", "amount"])
    result = transform(df)
    assert result.filter(col("flag") == "HIGH").count() == 1
```

## Output Rules
- One test class per source class
- Test method names: `method_expected_whenCondition` (Java) / `test_method_expected_when_condition` (Python)
- No mocking of value objects — use real instances
- No `Thread.sleep()` — use `await` / `StepVerifier.withVirtualTime`
- Every test must have a clear Arrange / Act / Assert structure (comment only if not obvious)
- Include `@DisplayName` annotations for Java if test name alone is insufficient
