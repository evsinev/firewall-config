# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

A note on version numbers: `pom.xml` has carried `1.1-SNAPSHOT` since 2013 and the published
releases were numbered separately by hand (`1.1-4` … `1.1-7`, then `1.1-1`). From `1.2.0`
onwards the release version comes from the git tag — pushing a tag like `1.2.0` builds the fat
jar and publishes it as `firewall-config-1.2.0.jar`
(see [`.github/workflows/release.yml`](.github/workflows/release.yml)).

## [Unreleased]

### Added

- **`external_peer: true` on a host.** Declares that the host is outside our address space even
  though its address is private — a partner reached over a tunnel — so traffic *towards* it is
  SNAT-ed to the service's `nat:` address, exactly as for a public destination. The flag is read on
  the destination side only; as a source such a host stays private and produces no DNAT.
  `examples/demo-network` gained `partner-vpn.example.com` to exercise it.

### Removed

- **The hard-coded SNAT address list in `PacketServiceImpl`.** Fifteen literal addresses (and the
  `172.16.4.` prefix) marked `// todo hot fix for SNAT` are gone; the same effect is now declared per
  host with `external_peer: true`.

  **Update your description *before* upgrading the generator.** Every destination that used to match
  that list needs the flag, including *each* host inside a matched network such as `172.16.4.0/24`. A
  missed host fails silently: the flow is still generated, just as a plain `FORWARD` with no
  translation. Generate into a scratch directory with both versions and diff — the difference must be
  empty.

## [1.3.0] — 2026-07-29

A safety net and a clear-out. The generated output is now pinned by golden files and a coverage
gate, and the dependency tree — frozen since 2013 — no longer carries a known CVE. Nothing about
the generated rule sets, wiki pages, zone files or diagrams changed: every golden file still
matches byte for byte, which is what made the dependency work reviewable at all.

One behaviour change to be aware of before upgrading: wiki pages are pushed to Redmine as JSON
rather than XML.

### Added

- **A test suite and an 80% coverage gate.** 210 JUnit tests, up from 9. The generated output
  of six generators is now compared against expected files committed under
  `src/test/resources/golden/`, so a silently changed iptables rule, wiki page, bind record,
  keepalived instance, RouterOS vlan block or nwdiag network fails the build — the gap CLAUDE.md
  used to record as *"nothing asserts on the generated output"*. Alongside them, unit tests cover
  the packet derivation's error branches, every `ConfigDaoYaml` validation rule (one deliberately
  broken fixture directory each, under `src/test/resources/config/`), the L2 graph derivation and
  diagram model — headless, through a recording `ICanvas` — and the network/string helpers.
  Several documented traps are pinned by name so they cannot regress unnoticed:
  `TInterface.getLongNetmask()` throwing for an explicit `netmask: 24`, `findAddress` comparing
  binary *strings* and so mis-picking the leg for an out-of-network destination, `isInNetwork`
  hard-coding /24, and `isIpAddress` accepting DNS names.
- **JaCoCo with a hard gate.** `./mvnw clean verify` now fails below 80% line coverage.
  The measured bundle excludes the Swing L2 editor, the Redmine HTTP clients, `podmancheck`,
  `CommandProcess` and thirteen CLI shims — roughly 31% of `src/main/java` that cannot be unit
  tested; each exclusion is commented in `pom.xml`. Adding a new CLI now means either testing it
  or adding it to that list. Current coverage is 92% (2048 of 2216 lines).

### Changed

- CI and the release workflow run `clean verify` instead of `clean package`, so the coverage
  gate actually runs. The fat jar is still produced by `package` and the demo-network sweep is
  unchanged.
- junit 4.10 → 4.13.2 (for `assertThrows`) plus `org.hamcrest:hamcrest`;
  `maven-surefire-plugin` pinned and run with `java.awt.headless=true`.
- `VelocityBuilderTest` and `YamlTest`, which printed to stdout and asserted nothing, were
  rewritten with assertions and moved to the packages of the classes they cover.
- **The Redmine wiki client now speaks JSON, not XML.** `RedmineEasyClient` was rewritten on
  `com.payneteasy.http-client` (the stack the issue client already used) and sends one
  `PUT <wiki-url>/<page>.json`. It reports the response body on a `409`/`422` instead of only the
  status line. Trust-all TLS is unchanged, but is now applied only to `https://` urls — a plain
  `http://` Redmine used to work by accident and would otherwise have started failing.
- `IConfigDao.persistPagesHistory()` throws `IOException` rather than `FileNotFoundException`
  (it closes the writer through try-with-resources now).
- **The bundled Maven wrapper moved 3.5.2 (2017) → 3.9.16.** It was the reason every modern build
  plugin was unusable: `maven-assembly-plugin` 3.8.0 and `maven-surefire-plugin` 3.5.6 both refuse to
  run below Maven 3.6.3. It also changes the super-POM defaults for the three plugins pinned nowhere
  in `pom.xml` — `maven-compiler-plugin` 3.1 → 3.13.0, `maven-jar-plugin` 2.4 → 3.4.1,
  `maven-resources-plugin` 2.6 → 3.3.1. 3.9.16 is the ceiling: Maven 4 requires Java 17.
- The first round of Dependabot updates landed: `snakeyaml` 2.4 → 2.6, `maven-assembly-plugin`
  3.3.0 → 3.8.0, `jacoco-maven-plugin` 0.8.12 → 0.8.15, `maven-surefire-plugin` 2.22.2 → 3.5.6, and
  the three pinned actions — `actions/checkout` → v7.0.1, `actions/setup-java` → v5.6.0,
  `actions/deploy-pages` → v5.0.0, which together clear the Node 20 deprecation warning CI had
  started emitting on every run. The golden files, the 210 tests and the `pages_history.yml` dump
  format are all unchanged, and the JaCoCo bundle still measures the same 83 classes — worth
  checking, because a surefire major bump is exactly what could have silently dropped the
  jacoco `argLine` and taken coverage to zero.
- Two Dependabot proposals were rejected rather than merged, both recorded as ignore rules with the
  reasoning in `.github/dependabot.yml`: `slf4j-nop` 2.0.18 (green CI, but slf4j 2.x providers are
  found through `ServiceLoader`, which the `slf4j-api` 1.7.36 that Velocity brings never calls — the
  jar would have silenced nothing) and `hamcrest-library` 3.0, which cannot compile because that
  artifact is an empty stub since hamcrest 2.x.
- `hamcrest-library` 1.3 → `org.hamcrest:hamcrest` 3.0, with `hamcrest-core` excluded from junit so
  the `org.hamcrest.*` classes are not on the test classpath in two versions at once. Since
  hamcrest 2.x the `-library` and `-core` artifacts are empty stubs, so bumping `hamcrest-library`
  on its own cannot compile. No test file changed.

### Security

- **Every dependency with a known CVE was updated**, and the whole `google-http-client` chain —
  the largest source of them — was removed:

  | Dependency | Change | Closes |
  |---|---|---|
  | `snakeyaml` | 1.11 → 2.4 | CVE-2022-1471 (RCE via `Constructor`), CVE-2017-18640, CVE-2022-38749…38752 |
  | `velocity` 1.7 → `velocity-engine-core` 2.4.1 | new artifact | CVE-2020-13936, plus `commons-collections` 3.2.1 and `commons-lang` 2.4 dropped |
  | `guava` | 13.0.1 → 33.6.0-jre | CVE-2018-10237, CVE-2020-8908, CVE-2023-2976 |
  | `google-http-client` 1.13.1-beta | **removed** | with it `httpclient` 4.0.1 (CVE-2012-6153, CVE-2014-3577, CVE-2020-13956), `guava-jdk5` 13.0, `commons-codec` 1.3, `commons-logging` 1.1.1, `xpp3`, `jsr305` 1.3.9 |
  | `commons-lang3` | 3.7 → 3.20.0 | CVE-2025-48924 |

  `gson` 2.11.0 → 2.14.0, `picocli` 4.6.2 → 4.7.7, `lombok` 1.18.32 → 1.18.46, mustache
  `compiler` 0.9.0 → 0.9.14 and `jfreesvg` 3.2 → 3.4.4 came along for hygiene. Every version is
  the newest that still targets Java 8. `slf4j-nop` is new, to keep Velocity 2 quiet.

  The generated output is byte-for-byte unchanged: all golden files still match, including from
  the fat jar, and `pages_history.yml` round-trips in the same format.
- **Dependabot** is enabled for maven, npm (`website/`) and the pinned GitHub Actions
  (`.github/dependabot.yml`), weekly, with minor and patch updates grouped. Major bumps that would
  break Java 8 (`jfreesvg` 4.x) or the JUnit 4 requirement are ignored, and so is `slf4j-nop`:
  it exists only to silence Velocity, so it has to match the `slf4j-api` that
  `velocity-engine-core` brings in (1.7.36). slf4j 2.x finds its provider through `ServiceLoader`,
  which the 1.7 api never calls — a 2.x nop would sit on the classpath silencing nothing, with a
  green build throughout.
- Velocity 2 needs `parser.allow_hyphen_in_identifiers` because every `iptables.vm` context key is
  hyphenated, and `parser.space_gobbling=bc` to keep the 1.7 whitespace. Both are set in
  `VelocityBuilder` and are load-bearing.
- snakeyaml 2.x refuses global `!!com.foo` tags, which `pages_history.yml` uses. Every `Yaml` is now
  built by the new `util/Yamls`, which allows exactly the `com.payneteasy.firewall.` prefix.

## [1.2.0] — 2026-07-29

First automated release. Every generator is now exercised by CI against a public demo network,
and the fat jar is downloadable instead of being built by hand.

### Added

- **Podman security-check generator.** `MainPodmanCheckTable` renders the results of a
  container hardening review as a Redmine table, `MainPodmanCheckHardningGuide` renders the
  hardening guide itself. Findings can be exported as Redmine issues through a new
  `RedmineIssueClient` (`podmancheck/`, `redmine/`).
- **`--filter` for the L3 diagram.** `MainL3Diagram` now accepts a network filter
  (`--filter internal,ipmi`) so a large estate can be drawn one segment at a time;
  `CreateL3Diagram` was reworked around it.
- **Documentation site.** A full Astro/Starlight site under `website/`, published to
  <https://evsinev.github.io/firewall-config/>: the YAML input format, every generator, the
  packet-derivation internals and the known limitations.
- **`examples/demo-network`.** A complete fictional estate — VRRP firewall pair, DMZ proxy,
  application and database servers, a core switch, an external peer — exercising SNAT, DNAT,
  service-name access lists, blocked addresses, custom rules and MSS clamping. Every example
  in the documentation comes from it, and CI regenerates all of it on every push, which makes
  it the project's end-to-end test.
- **Release automation.** Pushing a semver tag builds the fat jar and publishes a GitHub
  release with the notes from this file. Jars are attached as
  `firewall-config-<version>.jar`.

### Changed

- `./mvnw clean package` now produces the fat jar on its own, at the stable path
  **`target/firewall-config.jar`**. The `maven-assembly-plugin` is bound to the `package`
  phase and pinned to 3.3.0; `assembly:single` no longer has to be named explicitly, and the
  filename no longer carries the project version.

### Fixed

- **Custom rules in the FORWARD chain.** `TCustomRule` gained the missing fields and
  `iptables.vm` emits them, so a custom rule targeting FORWARD is no longer dropped.
- SNAT exceptions extended to cover newly added hosts (`PacketServiceImpl`).

### Removed

- Dead Travis CI and CircleCI configuration, superseded by GitHub Actions.

## [1.1-1] — 2024-02-09

Eight years of accumulated work, released as a single hand-uploaded jar
(`firewall-config-1.1-1.jar`). Highlights, grouped by area.

### Added

- **L2 topology.** A layer-2 scheme generator with an interactive Swing editor
  (`MainL2DiagramEditor`), plus SVG and PNG renderers (`MainL2SvgDiagram`,
  `MainL2PngDiagram`). Manual corrections live in a `current-l2-additions.yml` overlay —
  added links are drawn thick and red — and node positions persist between runs.
- **L3 diagram.** nwdiag output, merging of a hand-written `nwdiag-custom.diag`, and capture
  of the generator output for CI.
- **Label printing.** Host labels, patch-cord labels and rack stickers
  (`MainHostLabels`, `MainL2Labels`, `MainL2WireLabels`).
- **External-services audit report** (`MainExternalServices`) — every service reachable from
  outside, with its justification.
- **Blocking remote hosts** — explicit deny entries for remote addresses.
- **Full ICMP handling** — `echo-request`, `echo-reply`, `destination-unreachable` and
  `time-exceeded` are allowed instead of ICMP being blanket-dropped.
- **MSS clamping** per interface.
- **Custom rules** — free-form iptables lines attached to a host.
- Service justification and description fields, surfaced on the generated wiki pages.
- Software Cards parser and a Redmine textile report builder, with tests.

### Changed

- TCP packets with ACK/RST are accepted in the OUTPUT chain, and rejects use
  `--reject-with tcp-reset` instead of a silent drop.
- Bond interfaces are no longer drawn on the L2 diagram; `eth0.`, `eth1.`, `eth4.` and
  `nuc_ipmi` ports are skipped.
- `RedmineEasyClient` updated for the Redmine 4.2.10 REST wiki API, and the wiki XML format
  fixed.
- Build switched to the Maven wrapper (`./mvnw`).

### Fixed

- Forwarding when source and destination are in the same network, and when the destination
  service lives in a different network from the destination host.
- Routing lookups by virtual address; NPEs on hosts with no links and on incomplete
  interface definitions.
- Forced SNAT and several NAT exceptions.

### Removed

- The proprietary `com.yworks.yfiles` dependency — the L2 editor is drawn with plain Swing
  and JFreeSVG.

## [1.1-SNAPSHOT] — 2016-02-05

First public release. Assets `config-1.1-4.jar` … `config-1.1-7.jar` were uploaded by hand.
Covers the project from its first commit in January 2013.

### Added

- **The iptables generator.** A YAML description of hosts, interfaces, networks and protocols
  is turned into an `iptables-save` file per host: INPUT, OUTPUT and FORWARD rules derived
  from `access:` declarations, SNAT and DNAT from `nat:`, matching reverse rules for every
  forwarded packet, and a trailing REJECT for each chain. Access lists are written in terms
  of service names, and a whole host group can be generated at once (`group-internal`).
- `icmp`, `esp` and `ah` alongside TCP and UDP; deterministic output ordering by destination
  name and no generation timestamp, so regenerating produces a reviewable diff.
- **`services_links`** — source and destination services as first-class inputs to the
  generator.
- **Redmine wiki publishing** (`MainWiki`, `MainWikiDir`) with a Redmine client, uploading a
  page per host and network.
- **bind zone files**, forward and reverse (`MainBind`).
- **keepalived / VRRP** (`MainKeepalived`): virtual addresses, linked VRRP instances, VRRP
  packets allowed through the firewall, and NAT performed through a virtual address so it
  survives failover.
- **Diagrams and switch configuration**: L1 and L2 diagrams, Mikrotik trunk configuration
  (`MainMikrotik`), CentOS network interface scripts, per-host colours and `ipmi_ip`.

[1.3.0]: https://github.com/evsinev/firewall-config/releases/tag/1.3.0
[1.2.0]: https://github.com/evsinev/firewall-config/releases/tag/1.2.0
[1.1-1]: https://github.com/evsinev/firewall-config/releases/tag/1.1-1
[1.1-SNAPSHOT]: https://github.com/evsinev/firewall-config/releases/tag/1.1-SNAPSHOT
