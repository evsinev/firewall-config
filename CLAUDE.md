# CLAUDE.md

A Java 8 CLI that turns a directory of YAML host descriptions into `iptables-save` files, bind zones,
keepalived configs, L2/L3 diagrams and Redmine audit pages. Nothing is written by hand, and since
every generator loads and validates the whole description on startup, they double as a linter for it.

This file is a navigator; the documentation itself is under `website/src/content/docs/` — see
[Where to read more](#where-to-read-more).

## Build and run

```sh
./mvnw clean package                      # -> target/firewall-config.jar (assembly bound to `package`)
java -jar target/firewall-config.jar <config-dir> <host|group-NAME> <out-dir>   # the iptables generator
java -cp  target/firewall-config.jar com.payneteasy.firewall.MainL3Diagram …    # the other 20 entry points
```

The fat jar is the only delivery form — nothing is published to a Maven repository, and the
`1.1-SNAPSHOT` in `pom.xml` is meaningless (release versions come from the git tag).

## Verify a change

**The gate is `./mvnw clean verify`** — 208 tests plus a JaCoCo rule that fails the build below
**80% line coverage**. The measured bundle excludes the Swing editor, the Redmine clients,
`podmancheck`, `CommandProcess` and thirteen CLI shims (~31% of the tree); the list is in
`pom.xml`, one comment per entry. Adding a new CLI means either testing it or adding it there.

The generated output is compared byte for byte against expected files in
`src/test/resources/golden/` — iptables for 7 hosts, all 20 wiki pages, bind `zones.conf` and
zone records, keepalived, RouterOS and nwdiag. **After deliberately changing `PacketServiceImpl`
or `iptables.vm`, regenerate the golden files and read that diff** — it is the review step:

```sh
./mvnw clean package
cd examples/demo-network && mkdir -p gen && java -jar ../../target/firewall-config.jar . group-internal gen
java -jar ../../target/firewall-config.jar . sw-core-1 gen
cp gen/* ../../src/test/resources/golden/iptables/          # then read `git diff`
```

Zone files are compared by record lines only — `MainBind` stamps a `yyMMddHHmm` serial, a date,
the local hostname and `user.name` into every header. `MainWiki` skips pages whose hash is
unchanged, so its tests run against a copy without `pages_history.yml`
(`TestFixtures.copyDemoNetworkInputs`).

Where a trap from [Traps](#traps) is pinned by a test, the test says so in a comment — do not
"fix" the production behaviour to make it pass.

The demo-network sweep still runs in CI as a second net — every generator must accept the demo
network. From `examples/demo-network`:

```sh
JAR=../../target/firewall-config.jar
mkdir -p gen target wiki-out bind-out
java -jar $JAR . group-internal gen
java -cp $JAR com.payneteasy.firewall.MainL3Diagram --run-nwdiag=false --filter internal,ipmi . current
java -Djava.awt.headless=true -cp $JAR com.payneteasy.firewall.MainL2SvgDiagram --filter internal,ipmi . current
java -cp $JAR com.payneteasy.firewall.MainWiki ./wiki-out dummy-key .
java -cp $JAR com.payneteasy.firewall.MainBind . demo.example.com bind-out
java -cp $JAR com.payneteasy.firewall.MainKeepalived . fw-1
java -cp $JAR com.payneteasy.firewall.MainMikrotik . sw-core-1
```

Test helpers live in `src/test/java/com/payneteasy/firewall/testing/`: `TestFixtures` (fixture
lookup, golden text, recursive copy, stdout capture) and `RecordingCanvas` (an `ICanvas` that
records draw calls, which is what makes the L2 model testable headless). Broken-config fixtures —
one directory per `ConfigDaoYaml` validation rule — are under `src/test/resources/config/`; each
one needs a *valid* `protocols.yml`, or the constructor fails with `FileNotFoundException` and the
test passes for the wrong reason.

## How the code is laid out

Three stages, always in this order:

1. **Load** — `dao/ConfigDaoYaml` reads the entire description in its constructor. Host `name` and
   `group` come from the *filesystem* (`hosts/<group>/<name>.yml`), `services_links` are resolved
   into shared `TService` objects, `protocols.yml` is validated.
2. **Derive** — `service/impl/PacketServiceImpl` is **the only file containing firewall semantics**:
   INPUT/OUTPUT/FORWARD derivation, the SNAT-vs-DNAT decision, the FORWARD suppression filters.
   Change behaviour here, not in a template. `l2/editor/create/L2GraphCreator` and
   `l3/CreateL3Diagram` are the equivalent (much simpler) derivations for the diagrams.
3. **Render** — dumb formatting only: Velocity (`src/main/resources/iptables.vm`), eight Mustache
   templates, the `ICanvas` Swing/PNG/SVG path, and the Redmine clients.

## Where to read more

| Question | File |
|---|---|
| How are rules derived? Why is there no rule for X? | `website/src/content/docs/internals/packet-derivation.md` |
| Package map, pipeline, design consequences | `website/src/content/docs/internals/architecture.md` |
| Known traps and hard-coded assumptions | `website/src/content/docs/internals/limitations.md` |
| Every YAML field | `website/src/content/docs/reference/yaml-schema.md` |
| All 21 entry points and their arguments | `website/src/content/docs/reference/cli.md` |
| The `url:` / `access:` / `nat:` grammar | `website/src/content/docs/configuration/services.md` |

## Conventions

New code: picocli (`@CommandLine.Command` + `Callable<Integer>`; extend
`shell/AbstractDirPrefixFilterCommand` when the command takes `<dir> <prefix> [--filter]`), lombok
`@Data`/`@Builder`/`@FieldDefaults` for Gson DTOs only, ordinary field names, JUnit 4 with
`org.hamcrest.MatcherAssert.assertThat` (not the deprecated `Assert.assertThat`). No JUnit 5, no
AssertJ, no Mockito — `IConfigDao`'s 17 methods are used interdependently, so tests build real
`THost` beans or load a fixture directory instead of stubbing.

Editing an existing file: match that file's style — `aHostname` argument prefix, `theHosts` fields,
fields declared at the bottom of the class in `ConfigDaoYaml` and `PacketServiceImpl`. Do not
refactor it in passing.

Regardless of file:

- `T` prefix = YAML/JSON-mapped bean, `I` prefix = interface; derived models get no prefix.
- Fields read by Velocity or Mustache stay `snake_case` public fields **with** `getSnake_case()`
  getters. Renaming one breaks `iptables.vm` silently — Velocity resolves names at render time.
- Beans in `dao/model` and `service/model` are plain public-field beans; snakeyaml and Velocity need
  that, so no lombok there.
- No logging framework. Errors are long multi-line `ConfigurationException` / `IllegalStateException`
  messages naming the offending host and service — keep that; it is the product's error UX.
- 4-space indent, wildcard imports are normal, local declarations column-aligned in older classes.

## Traps

- **Netmasks**: only `/24` (implied) and `/16` are supported; anything else throws from
  `TInterface.getLongNetmask()`. The `/24` assumption also leaks into `networks.yml` keys and into the
  "same network" suppression filter.
- **SNAT for private ranges is a hard-coded address list** in `PacketServiceImpl` (marked
  `// todo hot fix for SNAT`). Adding a SNAT-ed private network means editing Java, not YAML.
- `findAddress` picks the source address with the longest *binary* prefix match — it is not a routing
  lookup, so multi-homed hosts can get the wrong leg. It is also the one unit-tested method.
- **No generator creates directories**, and several write CWD-relative fixed paths
  (`target/network.diag`, `target/hosts.html`, `<prefix>-l2.svg`). `mkdir -p target` first; this is
  the most common first failure.
- `<prefix>` is positionally required even by commands that ignore it (`MainL3Diagram`,
  `MainExternalServices`) — pass `current`.
- VRRP packets are computed but `iptables.vm` ignores them, so the generated rules do not permit VRRP
  advertisements. That needs a `customRules` entry for protocol 112 to `224.0.0.18`.

## Docs site (`website/`)

`npm install`, then `npm run dev` (serves `localhost:4321/firewall-config`) or `npm run build`.
The build **fails on broken internal links** (`starlight-links-validator`), and internal links need
the `/firewall-config/...` base prefix. `.github/workflows/docs.yml` deploys on `master` pushes that
touch `website/**`.

A behaviour change is not done until the matching page is updated.

## Releases

Write the `CHANGELOG.md` section first, then tag:

```sh
git tag -a 1.2.1 -m "1.2.1" && git push origin 1.2.1
```

`.github/workflows/release.yml` stamps the tag as the project version, builds the jar, runs the demo
sweep as a gate, and **fails if `CHANGELOG.md` has no `## [<tag>]` section**.

## Commits and branches

`master` is the default branch. One-line [Conventional Commits](https://www.conventionalcommits.org/)
in English (`feat:`, `fix:`, `docs:`, `ci:`), no body, no trailers. Commits before mid-2026 use the
older `#<redmine-id> Sentence case` form.

## Security

- Redmine API keys are passed as **positional arguments** — they land in shell history and in the
  wrapper scripts committed to description repositories.
- `redmine/RedmineEasyClient` installs a trust-all `X509TrustManager`; TLS verification is disabled.
- `MainPodmanCheckHardningGuide --redmine-enabled` creates Redmine issues with no deduplication, on
  every run.

Never commit a real API key, a real network description, or real host addresses. `examples/demo-network`
uses the RFC 5737 documentation ranges for exactly this reason.
