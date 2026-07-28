[![Build](https://github.com/evsinev/firewall-config/actions/workflows/maven.yml/badge.svg)](https://github.com/evsinev/firewall-config/actions/workflows/maven.yml)
[![Docs](https://github.com/evsinev/firewall-config/actions/workflows/docs.yml/badge.svg)](https://github.com/evsinev/firewall-config/actions/workflows/docs.yml)

# firewall-config

📖 **Full documentation: https://evsinev.github.io/firewall-config/**

Turns a YAML description of a network into everything that has to agree with it: `iptables` rules for
every host, L2 and L3 diagrams, DNS zones, keepalived and switch configuration, and the audit
documentation that proves the whole thing matches.

The premise is that **no firewall rule is ever written by hand**. You describe hosts, the services they
listen on, and who may reach them; the generator works out which chain a packet belongs in, which
interface it enters and leaves, whether it needs SNAT or DNAT, and which rules are redundant because
the two hosts already share a subnet.

## Generators

The input is a separate directory — one git repository per site — described under
[Configuration](https://evsinev.github.io/firewall-config/configuration/config-directory/).

| Generates | Command | Docs |
|---|---|---|
| `iptables-save` file per host | `java -jar firewall-config.jar <dir> group-internal <out>` | [iptables](https://evsinev.github.io/firewall-config/generators/iptables/) |
| L3 network diagram (nwdiag) | `MainL3Diagram <dir> <prefix>` | [L3 diagram](https://evsinev.github.io/firewall-config/generators/l3-diagram/) |
| L2 cabling diagram (SVG/PNG/editor) | `MainL2SvgDiagram <dir> <prefix>` | [L2 diagram](https://evsinev.github.io/firewall-config/generators/l2-diagram/) |
| Redmine audit wiki | `MainWiki <url\|dir> <key> <dir>` | [Redmine wiki](https://evsinev.github.io/firewall-config/generators/redmine-wiki/) |
| BIND zones, keepalived, `ifcfg-*`, RouterOS | `MainBind`, `MainKeepalived`, `MainMikrotik`, … | [Host provisioning](https://evsinev.github.io/firewall-config/generators/host-provisioning/) |
| Printable host, port and cable labels | `MainHostLabels`, `MainL2Labels`, `MainL2WireLabels` | [Labels](https://evsinev.github.io/firewall-config/generators/labels/) |
| External services, podman hardening, software inventory | `MainExternalServices`, `MainPodmanCheckTable`, … | [Audit reports](https://evsinev.github.io/firewall-config/generators/audit-reports/) |

All 21 entry points and their arguments are listed in the
[CLI reference](https://evsinev.github.io/firewall-config/reference/cli/).

## Quick example

`db-1` says who may talk to PostgreSQL — not by address, but by naming the application:

```yaml
# hosts/internal/db-1.yml
gw: 10.20.22.1

interfaces:
- name: eth0
  ip:   10.20.22.21

services:
- url:           postgres
  name:          main-db
  justification: The web application stores transaction data here
  access:        [web-app.service, adm-1]
```

`gen/db-1` gets the INPUT rules, and `gen/fw-1` — the firewall between the two subnets, mentioned
nowhere — gets the matching FORWARD pair:

```sh
# web-1 -> postgres://db-1   web-app --> main-db
-A FORWARD -s 10.20.20.21 -d 10.20.22.21 -i eth0.202 -o eth0.203 -p tcp -m tcp --dport 5432 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT
-A FORWARD -s 10.20.22.21 -d 10.20.20.21 -i eth0.203 -o eth0.202 -p tcp -m tcp --sport 5432 -m state --state RELATED,ESTABLISHED -j ACCEPT
```

## Get it

Download `firewall-config-<version>.jar` from the
[latest release](https://github.com/evsinev/firewall-config/releases/latest), or build it
yourself — Java 8, no separate Maven needed:

```sh
./mvnw clean package                       # -> target/firewall-config.jar
```

Try it against the bundled demo network:

```sh
cd examples/demo-network && mkdir -p gen target
java -jar ../../target/firewall-config.jar . group-internal gen
```

[`examples/demo-network`](examples/demo-network) is a complete fictional estate — VRRP firewall pair,
DMZ proxy, application and database servers, a core switch and an external peer — that exercises SNAT,
DNAT, service-name access lists, blocked addresses, custom rules and MSS clamping. Every example in the
documentation comes from it. See the
[quick start](https://evsinev.github.io/firewall-config/quick-start/) for a walkthrough of every
generator.

## Documentation

- [Installation](https://evsinev.github.io/firewall-config/installation/) — download or build, prerequisites, how the tool is released into a config repo
- [Configuration](https://evsinev.github.io/firewall-config/configuration/config-directory/) — the YAML input format, `url:` / `access:` / `nat:`
- [Reference](https://evsinev.github.io/firewall-config/reference/cli/) — CLI and YAML schema tables
- [Internals](https://evsinev.github.io/firewall-config/internals/packet-derivation/) — how rules are derived, and the [known limitations](https://evsinev.github.io/firewall-config/internals/limitations/)

The site lives in [`website/`](website) and is deployed by
[`.github/workflows/docs.yml`](.github/workflows/docs.yml).

## Releases

[`CHANGELOG.md`](CHANGELOG.md) records what shipped in each release. Pushing a semver tag
(`git tag -a 1.2.0 -m 1.2.0 && git push origin 1.2.0`) is the whole release process —
[`.github/workflows/release.yml`](.github/workflows/release.yml) builds the fat jar, runs the demo
network through every generator, and publishes a release with the notes from that file.
