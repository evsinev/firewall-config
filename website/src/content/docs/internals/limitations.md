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

Until this is fixed, pin the service to an explicit address (`url: postgres://10.20.20.21`) or keep
multi-homed hosts' extra legs inside networks they genuinely talk to directly.

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
- **The fat jar needs an explicit goal.** The assembly plugin is not bound to a lifecycle phase, so
  `./mvnw package` alone produces no runnable jar — you need
  `./mvnw clean package assembly:single`.
- **Releases are copies.** There is no published CLI artifact; the jar is committed into each
  description repository. Reproducible, but it means every site can be on a different version.
- **Thin tests.** Only `findAddress`, the YAML round-trip, a Velocity smoke test and the
  critical-software parser are covered. There is **no end-to-end test** over a sample configuration
  directory — [`examples/demo-network`](/firewall-config/quick-start/) exists precisely so that such a
  test would be easy to add, and running the generators against it is currently the closest thing to
  one.

## Next

- [Rule derivation](/firewall-config/internals/packet-derivation/) — the behaviour these caveats qualify.
- [Architecture](/firewall-config/internals/architecture/) — where in the code each item lives.
