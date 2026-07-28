# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

A note on version numbers: `pom.xml` has carried `1.1-SNAPSHOT` since 2013 and the published
releases were numbered separately by hand (`1.1-4` … `1.1-7`, then `1.1-1`). From `1.2.0`
onwards the release version comes from the git tag — pushing a tag like `1.2.0` builds the fat
jar and publishes it as `firewall-config-1.2.0.jar`
(see [`.github/workflows/release.yml`](.github/workflows/release.yml)).

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

[1.2.0]: https://github.com/evsinev/firewall-config/releases/tag/1.2.0
[1.1-1]: https://github.com/evsinev/firewall-config/releases/tag/1.1-1
[1.1-SNAPSHOT]: https://github.com/evsinev/firewall-config/releases/tag/1.1-SNAPSHOT
