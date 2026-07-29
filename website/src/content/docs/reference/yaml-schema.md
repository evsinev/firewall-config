---
title: YAML schema
description: Every field of every configuration file, with types, defaults and requirements.
sidebar:
  order: 2
---

Flat reference for the whole input format. For explanation and examples see
[Hosts and interfaces](/firewall-config/configuration/hosts/) and
[Services and access](/firewall-config/configuration/services/).

Files are parsed with SnakeYAML into plain Java classes, so **unknown keys are an error, not a
warning** — a typo in a field name fails the run rather than being silently ignored:

```
Could not read file db-1.yml: null; … Unable to find property 'typoField' on class:
com.payneteasy.firewall.dao.model.THost
```

## Host file — `hosts/<group>/<host>.yml`

| Field | Type | Default | Required | Notes |
|---|---|---|---|---|
| `gw` | address | — | **yes** | Default gateway; also defines which host routes for this one. May point at an undescribed router. |
| `interfaces` | list of [Interface](#interface) | — | **yes** | Order matters: the first with a real address is the default address. |
| `description` | string | — | recommended | Shown in wiki pages and audit tables. |
| `justification` | string | — | recommended | Becomes the "Business goal" section of the wiki page. |
| `services` | list of [Service](#service) | `[]` | no | |
| `services_links` | string | — | no | Service `name`s to reuse; separators are space, comma, tab, semicolon. |
| `color` | `"0xRRGGBB"` | pale beige | no | L2 diagram box and printed host label. |
| `blockedIpAddresses` | list of [BlockedIpAddress](#blockedipaddress) | — | no | |
| `customRules` | list of [CustomRule](#customrule) | — | no | |
| `external_peer` | boolean | `false` | no | The host is outside our address space despite a private address (a partner behind a tunnel). Traffic *towards* it is SNAT-ed to the service's `nat:` address, as for a public destination. |

`name` and `group` are derived from the file name and its parent directory and must not appear in the
file.

### Interface

| Field | Type | Default | Notes |
|---|---|---|---|
| `name` | string | — | Kernel interface name; used verbatim as `-i` / `-o`. |
| `ip` | address or `skip` | — | `skip` = exists at L2, no address. |
| `netmask` | `16` | `24` | **Only absent and `16` are supported**; any other value throws `Netmask not supported yet: …`. |
| `dns` | string | — | Space-separated DNS names for this address. |
| `vip` | address | — | One VRRP virtual address. Must have exactly one peer elsewhere. |
| `vips` | list of [VirtualIpAddress](#virtualipaddress) | — | Several virtual addresses, each with names. |
| `vrrpPriority` | number | `100` | keepalived priority. |
| `vlan` | number or `trunk` | — | Drives L2 colouring and switch configuration. |
| `port` | number | — | Physical switch port. |
| `link` | `<host>/<iface>` | — | Far end of the cable. Declare each cable on one side only. |
| `mss` | number | — | Emits `TCPMSS --set-mss <n>` for traffic from this address. |

### VirtualIpAddress

| Field | Type | Notes |
|---|---|---|
| `ip` | address | The virtual address. |
| `names` | string | Space-separated DNS names, resolvable from `url:` and `nat:`. |

### Service

| Field | Type | Required | Notes |
|---|---|---|---|
| `url` | string | **yes** | `<protocol>`, `<protocol> <port>`, `<protocol>://<address>`, `<protocol>://<address>:<port>`. |
| `access` | list of string | **yes** | Exact host, `group-<name>`, `<service-name>[.service]`, or `<prefix>-*`. |
| `name` | string | for reuse | Globally unique. Required to be referenced by `services_links` or by `<name>.service`. |
| `description` | string | recommended | Falls back to the protocol's description. |
| `justification` | string | recommended | Audit evidence. |
| `nat` | string | when NAT applies | Public URL, same grammar as `url`. |
| `program` | string | no | Overrides the protocol's `program`. |
| `tags` | list of string | no | Only read by `MainExternalServices`. |
| `external` | string | no | Free text, printed by `MainExternalServices`. |
| `dip` | string | — | Parsed but unused. |

### BlockedIpAddress

| Field | Type | Default | Notes |
|---|---|---|---|
| `ip` | address | — | Emitted at the top of `INPUT` and `FORWARD`. |
| `reason` | string | — | Becomes the comment above the rule. |
| `type` | `REJECT` \| `DROP` | `REJECT` | |

### CustomRule

| Field | Type | Required | Notes |
|---|---|---|---|
| `chain` | `INPUT` \| `OUTPUT` \| `FORWARD` \| `PREROUTING` \| `POSTROUTING` | **yes** | Omitting it throws `No chain type for host … and custom rule …`. |
| `rule` | string | **yes** | A complete `iptables-save` line, emitted verbatim, including its own `-A <CHAIN>`. |
| `description` | string | recommended | Comment above the rule. |
| `justification` | string | recommended | Comment above the rule. |

## `protocols.yml`

```yaml
protocols:
- name: ssh
  protocol: tcp
  port: 22
  program: sshd
  description: Secure Shell
  justification: Encrypted administrative access
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | **yes** | What `url:` refers to. |
| `protocol` | `tcp` \| `udp` | **yes** | Validated non-null. |
| `port` | number | **yes** | Validated non-zero. Overridable inline. |
| `program` | string | recommended | The listening daemon. |
| `description` | string | recommended | Inherited by services. |
| `justification` | string | recommended | Inherited by services. |

## `networks.yml`

```yaml
networks: {
  198.51.100.0 : internet,
  10.20.20.0   : app
}
```

A map from `/24` **network address** to name. Every `/24` in use must be present. A name starting with
`skip` excludes the network from the L3 diagram. `internet` is always sorted first.

## `<prefix>-l2-additions.yml`

```yaml
vlanColors: { trunk: 0xEF9A9A, 202: 0xA5D6A7 }
addedLinks:
  - sw-core-1/ether11 (206) >>>> ipmi-kvm-1/eth0 (206)
removedLinks: []
```

| Field | Type | Notes |
|---|---|---|
| `vlanColors` | map VLAN → `0xRRGGBB` | Anything `java.awt.Color.decode` accepts. |
| `addedLinks` | list of string | `<host>/<port> (<vlan>) >>>> <host>/<port> (<vlan>)`; VLANs optional. Creates hosts/ports that do not exist. |
| `removedLinks` | list of string | Same syntax; suppresses a cable from `hosts/`. |

The file must exist for any L2 command, even if all three keys are empty.

## `<prefix>-l2-positions.properties`

Written by the L2 editor. Keys are `<host>` for a host box and `<host>.<port>` for a port; values are
`x, y`.

```properties
sw-core-1=13, 1583
fw-1.eth0=16, 304
```

## `pages_history.yml`

Written by `MainWiki`; a SnakeYAML dump carrying a Java type tag. Local state — do not edit or commit.

```yaml
!!com.payneteasy.firewall.dao.model.TPagesHistory
lastUpdateDate: 2026-07-29T00:23:11.902Z
pageHistories:
- {pageHash: -1811445625, pageName: db-1_packets}
```

## Next

- [CLI reference](/firewall-config/reference/cli/)
- [Rule derivation](/firewall-config/internals/packet-derivation/)
