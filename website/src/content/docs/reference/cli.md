---
title: CLI reference
description: Every entry point, its arguments and its output path.
sidebar:
  order: 1
---

The jar bundles 21 entry points. Only one is the `Main-Class`; everything else is launched with `-cp`
and a fully-qualified class name.

```sh
JAR=target/config-1.1-SNAPSHOT-jar-with-dependencies.jar

java -jar $JAR …                                       # the iptables generator only
java -cp  $JAR com.payneteasy.firewall.MainL3Diagram …  # everything else
```

## Two argument conventions

The commands were written over a long period and come in two incompatible styles.

**Positional (`args[]`)** — the older commands. Arguments are strictly ordered, there is no `--help`,
and a wrong count produces a one-line `usage:` message from an exception. `MainWiki` is the only one
with an option, `-f`/`--force`, and it must come last.

**picocli** — the newer commands. They support `-h`/`--help`, named options, and share a base class
(`AbstractDirPrefixFilterCommand`) that defines a common contract:

```
<dir> <prefix> [-f|--filter GROUPS]
```

| | |
|---|---|
| `<dir>` | The configuration directory. |
| `<prefix>` | Prefix for the L2 side files (`<prefix>-l2-additions.yml`, `<prefix>-l2-positions.properties`) and for the output file name. Positionally required even by commands that ignore it — pass `current`. |
| `--filter` | Comma-separated list matched against **group names** and against **host-name prefixes**. Default `internal,ipmi,internet`. |

## Positional commands

| Class | Arguments | Output |
|---|---|---|
| `Main` | `<config-dir> <host\|group-NAME> <output-dir>` | `<output-dir>/<host>` — one `iptables-save` file per host. The jar's `Main-Class`. |
| `MainWiki` | `<wiki-url\|dir> <redmine-key> <config-dir> [-f\|--force]` | Redmine pages, or `.wiki` files if the first argument is not a URL |
| `MainWikiDir` | `<redmine-url> <redmine-key> <wiki-dir>` | Uploads each file in the directory as a wiki page |
| `MainBind` | `<config-dir> <domain-name> <output-dir>` | `<domain>.zone`, reverse zones, `static.zone`, `zones.conf` |
| `MainKeepalived` | `<config-dir> <host>` | `keepalived.conf` install script on stdout |
| `MainLinuxNetworkScript` | `<config-dir> <host> <iface>` | `ifcfg-<iface>` install script on stdout |
| `MainUbuntuBaseSetup` | `<config-dir> <host> <iface>` | `interfaces.d/<iface>`, `/etc/hosts`, `/etc/hostname`, `/etc/resolv.conf` on stdout |
| `MainMikrotik` | `<config-dir> <host> [vlan]` | RouterOS VLAN commands on stdout |
| `MainPacketsGraphviz` | `<config-dir>` | Graphviz edges on stdout (group `internal` only) |
| `MainHostLabels` | `<config-dir>` | `target/hosts.html` |
| `critsoft.CritSoftCollectorMain` | `<redmine-url> <redmine-page> <redmine-key> <software-cards-dir>` | Critical-software table pushed to Redmine |
| `l2.editor.create.L2CustomParameters` | — | Prints a sample `<prefix>-l2-additions.yml` |

## picocli commands

| Class | Extra options | Output |
|---|---|---|
| `MainL3Diagram` | `--run-nwdiag` (default `true`) | `target/network.diag` + stdout; optionally runs `nwdiag` and `open` |
| `MainL2SvgDiagram` | — | `<prefix>-l2.svg` |
| `MainL2PngDiagram` | — | `<prefix>-l2.png` |
| `MainL2DiagramEditor` | — | Interactive Swing editor; `s` saves `<prefix>-l2-positions.properties` |
| `MainL2Labels` | — | `target/labels.html` |
| `MainL2WireLabels` | takes only `<dir>` | `test.svg` |
| `MainExternalServices` | `-t/--tags` (default `ingress-nginx`), `-a/--access` (default `internet`) | Textile rows on stdout |
| `MainPodmanCheckTable` | `--results-dir` (**required**) | Textile matrix on stdout |
| `MainPodmanCheckHardningGuide` | `--result-file`, `--redmine-url`, `--redmine-key`, `--redmine-project`, `--redmine-parent-issue-id`, `--redmine-enabled` | Textile guide on stdout; optionally creates Redmine issues |

## Fixed output paths

Several commands ignore any notion of an output directory and write to a hard-coded path **relative to
the current working directory**. The directory must already exist.

| Path | Written by |
|---|---|
| `target/network.diag` | `MainL3Diagram` |
| `target/hosts.html` | `MainHostLabels` |
| `target/labels.html` | `MainL2Labels` |
| `test.svg` | `MainL2WireLabels` |
| `<prefix>-l2.svg` / `.png` | `MainL2SvgDiagram` / `MainL2PngDiagram` |
| `<prefix>-l2-positions.properties` | `MainL2DiagramEditor` |
| `pages_history.yml` (in the config dir) | `MainWiki` |

So the convention is to run every command **from inside the configuration directory** with `.` as
`<dir>`, after a `mkdir -p target`.

## Group-name assumptions

| Command | Assumption |
|---|---|
| `MainBind` | Only group `internal` |
| `MainPacketsGraphviz` | Only group `internal`, plus hard-coded name filters |
| everything else | Whatever `--filter` or the positional host argument selects |

## Next

- [YAML schema](/firewall-config/reference/yaml-schema/) — every field of every file.
- [Limitations](/firewall-config/internals/limitations/) — the assumptions baked into these commands.
