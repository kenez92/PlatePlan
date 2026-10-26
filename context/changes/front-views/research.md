---
date: 2026-10-26T20:09:46+01:00
researcher: unknown
git_commit: 01e101bf55c84a0f65561cde9c9c3c109a718d26
branch: feature/front-views
repository: PlatePlan
topic: "What the four Thymeleaf screens and site.css already commit to, and which copy and design-contract gaps a front pass has to cover"
tags: [research, codebase, thymeleaf, site-css, home, register, profile, plan]
status: complete
last_updated: 2026-10-26
last_updated_by: unknown
---

# Research: Front views — tokens, shared chrome, and copy against the current product

**Date**: 2026-10-26T20:09:46+01:00
**Researcher**: unknown
**Git Commit**: 01e101bf55c84a0f65561cde9c9c3c109a718d26
**Branch**: feature/front-views
**Repository**: PlatePlan

Source anchors below are in that commit. `context/changes/front-views/` is uncommitted on this branch. No browser render was taken.

## Research Question

For a pass over the whole front (`home`, `register`, `profile`, `plan`, and the shared chrome), where do the visual values and shared fragments live, which public sentences contradict the seven-day household product, and which contract gaps (tokens, duplicated fields, entry) does a later plan have to cover?

## Summary

On this commit the page front is six Thymeleaf files under `src/main/resources/templates`, styled by the eleven custom properties in `site.css:3-13`. The signed-out register control in that set is the secondary header link at `chrome.html:43`. `home.html` lines 7–84 contain no `href` to `/register`. Public sentences at `home.html:11`, `home.html:35`, `register.html:11`, and the shared footer `chrome.html:49` still say a next-day plan, one person, or both. `prd.md:20` and `prd.md:24` say seven days starting tomorrow, for the owner and at most three added people. The plan screen’s own lead at `plan.html:17` already says seven days. Profile and plan stay behind sign-in: `SecurityConfiguration.java:24` sets the login window to `/`, and `SecurityConfiguration.java:44-48` leaves `/profile` and `/plan` on `authenticated()`.

## Detailed Findings

### Value source

In `src/main/resources/static/css/site.css`, the `:root` block at lines 3–13 defines this set of custom properties: `--paper`, `--ink`, `--muted`, `--line`, `--card`, `--header`, `--field`, `--green`, `--green-press`, `--serif`, `--sans`. `--card` and `--header` are both `#fffcf8` (`site.css:7`, `site.css:11`). Names are colors and font stacks, not role names such as `primary` or `surface`.

This inspected stylesheet has no second token file beside it. A search of `src/main/resources` for `*.css` in this research returned `site.css` alone. The six HTML files link that sheet through `chrome.html:8` (`th:href="@{/css/site.css}"`), included from `home.html:3`, `register.html:3`, `profile.html:3`, and `plan.html:3`. Those six files contain no `style=` attribute.

Hex colors on rules, outside the `:root` values:

| Line | Selector | Value |
| --- | --- | --- |
| `site.css:42` | `.skip` | `#fff` |
| `site.css:127` | `.form-error` | `#b00020` |
| `site.css:162` | `input` | `#fff` |
| `site.css:174` | `.button` | `#fff` |
| `site.css:206` | `.button-secondary:hover` | `#f3eee6` |
| `site.css:498` | `select` | `#fff` |
| `site.css:591` | `.product-chip` | `#fff` |

`border-radius` in the lines read for this pass is a literal: `0.4rem` on `input` (`site.css:161`), `.button` (`site.css:172`), `select` (`site.css:497`), and `.product-chip` (`site.css:590`); `0.85rem` on `.spec, .panel, .account-form` (`site.css:279`). `:root` does not define a radius property.

Breakpoints in this file: `max-width: 1100px` (`site.css:635`), `max-width: 800px` (`site.css:650`), `prefers-reduced-motion: reduce` (`site.css:700`).

`input` width is `9rem` at `site.css:158`. `.account-form input` and `.profile-form .field input` override that to `100%` at `site.css:390-391` and `site.css:489-491`. The header login inputs use the `9rem` rule.

Named states present in this file: `.button:hover` (`site.css:184`), `:focus-visible` outline `var(--green)` (`site.css:211-217`), `.button:disabled` (`site.css:189`), `.form-error` (`site.css:125-128`). `.product-chips:empty` (`site.css:580-581`) sets margin to zero. PDF drawing colors in `PlanPdfWriter` were not opened.

### Shared fragments and duplicated fields

`chrome.html` defines `head` (lines 3–9), `header` (lines 11–46), and `footer` (lines 47–51). Each of `home.html`, `register.html`, `profile.html`, and `plan.html` repeats the skip link `Przejdź do treści` and `<main id="tresc">` around those fragments (`home.html:5-7` and the same two lines in the other three screens).

`profile-person.html:4` is the member field fragment. `profile.html` includes it at lines 147, 150, and 157. The owner block at `profile.html:24-135` is a separate copy of the same field groups (sex, goal, activity, calories, product lists), plus the owner email field at `profile.html:88-93`.

`.button` and `.button-secondary` are the shared control classes. There is no button fragment. Login fields at `chrome.html:33-40` and register fields at `register.html:25-32` both use `div.field`, with different ids.

### Entry

`GET /` renders `home` with no principal check. `GET /register` renders `register` when there is no principal. `SecurityConfiguration.java:44-46` permits `/`, `/register`, `/css/**`, `/js/**`, `/.well-known/**`, and `/error`. Any other request, including `/profile` and `/plan`, is `authenticated()` (`SecurityConfiguration.java:48`). Login page is `/` (`SecurityConfiguration.java:50`). This pass did not capture an HTTP response, so the redirect status is the `formLogin().loginPage("/")` setting, not a recorded status code.

When `currentLogin` is null, `chrome.html:29-44` shows the login form and `Zarejestruj się`. Plan and Profil links render when `currentLogin != null` (`chrome.html:19-20`). `home.html` does not branch its body on `currentLogin`, so a signed-in visitor on `/` still gets the marketing sections.

Nav links Produkt, Jak to działa, and Kalorie point at `/#produkt`, `/#jak-dziala`, and `/#kalorie` (`chrome.html:16-18`). Those section ids are on `home.html:33`, `home.html:43`, and `home.html:65`.

### Copy against the current product text

`prd.md:20` and `prd.md:24`: seven days starting tomorrow; the owner may add at most three people. `prd.md:72`: calories use age, height, weight, sex, goal, and activity level. `prd.md:109`: a child’s development is maintain or gain. `AGENTS.md:11`: add a library when `tech-stack.md` lists it.

Contradictions in the six templates:

| Quote | Where | Against |
| --- | --- | --- |
| „Plan na kolejny dzień, bez gotowej diety.” | `home.html:11` | `prd.md:20` (seven days starting tomorrow). „bez gotowej diety” matches that same paragraph. |
| „plan diety na kolejny dzień” | `home.html:3`, `home.html:12`, `home.html:27`, `home.html:60` | `prd.md:20` |
| „Aplikacja dla jednej osoby” | `home.html:35` | `prd.md:24` |
| „Jedno konto na jedną osobę.” | `register.html:11` | `prd.md:24`. The rest of that sentence (registration ends in the app; next visit uses the top bar) matches `prd.md:66`. |
| „plan diety na kolejny dzień” | `chrome.html:49` | `prd.md:20`. This footer is included from home, register, profile, and plan, so the plan page shows it under a lead that says seven days (`plan.html:17`). |

Activity is missing from the public input lists at `home.html:12`, `home.html:19`, `home.html:71`, and `register.html:13`. `prd.md:72` includes activity level. The profile screen states it: `profile.html:10`.

The owner goal `<select>` at `profile.html:61-63` offers Schudnąć, Utrzymać wagę, and Przytyć. No sentence in the six templates states the child limit from `prd.md:109`. This pass did not read the Java validator that refuses a child goal.

`plan.html:15-17` says seven days and four meals (śniadanie, drugie śniadanie, obiad, kolacja) and two PDFs that disappear on refresh. `plan.html:38-39` uses `dieta-na-tydzien.pdf` and `lista-zakupow.pdf`. `profile.html:139` says at most three added people. Those four lines match `AGENTS.md` Plan and Profile. The six templates do not contain the phrase „od jutra”.

`home.html` lines 7–84 contain no link to `/register`.

### Audit charges

Each charge is one gap a plan can pick up. Wording of new headlines is not a charge: `change.md` asks for encouraging account copy and does not lock sentences.

1. **Missing token.** `.form-error` uses `#b00020` (`site.css:127`), and the other hex values in the table above are outside `:root`. Effect: error text and several whites do not follow `--ink`, `--paper`, or `--card`.
2. **Missing shared fragment.** The owner fields (`profile.html:24-135`) repeat the member fragment (`profile-person.html:4`) instead of including it. Effect: a visual change to person fields is two edits. The email field is on the owner block at `profile.html:88-93`.
3. **Accidental entry.** On the signed-out start page the control styled as `.button` is „Zaloguj się” (`chrome.html:41`). „Zarejestruj się” is `.button-secondary` (`chrome.html:43`). The hero has no account link. Effect: the page that should ask for an account gives the primary control to login. `prd.md:51` still wants a login window with Register; the control exists in the header.
4. **Stale public copy.** The next-day and one-person sentences listed above, plus the activity omission, describe a smaller product than `prd.md:20`, `prd.md:24`, and `prd.md:72`. Effect: the text on the way to an account, and the footer under the plan, disagree with the screen the person gets after sign-in.

## Code References

- `src/main/resources/static/css/site.css:3-13` — the eleven custom properties
- `src/main/resources/static/css/site.css:127` — error color outside `:root`
- `src/main/resources/static/css/site.css:158` — `input` width `9rem`, used by the header login
- `src/main/resources/templates/fragments/chrome.html:8` — the stylesheet link
- `src/main/resources/templates/fragments/chrome.html:29-44` — signed-out login and register
- `src/main/resources/templates/fragments/chrome.html:49` — footer, next-day sentence
- `src/main/resources/templates/home.html:11` — hero headline, next day
- `src/main/resources/templates/home.html:35` — one person
- `src/main/resources/templates/register.html:11` — one person
- `src/main/resources/templates/profile.html:61-63` — three goals, no child sentence
- `src/main/resources/templates/plan.html:17` — seven days, four meals, two PDFs
- `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:24` — login window `/`
- `context/foundation/prd.md:20` — seven days starting tomorrow
- `context/foundation/tech-stack.md:24` — Thymeleaf; that paragraph does not name a CSS framework

## Architecture Insights

The view stack on this commit is server-rendered Thymeleaf plus one stylesheet. `tech-stack.md:24` names Thymeleaf and the declared starters. It does not name a CSS framework or a component library. `AGENTS.md:11` says to add a library when that file lists it. Shared UI that already exists is `chrome.html`, `profile-person.html`, and the classes `.button`, `.field`, `.form-error`, and `.panel`.

`/` is both the marketing page and the login window (`SecurityConfiguration.java:17-18`). A front pass that removes the header login form would drop the login window `prd.md:51` describes.

## Historical Context (from prior changes)

Scored against the files this pass opened.

- **Supported.** Views are Thymeleaf in Polish, composed from `fragments/chrome`. Stated in `context/archive/2026-10-02-save-user-profile/plan.md:17`. The six current templates still do that. The old line note `chrome.html:11-20` in that plan matches the current header start (`chrome.html:11`); the nav now runs through `chrome.html:21`.
- **Supported for the profile screen.** Owner form, then at most three people, empty owner name `Ja`, Polish messages. `context/changes/household-members/plan.md:124-128`. `profile.html:10` still says an empty name leaves the label Ja, and `profile.html:139` says at most three people.
- **Partial.** `context/archive/2026-10-02-register-and-sign-in/plan.md:36` says no `/konto` page and no other signed-in screen. No `konto.html` is in the six templates. The “no other signed-in screen” clause is contradicted by later `profile.html` and `plan.html`.
- **Partial.** `context/archive/2026-10-02-register-and-sign-in/plan.md:38` forbids a new library and names Bean Validation as one example. Current `tech-stack.md:24` lists `spring-boot-starter-validation`. The standing library rule for this pass is `AGENTS.md:11` plus the dependencies named in `tech-stack.md:24`, not that archived example.
- **Supported as a past decision, not a current marketing lock.** `context/archive/2026-10-01-spring-security-sign-in/plan.md` treats `/` as the login window. This pass did not re-open that plan. The current lock is `SecurityConfiguration.java:17-18` and `SecurityConfiguration.java:24`.

`context/foundation/lessons.md` is absent. `context/changes/front-views/change.md` (status `new` at the start of this research) asks for all views, account-encouraging copy, and a neat layout. It names no palette and no headline.

## Related Research

Not applicable to this question. Archived `research.md` files under `context/archive/` cover sign-in and the testing rollout. This pass did not re-read them.

## Open Questions

- Headline and section wording is an open product choice. `change.md` requires encouraging account copy. It does not approve particular sentences. Chat wording from the previous turn is not in the change folder.
- Header crowding is inferred from `site.css:158` (`9rem` inputs beside the login button). This research did not open the page in a browser, so the visual weight is not measured.
- Whether a child `LOSE_WEIGHT` choice is refused in Java was not read. The visible owner options at `profile.html:61-63` include Schudnąć.
- The body served for `/error` was not traced. `/error` is permitAll at `SecurityConfiguration.java:29` and `SecurityConfiguration.java:45`.
- `PlanPdfWriter` colors were not opened. They are outside the page stylesheet.
