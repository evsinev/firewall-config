---
title: Configuration directory
description: The layout of a network description — what each file is for and where host names and groups come from.
sidebar:
  order: 1
---

The input to every generator is a **configuration directory**: a plain directory of YAML files that
describes one site. It is not part of this repository. In practice it is a git repository of its own
per site — `firewall-moscow`, `firewall-ams-2`, and so on — with the
[fat jar committed alongside it](/firewall-config/installation/#how-it-is-released).

## Layout

```
<config-dir>/
├── hosts/                              # loaded recursively
│   ├── internal/
│   │   ├── fw-1.yml
│   │   ├── web-1.yml
│   │   └── db-1.yml
│   ├── ipmi/
│   │   └── sw-core-1.yml
│   └── external/
│       └── partner-api.example.com.yml
├── protocols.yml                       # required: named protocol/port catalogue
├── networks.yml                        # required for the L3 diagram: /24 -> human name
├── nwdiag-custom.diag                  # required for the L3 diagram: injected verbatim
├── current-l2-additions.yml            # required for any L2 command
├── current-l2-positions.properties     # written by the L2 editor
├── pages_history.yml                   # written by MainWiki
├── firewall-config.jar                 # by convention, committed with the description
└── firewall-*.sh                       # by convention, the wrapper scripts
```

| File | Required by | Reference |
|---|---|---|
| `hosts/**/*.yml` | everything | [Hosts and interfaces](/firewall-config/configuration/hosts/) |
| `protocols.yml` | everything | [Protocols and networks](/firewall-config/configuration/protocols-and-networks/) |
| `networks.yml` | `MainL3Diagram` | [Protocols and networks](/firewall-config/configuration/protocols-and-networks/) |
| `nwdiag-custom.diag` | `MainL3Diagram` | [L3 diagram](/firewall-config/generators/l3-diagram/) |
| `<prefix>-l2-additions.yml` | all L2 commands | [L2 additions and positions](/firewall-config/configuration/l2-additions/) |
| `<prefix>-l2-positions.properties` | L2 diagrams (optional) | [L2 additions and positions](/firewall-config/configuration/l2-additions/) |
| `pages_history.yml` | `MainWiki` (optional) | [Redmine wiki](/firewall-config/generators/redmine-wiki/) |

`protocols.yml` and `hosts/` are read by the constructor of the configuration loader, so **every**
command fails immediately if either is missing or invalid — even commands that would not otherwise
need them.

## Host names and groups come from the filesystem

This is the one rule that surprises everyone: a host file never states its own name or group.

```
hosts/internal/db-1.yml   ->  host name "db-1", group "internal"
```

The file name minus `.yml` is the host name; the name of the containing directory is the group.
Renaming a file renames the host everywhere it is referenced. `hosts/` is walked recursively, but
only the *immediate* parent directory becomes the group, so nesting deeper than one level produces
groups you probably did not intend.

Host names are used verbatim in `access:` lists, in `group-<name>` expansion, in the `--filter`
option of the L2/L3 commands and as the output file name of the iptables generator. Use the host's
real DNS short name; for external peers, the convention is to use the FQDN as the file name
(`partner-api.example.com.yml`).

## What groups are used for

Groups are not access-control zones — they are just labels — but a lot hangs off them:

- `access: [group-internal]` grants access to every host in the group.
- `java -jar firewall-config.jar . group-internal gen` generates a rule set for each host in the group.
- `--filter internal,ipmi` selects which hosts appear in the L2/L3 diagrams. The filter also matches
  host-name prefixes, so `--filter internal,fw-` works too.
- `MainWiki` writes one `<group>_group` overview page per group.
- `MainBind` and `MainPacketsGraphviz` **only** look at the group literally named `internal`.

A conventional split is `internal` (hosts you generate rules for), `ipmi` (out-of-band equipment:
switches, IPMI controllers), `external` (third-party peers you do not control), and `vpn`.

## Suggested repository layout

```
firewall-<site>/
├── hosts/ protocols.yml networks.yml nwdiag-custom.diag
├── current-l2-additions.yml
├── current-l2-positions.properties
├── firewall-config.jar
├── firewall-config.sh          # iptables -> ../ansible-<site>/roles/iptables/files/gen
├── firewall-l3.sh              # -> images/l3.svg
├── firewall-l2-svg.sh          # -> images/l2.svg
├── wiki-config.sh              # -> Redmine
├── images/                     # committed diagrams, for the wiki and for humans
└── .gitlab-ci.yml              # regenerate everything on every push
```

Keeping the generated `iptables` files in a *separate* Ansible repository, and the diagrams as
committed SVGs in this one, means a review of the description shows the resulting rule diff too.

## Next

- [Hosts and interfaces](/firewall-config/configuration/hosts/)
- [Services and access](/firewall-config/configuration/services/)
