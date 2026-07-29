---
title: Limitations
description: The hard-coded assumptions, sharp edges and known gaps, collected in one place.
sidebar:
  order: 3
---

Honest inventory of where the tool leaks. None of it is a blocker in practice — the estates it was
written for run on it — but every item here has surprised somebody.

## Configuration model

### Only `/24` and `/16` netmasks

`netmask:` accepts nothing except absent (meaning `/24`) and the literal `16`. Anything else throws:

```
Netmask not supported yet: 25. Please add support to TInterface.getLongNetmask() method
```

Worse, the `/24` assumption is not confined to that one method: `networks.yml` is keyed by `/24`, the
L3 diagram groups by `/24`, and the "same network" suppression filter compares address strings up to
the last dot. A `/25` or `/23` allocation cannot be described faithfully — split it into `/24`s, or
accept that two hosts in different halves of a `/24` are treated as adjacent.

Note that the *supported* `/24` case is the absent one: writing `netmask: 24` explicitly throws too.
Both behaviours are pinned by `ModelBeansTest.onlyAnAbsentNetmaskMeansSlash24` /
`anExplicitSlash24NetmaskThrows`, and the string comparison by `NetworksTest`, so a fix has to
update those tests deliberately rather than trip over them.

### SNAT for private ranges is hard-coded

The SNAT decision is `isPublicAddress(destination)` — *plus a literal list of private addresses* baked
into `PacketServiceImpl`, marked `// todo hot fix for SNAT`:

```java
if(isPublicAddress(destinationHost.getDefaultIp())
        || destinationHost.getDefaultIp().equals("10.12.12.50")
        || destinationHost.getDefaultIp().equals("10.170.1.1")
        …
        || destinationHost.getDefaultIp().startsWith("172.16.4.")
        ) { // todo hot fix for SNAT
```

These are real addresses from the sites the tool was written for: partner networks reached over
tunnels, where traffic must be translated even though both ends are RFC 1918. **Adding a
SNAT-ed private network today requires editing Java and rebuilding the jar** — it cannot be expressed
in YAML. If you adopt the tool for a different estate, this list is the first thing to make
configurable.

### `findAddress` is not a routing lookup

For a multi-homed source, the address used in the rule is the one sharing the longest **binary prefix**
with the destination. That ignores netmasks and the actual route table, so it is right whenever the
source has a leg in or near the destination's network and can be wrong otherwise.

Concretely: a host with legs in `10.20.20.0/24` and `10.20.6.0/24`, reaching `10.20.2.21`, gets
`10.20.6.31` in the rule — because `6` and `2` share more leading bits than `20` and `2` do — while
Linux will actually send from `10.20.20.31` via the default gateway. The rule then never matches.

The "binary prefix" is in fact computed on the *decimal* rendering of
`Integer.toBinaryString()`, which drops leading zeros, so the comparison is not even a true prefix
length — see `PacketServiceImplTest.testFindAddress`, which pins the current answer for an
out-of-network destination.

Until this is fixed, pin the service to an explicit address (`url: postgres://10.20.20.21`) or keep
multi-homed hosts' extra legs inside networks they genuinely talk to directly.

### Two outputs depend on filesystem listing order

`ConfigDaoYaml` loads hosts in `File.listFiles()` order and never sorts them. Almost everything
downstream sorts before rendering — the packet lists end in `Collections.sort`, access lists go
through a `TreeSet`, the bind forward and reverse zones through a `TreeSet`/`TreeMap` — but two
outputs render hosts in load order directly:

- **`static.zone`** — `MainBind.createServiceZone` collects records into an `ArrayList`.
- **the `<group>_group.wiki` pages** — `WikiServiceImpl.createServersPage` walks `listHosts()`.

Regenerating the same description on macOS (APFS) and Linux (ext4) therefore produces the same
records in a different order in those two files, which shows up as noise in the review diff. The
tests compare them as sets for this reason (`MainBindTest`, `MainWikiTest`). Sorting them in the
generator would be a one-line change, but it rewrites those two files for every existing estate at
once, so it is deliberately left alone.

### Shared named services collide in `static.zone`

`services_links` copies a *named* definition onto many hosts, and `MainBind`'s `static.zone` emits one
`A` record per service `name` per host. Shared base services therefore produce duplicates:

```text
ssh       IN A 10.20.20.11
ssh       IN A 10.20.20.12
ssh       IN A 10.20.2.21
```

Valid DNS, almost certainly not intended. Serve `static.zone` selectively.

### Fields that do nothing

- `dip` on a service is parsed and never used.
- `vrrp-packets` and `linked-vrrp-packets` are computed for every host and passed to `iptables.vm`,
  which does not reference them. VRRP advertisements are consequently **not** permitted by the
  generated rule set — allow protocol 112 to `224.0.0.18` with a
  [`customRules`](/firewall-config/configuration/hosts/#custom-rules) entry if your chain policy would
  otherwise drop them.

### `vrrpPriority` does not reach `keepalived.conf`

`keepalived.mustache` deliberately switches Mustache delimiters so that `{{vrrp_priority}}` is emitted
*literally*, as a placeholder for a downstream templating pass. The `vrrpPriority:` value from the
description is computed and then discarded by the template, and `auth_pass` is the fixed placeholder
`1111`.

There is also a `checkPairedInterface` routine that would reject a VRRP pair whose two members share a
priority — but the call to it is commented out, so that mistake is not caught.

## Command-line surface

### Two conventions, one jar

The older commands take strictly positional arguments, have no `--help`, and report a wrong argument
count as a one-line exception. The newer ones use picocli with `--help` and named options. There is no
single dispatcher: you launch a class by name.

### Nothing creates directories

Output directories must exist. Several commands write to paths fixed relative to the current working
directory — `target/network.diag`, `target/hosts.html`, `target/labels.html`, `test.svg`,
`<prefix>-l2.svg` — so the working convention is to run from inside the configuration directory after
`mkdir -p target`. In CI this is the most common first failure.

### `<prefix>` is required even when unused

Every picocli command inherits `<dir> <prefix>`, so `MainL3Diagram` and `MainExternalServices` demand a
prefix they never read. Pass `current`.

### Site-specific hard-coding

| Command | Assumption |
|---|---|
| `MainBind`, `MainPacketsGraphviz` | Only the group literally named `internal` |
| `MainUbuntuBaseSetup` | `search idea`, `nameserver 10.2.2.21`, `nameserver 10.2.2.22` |
| `MainPacketsGraphviz` | Drops `ntp`, `dns` and some site-specific host prefixes |
| `MainL2Labels` | Rewrites/shortens some host names to fit a label |
| `MainKeepalived` | `auth_pass 1111`, `router_id LVS_DEVEL` |
| `getInputPackets` | Skips an interface literally named `ipmi_nuc` |

## Security

- **`RedmineEasyClient` disables TLS verification** — it installs a trust-all `X509TrustManager`.
  Anything pushed to Redmine (`MainWiki`, `CritSoftCollectorMain`, the podman issue creation) travels
  over a connection whose certificate is not checked.
- **API keys are positional arguments**, so they appear in shell history and `ps` output, and in the
  usual arrangement they live in a wrapper script committed to the description repository. Pass them
  from CI secrets and scope the Redmine key narrowly.
- `MainPodmanCheckHardningGuide --redmine-enabled` **creates issues** on every run, with no
  deduplication — re-running duplicates them.

## Build and release

- **Java 8.** `source`/`target` are `1.8`. The code itself is modest (streams, `StringJoiner`), so
  raising the level is plausible, but the Swing/AWT rendering path and the ancient dependency set
  (`snakeyaml` 1.11, `velocity` 1.7, `guava` 13, `google-http-client` 1.13-beta) make it a real
  upgrade rather than a flag change.
- **The version in `pom.xml` is meaningless.** It has been `1.1-SNAPSHOT` since 2013; the release
  version comes from the git tag and is stamped into the jar by CI. Nothing is published to a Maven
  repository, so the artifact is only ever consumed as a jar file.
- **Releases are still copies in practice.** The jar is published on
  [Releases](https://github.com/evsinev/firewall-config/releases), but the convention remains that
  each description repository commits the jar it was generated with. Reproducible, but it means
  every site can be on a different version.
- **Coverage is measured over a reduced bundle.** `./mvnw clean verify` fails below 80% line
  coverage, but roughly 31% of `src/main/java` is excluded from the denominator because it cannot be
  unit tested: the Swing L2 editor, the Redmine HTTP clients, `podmancheck`, the `nwdiag`
  subprocess wrapper and thirteen CLI shims that call `System.exit`. The exclusion list lives in
  `pom.xml` with a comment per entry. The headline number is therefore not whole-tree coverage, and
  the excluded code is exercised only by the demo-network sweep, which checks exit codes and not
  output.
- **Golden files are the regression net.** The output of the iptables, wiki, bind, keepalived,
  RouterOS and nwdiag generators is compared byte for byte against
  `src/test/resources/golden/`, so a silently changed rule now fails the build. The cost is that a
  deliberate change requires regenerating those files and reviewing the diff — see the *Verify a
  change* section of `CLAUDE.md`. Bind zone files are compared by record lines only, because their
  headers carry a timestamp, the local hostname and `user.name`.

## Next

- [Rule derivation](/firewall-config/internals/packet-derivation/) — the behaviour these caveats qualify.
- [Architecture](/firewall-config/internals/architecture/) — where in the code each item lives.
