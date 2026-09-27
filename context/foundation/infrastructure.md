---
project: PlatePlan
researched_at: 2026-09-26
recommended_platform: Fly.io
runner_up: Render
context_type: mvp
tech_stack:
  language: Java
  framework: Spring Boot 4.1.1
  runtime: Java 21
---

## Recommendation

**Deploy on Fly.io.**

PlatePlan is one Gradle module: Java 21, Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, Thymeleaf, Actuator), account data that must survive visits, and two PDFs generated per request. The app is request/response, one region is enough, external data services are acceptable, and there is no preferred host. Cost matters, but the free container option (Render) cannot keep a Spring Boot process and the account profile reliable at 512 MB with an ephemeral disk. Fly.io runs the Docker image on a Machine, with `flyctl` for deploy, logs, secrets, and image rollback. Use one `shared-cpu-1x` Machine at 1 GB in Frankfurt (`fra`), with high availability off and auto-stop on. A Machine that stays running is about $5.70 per month in a baseline region (Fly resource pricing, checked 2026-09-26); a stopped Machine bills rootfs at $0.15 per GB per month. Checked the same day: `flyctl` has no Java or Gradle scanner, Warsaw (`waw`) is not a current region, and Machine memory prices rise 20% on 2026-10-01.

## Platform Comparison

Scores are Pass / Partial / Fail against CLI-first maintenance, managed runtime, agent-readable docs, a stable deploy API, and MCP or an equivalent agent integration. Vercel and Netlify are out: they have no JVM for this stack.

| Platform | CLI-first | Managed/Serverless | Agent-readable docs | Stable deploy API | MCP / integration | Sum |
|---|---|---|---|---|---|---|
| Render | Pass | Pass | Pass | Pass | Pass | 10 |
| Fly.io | Pass | Pass | Pass | Pass | Partial | 9 |
| Cloudflare Containers | Pass | Pass | Pass | Partial | Pass | 9 |
| Railway | Partial | Pass | Pass | Pass | Partial | 8 |
| Vercel | Pass | Pass | Pass | Fail | Partial | rejected |
| Netlify | Pass | Pass | Partial | Fail | Pass | rejected |

Render scores highest on the five criteria and is the only $0 JVM path: a free web service is 0.1 CPU and 512 MB, sleeps after 15 minutes without inbound traffic, and wakes in about a minute ([Deploy for Free](https://render.com/docs/free)). Java is Docker-only ([Docker on Render](https://render.com/docs/docker)). The CLI covers deploys and logs; an official MCP server can create services, read logs, and run read-only SQL ([Render MCP](https://render.com/docs/mcp-server)). The free disk is ephemeral, free Postgres expires 30 days after creation and has no backups, and 512 MB is a known OOM line for Spring Boot. Rollback on the free plan keeps only the two previous deploys.

Fly.io is a managed Machine host. `fly deploy`, `fly logs`, and `fly secrets` are non-interactive. Docs are indexed at `https://docs.fly.io/llms.txt`. MCP exists as `fly mcp server` and is marked experimental in the flyctl docs, so that criterion is Partial. There is no Java scanner; a Dockerfile has to be in the repo before `fly launch`. The trial is 2 VM hours or 7 days, and trial Machines stop after 5 minutes ([Free Trial](https://fly.io/docs/about/free-trial/)). A card is required to keep running. Pricing is per second while started ([Resource Pricing](https://fly.io/docs/about/pricing/)).

Cloudflare Containers have been generally available since 2026-04-13 and can run a Docker image, including a JVM ([changelog](https://developers.cloudflare.com/changelog/post/2026-04-13-containers-sandbox-ga/)). They require the Workers Paid plan ($5 per month minimum) and a JavaScript Worker plus a Durable Object in front of the container ([Containers pricing](https://developers.cloudflare.com/containers/pricing/)). That second runtime is why the deploy API is Partial for this repo. Docs are markdown and an MCP endpoint exists. A single region and a cost-first MVP do not need the edge network.

Railway has a Spring Boot guide, detects a Gradle repo, and bills CPU and RAM by the second ([pricing](https://railway.com/pricing), [Spring Boot guide](https://docs.railway.com/guides/spring-boot)). The Free plan is a $1 monthly credit and a 0.5 GB cap, which does not cover an always-on JVM. Serverless sleep can answer the first request with 502. Hobby is $5 per month even when usage is lower. Arbitrary rollback is in the dashboard; image retention on Free is 24 hours, so CLI-first is Partial. Remote MCP at `mcp.railway.com` is documented, and Railway has described it as public testing, so MCP is Partial.

Vercel function runtimes are Node.js, Bun, Python, Rust, Go, Ruby, Wasm, and Edge ([runtimes](https://vercel.com/docs/functions/runtimes)). There is no Java runtime. Hobby is $0 for personal non-commercial use and still cannot run this process. Vercel MCP is in public beta.

Netlify Functions are JavaScript, TypeScript, or Go. There is no JVM. The official MCP server is a real integration, and it does not create a place to run Spring Boot.

### Shortlisted Platforms

#### 1. Fly.io (Recommended)

One container, one region, a real JVM, and a CLI an agent can drive. High availability defaults to two Machines; this MVP should launch with `--ha=false`, 1 GB RAM, and auto-stop. Account data must not live only on the Machine rootfs. Ollama does not belong on the cheap Machine.

#### 2. Render

Best raw criterion score and the only zero-dollar host that can start a JVM container. It lost the decision because 512 MB, spin-down, and a disk that disappears on sleep fight the requirement that the profile survives between visits. It remains the fallback if a card or the October memory price is unacceptable.

#### 3. Railway

Closest product to Fly for a Spring Boot jar, with an official guide. The $1 free credit and 512 MB cap lose to a Fly Machine that can sleep at rootfs prices, and the $5 Hobby floor matches Cloudflare's minimum without Cloudflare's edge benefit.

## Anti-Bias Cross-Check: Fly.io

Render led the first scoring pass. Its cross-check (512 MB OOM, 15-minute spin-down, ephemeral disk, no room for local Ollama) is why the decision moved here. The checks below are for Fly.io.

### Devil's Advocate — Weaknesses

1. `fly launch` defaults `--ha` to true, so the first deploy can create two Machines. The smallest preset is 256 MB. Spring Boot 4.1.1 is killed before it listens, and the second Machine still bills.
2. The proxy only reaches a process bound to `0.0.0.0` on `internal_port`. `Dockerfile` and `fly.toml` set `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080`. `application.properties` does not set `server.address`. If those environment variables are removed, Spring Boot binds to localhost, the health check fails, and the release never goes healthy. The same file exposes every Actuator endpoint, with heap dump and shutdown unrestricted, and the build has no Spring Security.
3. The trial is 2 VM hours or 7 days, and trial Machines stop after 5 minutes. Continuing needs a card, which ends the trial and starts billing. On 2026-10-01 Fly raises Machine memory prices by 20%. A 1 GB Machine left running is already about $5.70 per month in a baseline region before that increase.
4. Local Ollama does not fit a 1 GB Machine. Model weights are gigabytes. Putting inference on the same Machine, or on a GPU preset (`--vm-gpu-kind` on `fly launch`), turns a diet-plan MVP into a large compute bill.
5. `fly deploy --image` rolls back the image only. It does not roll back `fly.toml`, secrets, or a database. Managed Postgres is billed outside the app and is not deleted when the app is deleted. Volumes bill at $0.15 per GB per month while the Machine is stopped, and new volumes get daily snapshots (first 10 GB of snapshot data free, then $0.08 per GB). Fly does not promise to keep old images forever.

### Pre-Mortem — How This Could Fail

The app moved to Fly.io so the JVM and the account profile would stay intact. `fly launch` kept its defaults. Two Machines came up at 256 MB. Spring Boot died before port 8080 opened, and the process was bound to localhost, so the proxy still failed after one Machine was given more RAM. The spare Machine kept billing. A volume was added for the profile. It billed while stopped, and daily snapshots turned on by default. To keep the stack's local Ollama, the model was placed on that Machine. RAM jumped from 1 GB into a much larger size in the same week memory prices rose 20 percent, on 1 October 2026. The app was later destroyed from the CLI. A Managed Postgres cluster, accepted from the launch prompt, was not. Six months later the morning plan still called a model that did not fit the small Machine, and the invoice looked like a small always-on platform rather than rootfs on a stopped Machine.

### Unknown Unknowns

- `flyctl` scanner list on master (checked 2026-09-26) has no Java, Gradle, or Spring Boot entry. It picks up a Dockerfile only when that file already exists. `fly launch` will not scaffold a Boot 4.1.1 image. Fly's own agent note that says "do not hand-write a Dockerfile" applies to frameworks the scanner knows.
- Warsaw (`waw`) is a deprecated region. Current European codes include `fra`, `ams`, `cdg`, `lhr`, and `arn` ([Regions](https://fly.io/docs/reference/regions/)). Confirm with `fly platform regions` before launch.
- Scale-down and the bluegreen strategy send SIGINT by default (`--signal` on `fly launch`). A process that ignores it is killed later and the deploy looks stuck.
- `fly secrets list` shows names and digests, not values. An agent cannot read a secret back out to verify it.
- Auto-stop defaults to `stop` on current `fly launch`, which is what this app wants. That default is easy to override with `--auto-stop off` while debugging a slow boot, and the Machine then bills 24 hours a day.
- `fly mcp server` is experimental. Treat `fly logs --no-tail` and `fly deploy` as the stable agent path.
- The boot jar for this build is `build/libs/PlatePlan-0.0.1-SNAPSHOT.jar` (`rootProject.name` in `settings.gradle.kts`, `version` in `build.gradle.kts`). A Dockerfile that copies `target/*.jar` or a Boot 3 `starter-web` layout will not match this tree. Local development stays `.\gradlew.bat bootRun`. Fly does not replace that loop.

## Operational Story

- **Preview deploys**: Fly does not publish a pull-request preview URL. A branch is tested by deploying to a second app (`fly launch` with another `--name`) or by deploying straight to the single MVP app. Fork PRs have no automatic protection story because there is no automatic preview.
- **Secrets**: `fly secrets set KEY=value` stores them for the app and restarts Machines. `fly secrets list` shows names only. Anyone with deploy rights on the app can replace a secret; nobody can read the old value back. Rotate by setting a new value. Do not put secrets in `fly.toml`.
- **Rollback**: `fly releases --image`, then `fly deploy --image registry.fly.io/<app>:<label>`. That boots the old image without a rebuild. Schema changes, volume contents, secrets, and `fly.toml` stay as they are. Images can be pruned; a label you may need later should also live in your own registry.
- **Approval**: A human adds the payment card, deletes a volume, and destroys Managed Postgres. An agent with a Fly token may `fly deploy`, `fly logs --no-tail`, `fly status`, and `fly secrets set` for non-production values. Destroying the app or a database stays with a human, because Postgres is not removed with the app.
- **Logs**: `fly logs --no-tail` prints the current buffer and exits. `fly logs` streams until interrupted. `fly status` shows Machine health. The experimental MCP server can expose logs, but the CLI is the contract.

## Risk Register

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Two 256 MB Machines on first launch, Spring Boot OOM | Devil's advocate | H | H | `fly launch --ha=false --vm-memory 1024`. After launch, `fly status` must show one Machine. |
| App listens on localhost, proxy health check fails | Devil's advocate | H | H | `Dockerfile` and `fly.toml` set `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080`. `internal_port` is 8080. Keep both if the image changes. |
| Heap dump and shutdown are public and unauthenticated | Repo config | H | H | `application.properties` sets `management.endpoints.web.exposure.include=*` and unrestricted `heapdump` and `shutdown`. Restrict both before a public deploy. There is no Spring Security on the classpath. |
| Trial ends in 2 hours or at the first card, then a 24/7 bill | Devil's advocate | H | M | Add the card on purpose. Keep `--auto-stop stop`. Confirm with `fly status` that the Machine stops when idle. |
| Memory price +20% on 2026-10-01 | Research finding | H | M | Size the Machine at 1 GB, not a GPU or multi-GB preset. Re-check `fly platform vm-sizes` after 1 October 2026. |
| Ollama on the same Machine | Devil's advocate / Pre-mortem | H | H | Keep inference off this Machine. Call an external model endpoint, or run Ollama only on the development machine. |
| Profile stored on rootfs and lost on stop | Pre-mortem | H | H | Put account fields on a volume or an external database. Rootfs of a stopped Machine is not app storage. |
| Volume and snapshot charges while stopped | Unknown unknowns | M | L | Start with the smallest volume. Snapshots are on by default; disable them if the profile is disposable test data. |
| Managed Postgres survives `fly apps destroy` | Pre-mortem / Unknown unknowns | M | M | Launch with `--no-db`. If a database is created later, delete it in the dashboard as its own step. |
| Image rollback leaves schema and secrets behind | Devil's advocate | M | M | Keep migrations backward-compatible. Record the image label at deploy time. |
| `waw` used from an old guide | Unknown unknowns | M | M | Use `fra` (Frankfurt) unless `fly platform regions` shows a closer current code. |
| Experimental MCP treated as the only ops path | Unknown unknowns | L | M | Use `fly deploy` and `fly logs --no-tail` for anything that must work unattended. |

## Getting Started

These commands match this repo (Gradle wrapper, Java toolchain 21, Boot 4.1.1, jar `build/libs/PlatePlan-0.0.1-SNAPSHOT.jar`) and current `fly launch` flags (checked 2026-09-26). `.\gradlew.bat bootRun` remains the local server. Do not add a Fly-specific dev command.

1. Install and log in from PowerShell:

```powershell
iwr https://fly.io/install.ps1 -useb | iex
fly auth login
fly platform regions
```

2. `Dockerfile`, `.dockerignore`, and `fly.toml` are already in the repo. The image builds `build/libs/PlatePlan-0.0.1-SNAPSHOT.jar` on Java 21 and listens on `0.0.0.0:8080`. `fly.toml` names the app `plate-plan`, uses one shared CPU and 1 GB in `fra`, and stops the Machine when idle. `application.properties` exposes every Actuator endpoint, including unrestricted heap dump and shutdown, with no authentication in the build.

3. Create the empty app once, without a database and without Fly's generated workflow (this repo already has `.github/workflows/ci.yml`):

```powershell
fly apps create plate-plan --org personal
```

If that org name is wrong, use the org shown by `fly orgs list`.

4. Confirm `fly.toml`, then deploy from the machine that is logged in:

```powershell
fly deploy --ha=false --remote-only
fly status
fly logs --no-tail
```

5. Set secrets only after the app exists (`fly secrets set` returns 404 otherwise). Example: `fly secrets set DATABASE_URL=jdbc:postgresql://...`. List names with `fly secrets list`. Roll back with `fly releases --image` and `fly deploy --image <registry.fly.io image>`.

## Out of Scope

The following were not evaluated in this research:

- Production-scale architecture (multi-region, HA, DR)

GitHub Actions is in the repo: `.github/workflows/ci.yml` runs `./gradlew test` on pull requests and on `main`. After a push to `main` it creates the Fly app `plate-plan` when `flyctl status` cannot see it, then runs `flyctl deploy --remote-only --ha=false`. The action reads the repository secret `FLY_API_TOKEN`. An organization token can create that app; a deploy token cannot, because it only exists after the app does.
