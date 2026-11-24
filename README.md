# QA Automation Tests

A small automation project with:

- **UI tests** for [Sauce Demo](https://www.saucedemo.com), using Selenide (Selenium) and JUnit 5
- **API tests** for [Restful Booker](https://restful-booker.herokuapp.com), using RestAssured and JUnit 5

## Project structure

```
src/test/java/
├── UITest.java     # UI scenarios + helper methods (login, sorting, cart)
└── APITest.java    # API scenario: create a booking and read it back
pom.xml             # dependencies and test configuration
mvnw, mvnw.cmd      # Maven Wrapper (no local Maven install needed)
Dockerfile          # image for running the tests without local Java/Maven
README.md
```

## Prerequisites

**Local execution**

- JDK 21
- Google Chrome (for the UI tests; Selenide downloads the matching driver itself)
- Maven is optional: the bundled Maven Wrapper (`./mvnw`) downloads it on first run

**Docker execution**

- Docker only. You don't need Java, Maven or Chrome on the host.

Both setups need internet access, because the tests run against public demo sites.

## Local execution

| What       | Command                        |
|------------|--------------------------------|
| UI tests   | `./mvnw test -Dtest=UITest`    |
| API tests  | `./mvnw test -Dtest=APITest`   |
| All tests  | `./mvnw test`                  |

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Useful options:

- `-Dselenide.headless=true` runs the browser without a window.
- `-Dselenide.browser=firefox` uses a different browser.
- `-Dselenide.timeout=<ms>` changes how long Selenide waits for conditions.

Reports are written to `target/surefire-reports/`. Selenide saves screenshots and page sources of failed UI tests in `build/reports/tests/`.

## Docker execution

Build the image:

```bash
docker build -t qa-automation-tests .
```

Run all tests:

```bash
docker run --rm qa-automation-tests
```

Run a single suite. Arguments after the image name are passed to Maven:

```bash
docker run --rm qa-automation-tests test -Dtest=UITest
docker run --rm qa-automation-tests test -Dtest=APITest
```

To keep the reports on the host, mount the output folders:

```bash
docker run --rm -v "$PWD/target:/app/target" -v "$PWD/build:/app/build" qa-automation-tests
```

The image is based on `maven:3.9-eclipse-temurin-21` with Google Chrome installed. Dependencies and test classes are resolved and compiled at build time. Chrome runs headless with `--no-sandbox` and `--disable-dev-shm-usage`, which it needs inside a container. The image is pinned to `linux/amd64` because Google Chrome has no Linux ARM build; on Apple Silicon it runs through emulation.

## Test approach

### Locators

The tests prefer the `data-test` attributes that Sauce Demo exposes, such as `[data-test='inventory-item']` and `[data-test='product-sort-container']`. These attributes exist only for testing, so they don't change when styling or layout changes. Where an element has a stable `id` or `name` (`#login-button`, `user-name`, `password`), that is used instead, because it is just as short and unique. A few CSS classes (`.inventory_item`, `.cart_item`) are used for simple presence checks. Text is used only where the text itself is being checked (the button label changing from "Add to cart" to "Remove").

### Synchronization

The tests use no `Thread.sleep`. Selenide's conditions retry automatically until they pass or time out (4 s by default). Examples: `shouldHave(sizeGreaterThan(0))`, `shouldHave(text("Remove"))` and `webdriver().shouldHave(urlContaining(...))`. After each navigation step, the test first waits for the expected URL (`shouldBeOnProductsPage()`, `shouldBeOnCartPage()`) and only then interacts with the page. Product lists are read only after they contain at least one element. One exception: the cart item count is read directly with `size()` (no retry). It is safe because the test has already waited for the cart page URL, but `shouldHave(size(1))` would be the stricter option.

The API calls are synchronous HTTP requests, so they need no extra synchronization.

### Code structure

The suite is small, so each area lives in a single class:

- **`UITest`:** the test methods read as scenario steps. Repeated actions are private helper methods: `login`, `applyOrderSorting`, `addAnyProductToCart`, `verifyPricesSortedAscending` and the `shouldBeOn...Page` checks. Sort options are an enum, so there are no magic strings in the tests.
- **`APITest`:** a RestAssured `given / when / then` chain. It creates a booking, takes the returned `bookingid`, then fetches the booking and checks every field.

Test data (credentials, API base URL, booking data) are constants at the top of each class, so they're easy to find and change. The Sauce Demo URL is set in the `@BeforeEach` method of `UITest`. If the suite grows, the next step would be page objects for the UI and a shared request specification or client for the API.

### Assumptions and limitations

- The tests run against public demo sites. Downtime, rate limiting or data changes there will fail the tests.
- Restful Booker returns `200 OK` (not `201 Created`) when a booking is created, so the test asserts 200.
- Restful Booker is shared and gets reset periodically. The test creates its own booking and reads it back right away, so it doesn't depend on existing data. It does not delete the booking afterwards.
- The demo credentials (`standard_user` / `secret_sauce`) are public, so it's fine to keep them in the code. A real project would inject secrets through environment variables or a CI secret store.
- The UI tests run in Chrome only; other browsers are possible through Selenide properties but have not been tested.
