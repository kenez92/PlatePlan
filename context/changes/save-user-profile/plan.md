# Save User Profile Implementation Plan

## Overview

A signed-in user keeps their whole profile on one screen, `/profile`: age, height, weight, sex, goal, activity level, preferred products, and excluded products. One form, one POST, one validation, one write. The data is stored in a new table `user_profile`, one row per account, found by the signed-in login. This is roadmap S-02 (US-01, FR-003) widened to all of FR-003. S-03 later adds the confirmed calorie number to the same table and the calculation on top of it. The profile never feeds a calorie number here, and the products never change calories at all; S-04 reads the products to build the diet.

The change was opened as `save-food-preferences` and renamed to `save-user-profile` during planning because the scope grew from the two product lists to the whole profile.

## Current State Analysis

- Sign-in and registration are done (S-01). `SecurityConfiguration` permits only `/`, `/register`, `/css/**`, `/error`, and the two Actuator endpoints, and ends with `anyRequest().authenticated()` (`src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:36-41`), so `/profile` is protected with no change to that file.
- The principal carries only the stored login, with the letter case the user typed, and no roles (`account/AccountPrincipalService.java:13-15`). `Principal.getName()` is therefore the one safe key for "own data only"; nothing from the request may choose whose row is read.
- `account` has a unique index on `upper(username)` and no plain unique constraint (`src/main/resources/db/changelog/changes/002-create-account.xml:10-27`), so a foreign key on the login is not possible in PostgreSQL. The user chose a `login` column as the primary key of `user_profile`, with no `id`, no second index, and no foreign key. Exact matching is enough: sign-in loads the account case-insensitively and the principal carries the login exactly as `account` stores it, and `account` already refuses two logins that differ only in case.
- The next free changelog file is `003-…`; `db.changelog-master.xml` includes every XML file in `changes/` alphabetically. Hibernate runs with `ddl-auto=none`, so the schema is Liquibase only.
- `RegistrationService` is the pattern for database work: it catches `DataIntegrityViolationException` and `DataAccessException` inside the method, logs only the exception class names, and returns a result object instead of throwing (`account/RegistrationService.java:42-56`). `application.properties` switches off `SqlExceptionHelper` logging because PostgreSQL puts the login in the error text (`src/main/resources/application.properties:16`). A primary-key violation on `login` has the same problem, so the same rule applies.
- `RegistrationValidator` is the pattern for text rules: a `@Component` returning an `Optional` of an error, refusing control, format, and non-ordinary-space characters, counting code points (`account/RegistrationValidator.java:27-50`). `LoginNormalizer` does strip and NFC (`account/LoginNormalizer.java:17-21`). `RegistrationForm` is the pattern for a form record that holds personal data: its `toString()` is redacted (`controller/RegistrationForm.java`).
- Views are server-rendered Thymeleaf in Polish, built from `fragments/chrome` (head, header, footer). Forms use `th:action`, which adds the CSRF token. The header gets the login from the model through `CurrentAccountAdvice` (`controller/CurrentAccountAdvice.java:18-20`); the Thymeleaf Security extras are not a dependency. The header `nav` is in `templates/fragments/chrome.html:11-20`.
- `RegisterControllerTest` is the pattern for a controller test: `@WebMvcTest` with the real `SecurityConfiguration` imported and the service replaced by a mock in a nested `@TestConfiguration` (`src/test/java/com/kenez92/plateplan/controller/RegisterControllerTest.java:39-44`, `189-211`). `.cursor/rules/testing.mdc` requires `should…` names, final variables and fields, constructor injection, no `private static` constants in tests, and whole-object comparison.
- Style: production constants are `private static final`, no string literal in a method body, every parameter `final`, every repository `@Repository` (`AGENTS.md`).
- The roadmap splits FR-003 between S-02 (products) and S-03 (body data). S-03 is blocked only on the size of the goal adjustment, which the calculation needs and the data entry does not. `context/foundation/lessons.md` does not exist; no lessons apply. No `calorie-formula` (F-03) change folder exists yet.

## Desired End State

A signed-in user opens `/profile` from a "Profil" link in the header and sees one form: age, height in cm, weight in kg, sex, goal, activity level, and two text fields for preferred and excluded products typed separated by `;`. "Zapisz profil" saves everything at once and returns to the screen with "Profil zapisany." If any field is wrong, nothing is saved, every wrong field shows its own message, and everything typed stays in the form. The profile survives logout and a new session, so the next visit shows it filled in (US-02). A signed-out visitor is sent to the login window. With the database down, the screen says so and never returns a 500. Verify with `.\gradlew.bat test` and the manual walk-through in Phase 2.

### Key Discoveries:

- Scope (decided): the whole of FR-003 on one screen, one form, one request. S-03 is reduced to the calorie calculation, the goal adjustment, and accept-or-edit, and gets the profile as its input.
- Body fields (decided): all six are required to save. Age is a whole number 10–110. Height is a whole number of centimetres 80–250. Weight is kilograms 20.0–400.0 with at most one decimal place; both `,` and `.` are accepted as the decimal mark, because Polish users type a comma. Sex is `MALE` or `FEMALE` (the two BMR lines of the formula). Goal is `LOSE_WEIGHT`, `MAINTAIN`, or `GAIN`. Activity is `SEDENTARY`, `LIGHT`, `MODERATE`, or `HIGH` (PRD, Business Logic). The wide ranges let a minor save a profile; the formula is for adults and S-03 may narrow them.
- Products (decided): each list may be empty. A name is 2–50 code points after normalization (strip, NFC, runs of ordinary spaces collapsed to one) with no control, format, or non-ordinary-space character and no `;`. A comma is allowed. The typed letter case is stored. Two names are the same when equal after lower-casing with `Locale.ROOT`. A repeat inside one list, a product on both lists, and more than 50 products on a list are refused, never merged or moved.
- Separator (decided): `;` in the input fields and in the database, so a comma can be part of a name ("mleko 3,2%"). A name containing `;` cannot be typed, and the validator refuses it if it arrives any other way.
- Save is all-or-nothing (decided): every field is checked, all problems are reported together, at most one per field, and nothing is written unless there are none. The check order inside a product list is invalid name, repeat, list full; a product on both lists is reported on the excluded field, last, and only if that field has no other problem.
- Row creation (derived): every column is `not null`, and the row is created by the first successful save, so an account with no row reads as an empty form. `RegistrationService` stays untouched.
- Concurrency (decided): last write wins, no version column. A simultaneous first save from two tabs can hit the unique index; the user sees the retry message.
- No `@Transactional` on the service (derived): the exceptions must be caught inside the method, and a commit-time failure would escape a transactional proxy. `repository.save` has its own transaction.
- Names (decided): table `user_profile`, entity `UserProfile`, package `profile`, screen `/profile`, link "Profil", controller `ProfileController`, service `ProfileService`. The product-list helpers keep the `Product…` names.
- Enum ownership (derived): `Sex`, `Goal`, and `ActivityLevel` are created here as plain enums with names only. F-03 needs the same sex and activity values for the BMR formula and should add its multipliers beside them or map from them, not define a second set.

## What We're NOT Doing

- No calorie calculation, no goal-adjustment size, no confirmed-calories column or field. S-03 adds `confirmed_calories` to `user_profile` with its own changeSet.
- No per-product add and remove buttons. The two lists are edited as text in the same form.
- No foreign key to `account` and no `account_id`; the primary key is the `login` column, and there is no `id` and no second index.
- No redirect to `/profile` after registration; registration still lands on `/`.
- No JavaScript, no new library, no optimistic locking, no sorting, no diacritic folding ("ser żółty" and "ser zolty" are different products).
- No use of the products in a prompt (S-04).
- No logging of logins, field values, product names, or exception messages from the profile code.
- No change to `SecurityConfiguration`, `RegistrationService`, or `002-create-account.xml`.

## Implementation Approach

Three phases, each ending with a green `.\gradlew.bat test`. Phase 1 is storage and rules with no screen, fully covered by unit tests. Phase 2 is the screen on top of it. Phase 3 brings `AGENTS.md` in line. New classes go in a new package `com.kenez92.plateplan.profile`, the controller stays in `controller`, matching the existing split. Formatting follows `config/LiquibaseConfiguration.java`: four spaces, IntelliJ style, `final` parameters, no blank line before a closing brace. Tests follow `.cursor/rules/testing.mdc`.

## Critical Implementation Details

- **Timing & lifecycle** — The service queries the database only when a request arrives, never at start, so the application still starts with an unreachable database. `GET /profile` and `POST /profile` must answer with status 200 and a message when the database fails, not a 500.
- **State sequencing** — Parse and validate everything first, then write once. A refused save must leave the stored row exactly as it was, including not creating it. The login for every read and write comes from `Principal.getName()`; a `login` request parameter must be ignored.
- **Debug & observability** — Log only `exception.getClass().getName()` and the cause's class name, like `RegistrationService`. A unique-index violation message contains the login, and age, height, weight, sex, goal, and products are personal data; none may reach a log or a response outside the signed-in owner's own form. `ProfileInput.toString()` is redacted.

## Phase 1: Storage and rules

### Overview

Add the table, the entity, the repository, and a service that parses, validates, loads, and saves the whole profile. After this phase a profile can be saved and read from code and is covered by unit tests; there is no screen yet.

### Changes Required:

#### 1. Migration

**File**: `src/main/resources/db/changelog/changes/003-create-user-profile.xml` (new)

**Intent**: Create the profile row of an account, using Liquibase change types and the same shape as `002-create-account.xml`.

**Contract**: one changeSet `003-create-user-profile`, author `plateplan`. Table `user_profile`, every column `not null`: `login varchar(50)` primary key; `age integer`; `height_cm integer`; `weight_kg numeric(4,1)`; `sex varchar(10)`; `goal varchar(20)`; `activity_level varchar(20)`; `preferred_products text`; `excluded_products text`. An empty list is stored as an empty string. No separate index: the primary key serves the lookup. No raw `<sql>`, no foreign key, no default row. A comment says that S-03 adds `confirmed_calories` in a new file.

#### 2. Entity and repository

**Files**: `src/main/java/com/kenez92/plateplan/profile/UserProfile.java`, `UserProfileRepository.java` (new)

**Intent**: Map the table without any rule. The entity holds the raw stored values, and the two product columns are the stored text.

**Contract**:
- `@Entity @Table(name = "user_profile") UserProfile` with `@Id String login`, `int age`, `int heightCm`, `BigDecimal weightKg`, `Sex sex`, `Goal goal`, `ActivityLevel activityLevel`, `String preferredProducts`, `String excludedProducts`; the three enums are stored as their names (`@Enumerated(EnumType.STRING)`); protected no-argument constructor; one public constructor taking the login and all values; getters; and one method that replaces every value but the login on a loaded row.
- `@Repository interface UserProfileRepository extends JpaRepository<UserProfile, String>`; the row is read with `findById(login)`, the login exactly as `Principal.getName()` returns it.

#### 3. Vocabulary and product text rules

**Files**: `profile/Sex.java`, `Goal.java`, `ActivityLevel.java`, `ProductLists.java`, `ProductListFormat.java`, `ProductNameValidator.java` (new)

**Intent**: Keep the vocabulary and the text rules out of the service so each can be tested alone.

**Contract**:
- `enum Sex { MALE, FEMALE }`, `enum Goal { LOSE_WEIGHT, MAINTAIN, GAIN }`, `enum ActivityLevel { SEDENTARY, LIGHT, MODERATE, HIGH }`; names only, no multipliers.
- `record ProductLists(List<String> preferred, List<String> excluded)`; both lists are immutable copies.
- `@Component ProductListFormat`: `List<String> split(final String text)` splits on `;`, normalizes each part (strip, NFC, runs of ordinary spaces to one), and drops empty parts; a `null` text is empty. `String join(final List<String> products)` joins with `;` and no spaces (the stored form). `String display(final List<String> products)` joins with `; ` (the form field). Stored text and typed text share `split`, so they cannot disagree.
- `@Component ProductNameValidator`: `boolean isValid(final String name)` is true for 2–50 code points with no control, format, or non-ordinary-space character and no `;`.

#### 4. Input, parsing, and result types

**Files**: `profile/ProfileInput.java`, `ProfileDetails.java`, `ProfileField.java`, `ProfileError.java`, `ProfileProblem.java`, `ProfileParser.java`, `ProfileResult.java` (new)

**Intent**: Turn what the browser sent into a valid profile, or into the list of what is wrong, without touching the database. Expected outcomes are values, as `RegistrationResult` is.

**Contract**:
- `record ProfileInput(String age, String heightCm, String weightKg, String sex, String goal, String activityLevel, String preferredProducts, String excludedProducts)`: every field exactly as typed (or as displayed); a field the browser did not send is `null`. `toString()` is redacted, like `RegistrationForm`.
- `record ProfileDetails(int age, int heightCm, BigDecimal weightKg, Sex sex, Goal goal, ActivityLevel activityLevel, ProductLists products)`: the valid profile.
- `enum ProfileField { AGE, HEIGHT, WEIGHT, SEX, GOAL, ACTIVITY, PREFERRED, EXCLUDED }`; `enum ProfileError { REQUIRED, INVALID_VALUE, PRODUCT_INVALID, PRODUCT_DUPLICATE, PRODUCT_CONFLICT, LIST_FULL }`; `record ProfileProblem(ProfileField field, ProfileError error, List<String> products)` where `products` holds the offending names as typed and is empty for the body fields.
- `@Component ProfileParser(final ProductListFormat, final ProductNameValidator)` with `ProfileParseResult parse(final ProfileInput input)`, where `ProfileParseResult(ProfileDetails details, List<ProfileProblem> problems)` holds the details or the problems, never both. Rules as in Key Discoveries: a blank or missing body field is `REQUIRED`; a wrong format or an out-of-range number or an unknown enum name is `INVALID_VALUE`; products as above. All problems are returned together, at most one per field, in `ProfileField` order.
- `record ProfileResult(ProfileInput profile, List<ProfileProblem> problems, boolean unavailable)`: the profile to show (stored values after a load or a successful save, the typed values after a refusal or a database failure), the problems, and whether the database failed. Factories `loaded`, `saved`, `rejected`, `unavailable`, and an `isSaved()`-style reader.

#### 5. Service

**File**: `profile/ProfileService.java` (new)

**Intent**: The only place that reads or writes the table.

**Contract**: `@Service ProfileService(final UserProfileRepository, final ProfileParser, final ProductListFormat)` with:
- `ProfileResult load(final String login)` — no row is an input with every field empty; a stored row becomes a `ProfileInput` (weight with one decimal and a dot, products through `display`).
- `ProfileResult save(final String login, final ProfileInput input)` — parse; on problems return `rejected` and write nothing; otherwise find the row by `findById`, replace its values (or create it), save once, and return `saved` with the stored values.
- A `DataIntegrityViolationException` or any `DataAccessException`, on the read or the save, returns `unavailable` carrying the typed input. Only class names are logged.

#### 6. Tests

**Files**: `src/test/java/com/kenez92/plateplan/profile/ProductListFormatTest.java`, `ProductNameValidatorTest.java`, `ProfileInputTest.java`, `ProfileParserTest.java`, `ProfileServiceTest.java` (new)

**Intent**: Prove each rule and decided term with the smallest case.

**Contract**: unit tests, no Spring; `@ExtendWith(MockitoExtension.class)` only where a mock is used; results compared whole with `usingRecursiveComparison()`.
- `ProductListFormatTest`: `shouldSplitOnSemicolonsAndKeepCommasInsideAName`, `shouldIgnoreEmptyPartsFromADoubleOrTrailingSemicolon`, `shouldStripComposeAndCollapseSpaces`, `shouldTreatNullAsEmpty`, `shouldJoinWithSemicolonsAndNoSpaces`, `shouldDisplayWithASemicolonAndASpace`, `shouldReturnTheSameListAfterJoinAndSplit`.
- `ProductNameValidatorTest`: `shouldAcceptTwoCharacters`, `shouldRejectOneCharacter`, `shouldAcceptFiftyCharactersAndRejectFiftyOne`, `shouldRejectASemicolon`, `shouldAcceptAComma`, `shouldRejectControlFormatAndNonOrdinarySpaceCharacters`.
- `ProfileInputTest`: `shouldNotShowAnyValueInToString`.
- `ProfileParserTest`: `shouldParseAValidProfile`, `shouldAcceptACommaOrADotAsTheDecimalMark`, `shouldRequireEveryBodyField` (each blank or `null` alone), `shouldAcceptAgeFrom10To110AndRefuseTheNeighbours` (9, 10, 110, 111), `shouldRefuseAnAgeThatIsNotAWholeNumber` ("30.5", "abc"), the same two for height (79, 80, 250, 251) and weight (19.9, 20.0, 400.0, 400.1, "72.55", "1e2"), `shouldRefuseAnUnknownSexGoalOrActivity`, `shouldAcceptEmptyProductLists`, `shouldRefuseAnInvalidProductName`, `shouldRefuseARepeatInsideOneListIgnoringCase` ("Mleko; mleko"), `shouldAcceptFiftyProductsAndRefuseTheFiftyFirst`, `shouldRefuseAProductOnBothListsAndReportItOnTheExcludedField`, `shouldReportEveryProblemTogetherAtMostOnePerField`.
- `ProfileServiceTest` (mocked repository): `shouldReturnAnEmptyInputWhenTheAccountHasNoRow`, `shouldReturnTheStoredValuesInTheirFormFormat`, `shouldCreateTheRowOnTheFirstSave`, `shouldReplaceEveryValueOfTheExistingRowOnSave`, `shouldLookTheRowUpByTheExactLogin`, `shouldKeepTheTypedCaseOfProducts`, `shouldSaveNothingWhenAnyFieldIsRefused`, `shouldNotCreateARowWhenTheSaveIsRefused`, `shouldReportUnavailableWhenTheReadFails`, `shouldReportUnavailableWhenTheSaveFails` (and carry the typed input), `shouldReportUnavailableOnAUniqueIndexViolation`, `shouldLogOnlyTheClassNamesWhenTheDatabaseFails` (a log message holds neither the login nor any value nor a product name).

### Success Criteria:

#### Automated Verification:

- Suite passes with the new rules and tests: `.\gradlew.bat test`
- The full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- The migration uses Liquibase change types only: `rg -n "<sql" src/main/resources/db/changelog/changes/003-create-user-profile.xml` finds nothing
- Logging stays free of personal data: `rg -n "LOGGER\." src/main/java/com/kenez92/plateplan/profile` matches only `ProfileService`, and those calls pass only class names

#### Manual Verification:

- With a reachable PostgreSQL (the development database or a local one) and `SESSION_COOKIE_SECURE=false`, `.\gradlew.bat bootRun` logs that changeSet `003-create-user-profile` ran, and `\d user_profile` shows the nine columns, all `not null`, with `login` as the primary key
- Starting again does not run the changeSet a second time

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: The profile screen

### Overview

Add `GET /profile` and `POST /profile`, the Polish view, the header link, and the styles. After this phase a signed-in user can fill in, save, and reload the whole profile in the browser.

### Changes Required:

#### 1. Controller

**File**: `src/main/java/com/kenez92/plateplan/controller/ProfileController.java` (new)

**Intent**: Route only. Read the login from the session, call the service, and turn the result into a view or a redirect. No rule lives here.

**Contract**: `@Controller @RequestMapping("/profile")`, constructor takes `final ProfileService`.
- `GET /profile` renders view `profile` with the loaded `ProfileInput` as the form. If the service reports unavailable, the model says the profile could not be loaded and the view shows no form.
- `POST /profile` binds a `ProfileInput` (`@ModelAttribute`), calls `save` with `Principal.getName()`, and no request parameter selects an account. Saved: redirect to `/profile` with a flash attribute for the notice. Refused: render `profile` with status 200, the typed input, and the problems as a map from the field name to its problem (one per field), so the view can show each message by its field. Database failure: render `profile` with status 200, the typed input, and the retry message.
- Constants (view name, attribute names, path) are `private static final`; no string literal in a method body.

#### 2. View

**File**: `src/main/resources/templates/profile.html` (new)

**Intent**: One screen, one form, Polish text, no JavaScript, consistent with `register.html` and the shared chrome.

**Contract**:
- Uses `fragments/chrome` head, header, and footer; page title "Profil · PlatePlan". One `form` with `th:action="@{/profile}"`, method post, and a single submit "Zapisz profil".
- Fields, each with a visible `label`: "Wiek" (years), "Wzrost (cm)", "Waga (kg)", "Płeć" (select: Mężczyzna, Kobieta), "Cel" (select: Schudnąć, Utrzymać wagę, Przytyć), "Poziom aktywności" (select: "Siedzący tryb życia", "Lekka aktywność", "Umiarkowana aktywność", "Wysoka aktywność", each with the PRD wording as a short description), "Produkty preferowane" and "Produkty wykluczone" (text fields, `maxlength` 2600, hint: "Rozdzielaj produkty średnikiem. Przecinek może być częścią nazwy."). Selects start with an empty "Wybierz" option. Every value is refilled from the form object.
- Every value and every product name uses `th:text`, `th:value`, or `th:selected`, never `th:utext`, so they are escaped.
- A field's message appears next to it with `role="alert"`; the notice "Profil zapisany." uses `role="status"`. Messages: required ("Uzupełnij to pole."), age ("Wiek: liczba całkowita od 10 do 110."), height ("Wzrost: liczba całkowita od 80 do 250 cm."), weight ("Waga: od 20 do 400 kg, najwyżej jedno miejsce po przecinku."), choice ("Wybierz jedną z dostępnych opcji."), invalid product name ("Nazwa produktu musi mieć od 2 do 50 znaków i nie może zawierać średnika ani znaków sterujących:" plus the names), repeat ("Produkt powtarza się na liście:" plus the names), list full ("Lista może mieć najwyżej 50 produktów."), conflict ("Ten produkt jest też na liście preferowanych:" plus the names), save failure ("Nie udało się zapisać profilu. Spróbuj ponownie za chwilę."), load failure ("Nie udało się wczytać profilu. Spróbuj ponownie za chwilę.").
- A short lead says that the body data and the goal are used to calculate calories and that the products only shape the plan and never the calories.

#### 3. Header link

**File**: `src/main/resources/templates/fragments/chrome.html`

**Intent**: Make the screen reachable from every page for a signed-in visitor only.

**Contract**: add a "Profil" link to `/profile` in the header `nav`, rendered only when `currentLogin != null`.

#### 4. Styles

**File**: `src/main/resources/static/css/site.css`

**Intent**: Style the new form with the existing tokens and components (`.panel`, `.field`, `.button`, `.form-error`, the focus rules).

**Contract**: a profile form layout (two columns for the numeric fields on wide screens, one column under 800 px like `.split`), `select` and long text fields styled like `input` with a visible focus, a field-level message style, and `.form-notice` with sufficient contrast. Note that the existing `input` rule sets a fixed width; the profile fields must fill their column.

#### 5. Tests

**Files**: `src/test/java/com/kenez92/plateplan/controller/ProfileControllerTest.java` (new), `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java` (extend)

**Intent**: Prove the screen on the real filter chain and prove that the data is only the signed-in user's.

**Contract**:
- `ProfileControllerTest`: `@WebMvcTest(ProfileController.class)` with `SecurityConfiguration` imported and a nested `@TestConfiguration` that mocks `ProfileService`; signed-in tests use `@WithMockUser(username = "alice")`. Methods: `shouldShowAnEmptyFormWhenTheAccountHasNoProfile`, `shouldShowTheStoredProfile` (values and the selected options), `shouldEscapeEveryStoredAndTypedValue`, `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` (a `login=bob` parameter is ignored; the service is called with `alice`), `shouldRedirectWithASavedNoticeAfterASave`, `shouldShowTheFormAgainWithTheTypedValuesWhenTheSaveIsRefused`, `shouldShowTheMessageForEachProblemNextToItsField`, `shouldKeepTheTypedValuesWhenTheDatabaseFailsOnSave`, `shouldHideTheFormWhenTheProfileCannotBeLoaded`, `shouldRejectAPostWithoutACsrfToken`, `shouldShowTheProfileLinkInTheHeaderOfASignedInVisitor`. HTML stays a string assertion; results from the service stub are whole objects.
- `SecurityConfigurationTest`: `shouldRedirectASignedOutVisitorFromTheProfileToTheLoginWindow` (GET `/profile` is 302 to `/`) and `shouldRedirectASignedOutPostToTheProfileToTheLoginWindow` (with a CSRF token, 302 to `/`).

### Success Criteria:

#### Automated Verification:

- Suite passes with the controller, view, and tests: `.\gradlew.bat test`
- The full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- No unescaped output in the new view: `rg -n "th:utext" src/main/resources/templates` finds nothing
- No literal in a controller method body that should be a constant: reviewed against `RegisterController` (view name, attributes, path are `private static final`)

#### Manual Verification:

- Signed in against a real database: the "Profil" link appears in the header and not when signed out; a signed-out request to `/profile` lands on the login window
- Fill in age 34, height 180, weight "82,5", Mężczyzna, Utrzymać wagę, Umiarkowana aktywność, preferred "mleko 3,2%; jajka; ser", excluded "orzechy": after "Zapisz profil" the screen shows "Profil zapisany." and every value, and weight shows 82.5
- Log out and in again: the profile is still filled in, in the same order
- Save with an empty field, an age of 9, a weight of "72,55", and "Jajka; jajka" in the preferred field: nothing is saved, each wrong field shows its own message, and everything typed is still in the form
- Put "ser" in both lists: refused with the conflict message on the excluded field
- Leave both product fields empty and save: accepted
- Register a second account: its form is empty and it never shows the first account's values
- Stop the database (or point `DATABASE_URL` at an unreachable host) and open `/profile`: the page says it could not load and shows no form, no error page; saving while it is down keeps the typed values and shows the retry message
- Keyboard only: every field and button can be reached and has a visible focus; the layout holds at 375 px

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase.

---

## Phase 3: Documentation

### Overview

Bring the repository guide in line with what now exists. The roadmap was already updated during planning to the new scope and name.

### Changes Required:

#### 1. Repository guide

**File**: `AGENTS.md`

**Intent**: A future agent must know about the new migration, the screen, and the rules without reading the plan.

**Contract**: add `changes/003-create-user-profile.xml` to the list of changes so far; add a short paragraph on the profile next to "Account flow": the table `user_profile` keyed by the `login` column, the screen `/profile` and its single form, the required body fields and their ranges, the `;` separator and the product-name rules, all-or-nothing save, the limit of 50 products per list, last write wins, and that S-03 adds `confirmed_calories` to this table. Keep the file's existing style and do not reformat unrelated text.

### Success Criteria:

#### Automated Verification:

- The guide names the new table and the screen: `rg -n "user_profile|/profile" AGENTS.md` matches
- Suite still passes: `.\gradlew.bat test`

#### Manual Verification:

- `AGENTS.md` reads correctly next to the account paragraph and states no rule the code does not follow
- No document in `context/foundation/` still calls this change `save-food-preferences` or the table `user_preferences`: `rg -n "save-food-preferences|user_preferences" context/foundation AGENTS.md` finds nothing

**Implementation Note**: After completing this phase, pause for manual confirmation before archiving the change.

---

## Testing Strategy

### Unit Tests:

- Text rules: split, normalize, join, display, product-name validation (semicolon refused, comma allowed, 2–50 code points).
- Parser rules: required fields, the boundary of each range, the decimal mark, enum names, product repeats, conflicts, the 50 limit, all problems together and at most one per field.
- Service rules: lazy row creation, replace on save, lookup by login ignoring case, refusal writes nothing, database failure paths, log content.

### Integration Tests:

- `@WebMvcTest` slice with the real security chain: signed-out redirect, CSRF, own-login-only, each message next to its field, escaping, typed values kept, form hidden on load failure.
- `ApplicationTest.shouldLoadContext` stays the one test that boots the application.

### Manual Testing Steps:

1. Register, open Profil, fill in and save as in the Phase 2 list.
2. Log out and in again and confirm the profile persists.
3. Try each wrong value and confirm nothing is saved and the typed values stay.
4. Register a second account and confirm isolation.
5. Break the database connection and confirm the message instead of an error page.

## Performance Considerations

Each request does one read, and a save does one more write, of one short row. The product lists are capped at 50 names of 50 characters, so the row stays small. The primary key on `login` serves the lookup.

## Migration Notes

The changeSet only creates a new table; no data moves and no existing changeSet is edited. Rollback is dropping `user_profile`, which holds only user-entered data. Existing accounts have no row and read as an empty form. S-03 adds `confirmed_calories` as a nullable column in a new changeSet, because a saved profile may exist before any number is confirmed.

## References

- Roadmap: `context/foundation/roadmap.md` (S-02, S-03), PRD: `context/foundation/prd.md` (FR-003, Business Logic, Access Control)
- Database pattern: `src/main/java/com/kenez92/plateplan/account/RegistrationService.java:42-56`, `src/main/resources/db/changelog/changes/002-create-account.xml:10-27`
- Text rules and redacted form pattern: `src/main/java/com/kenez92/plateplan/account/RegistrationValidator.java:27-50`, `LoginNormalizer.java:17-21`, `controller/RegistrationForm.java`
- Controller test pattern: `src/test/java/com/kenez92/plateplan/controller/RegisterControllerTest.java:39-44`
- Archived plan of the same shape: `context/archive/2026-10-02-register-and-sign-in/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Storage and rules

#### Automated

- [x] 1.1 Suite passes with the new rules and tests: `.\gradlew.bat test` — f730733
- [x] 1.2 The full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — f730733
- [x] 1.3 The migration uses Liquibase change types only: `rg -n "<sql" src/main/resources/db/changelog/changes/003-create-user-profile.xml` finds nothing — f730733
- [x] 1.4 Logging stays free of personal data: `rg -n "LOGGER\." src/main/java/com/kenez92/plateplan/profile` matches only `ProfileService`, and those calls pass only class names — f730733

#### Manual

- [ ] 1.5 With a reachable PostgreSQL and `SESSION_COOKIE_SECURE=false`, `.\gradlew.bat bootRun` logs that changeSet `003-create-user-profile` ran, and `\d user_profile` shows the nine columns, all `not null`, with `login` as the primary key
- [ ] 1.6 Starting again does not run the changeSet a second time

### Phase 2: The profile screen

#### Automated

- [ ] 2.1 Suite passes with the controller, view, and tests: `.\gradlew.bat test`
- [ ] 2.2 The full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- [ ] 2.3 No unescaped output in the new view: `rg -n "th:utext" src/main/resources/templates` finds nothing
- [ ] 2.4 No literal in a controller method body that should be a constant: reviewed against `RegisterController`

#### Manual

- [ ] 2.5 The "Profil" link appears in the header when signed in and not when signed out; a signed-out request to `/profile` lands on the login window
- [ ] 2.6 Saving age 34, height 180, weight "82,5", Mężczyzna, Utrzymać wagę, Umiarkowana aktywność, preferred "mleko 3,2%; jajka; ser", and excluded "orzechy" shows "Profil zapisany." with every value, and the weight as 82.5
- [ ] 2.7 After logging out and in again the profile is still filled in, in the same order
- [ ] 2.8 Saving with an empty field, an age of 9, a weight of "72,55", and "Jajka; jajka" saves nothing, shows each message next to its field, and keeps everything typed
- [ ] 2.9 "ser" in both lists is refused with the conflict message on the excluded field
- [ ] 2.10 Leaving both product fields empty is accepted
- [ ] 2.11 A second registered account has an empty form and never shows the first account's values
- [ ] 2.12 With the database unreachable, `/profile` says it could not load and shows no form and no error page, and saving keeps the typed values with the retry message
- [ ] 2.13 Keyboard-only use works with a visible focus, and the layout holds at 375 px

### Phase 3: Documentation

#### Automated

- [ ] 3.1 The guide names the new table and the screen: `rg -n "user_profile|/profile" AGENTS.md` matches
- [ ] 3.2 Suite still passes: `.\gradlew.bat test`

#### Manual

- [ ] 3.3 `AGENTS.md` reads correctly next to the account paragraph and states no rule the code does not follow
- [ ] 3.4 No document in `context/foundation/` still calls this change `save-food-preferences` or the table `user_preferences`: `rg -n "save-food-preferences|user_preferences" context/foundation AGENTS.md` finds nothing
