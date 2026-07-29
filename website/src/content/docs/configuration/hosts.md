---
title: Hosts and interfaces
description: Every field of a host file — gw, interfaces, VRRP, blocked addresses and custom rules.
sidebar:
  order: 2
---

One YAML file under `hosts/<group>/` describes one host. Servers, firewalls, switches, IPMI
controllers and third-party peers are all "hosts" — describing an external peer as a host is how you
grant *outbound* access to it.

```yaml title="hosts/internal/web-1.yml"
description:   Customer-facing web application
justification: Primary business service
color:         "0xB2DFDB"

gw: 10.20.20.1

interfaces:
- name: eth0
  ip:   10.20.20.21

- name: ipmi
  ip:   skip

services_links: ssh, node-exporter

services:
- url:           https
  name:          web-app
  description:   Customer-facing web application
  justification: Primary business service
  nat:           https://gw.demo.example.com
  access:        [partner-api.example.com, adm-*]
```

## Host fields

| Field | Type | Required | Purpose |
|---|---|---|---|
| `gw` | address | **yes** | Default gateway. Also defines *which host routes for this one* — see below. |
| `description` | text | recommended | Shown in the wiki pages and the audit tables. |
| `justification` | text | recommended | Why this host exists at all. Becomes the "Business goal" section of its wiki page. |
| `interfaces` | list | **yes** | See [Interface fields](#interface-fields). |
| `services` | list | no | What the host listens on — see [Services and access](/firewall-config/configuration/services/). |
| `services_links` | string | no | Shared service definitions to reuse, separated by space, comma or semicolon. |
| `color` | `"0xRRGGBB"` | no | Box colour in the L2 diagram and on the printed host label. Defaults to a pale beige. |
| `blockedIpAddresses` | list | no | See [Blocked addresses](#blocked-addresses). |
| `customRules` | list | no | See [Custom rules](#custom-rules). |
| `external_peer` | boolean | no | The host is not part of our address space even though its address is private — a partner reached over a tunnel. Traffic towards it is translated: see [SNAT — a private peer that is not ours](/firewall-config/configuration/services/#snat--a-private-peer-that-is-not-ours). |

`name` and `group` are **not** fields — they come from
[the file name and directory](/firewall-config/configuration/config-directory/#host-names-and-groups-come-from-the-filesystem).

### `gw` is the most important field

`gw` does double duty. It is the host's default gateway, and it is the only thing that tells the
generator about routing topology: to find everything a firewall forwards for, the tool inverts the
relation — *which hosts name one of my addresses (real or virtual) as their `gw`?*

```yaml
# db-1
gw: 10.20.22.1        # -> the VRRP address of fw-1/fw-2 eth0.203
```

Because `10.20.22.1` is a virtual address on `fw-1` and `fw-2`, both firewalls now know they route
for `db-1`, and FORWARD rules for `db-1`'s services appear in both rule sets. Get `gw` wrong and you
get either no FORWARD rules at all or
`Can't find interface at host X which connected to Y`.

A `gw` that belongs to no described host is fine and normal — that is how you model the upstream ISP
router:

```yaml
# fw-1
gw: 198.51.100.1      # the ISP, deliberately not described as a host
```

## Interface fields

| Field | Type | Default | Purpose |
|---|---|---|---|
| `name` | string | — | Interface name as the kernel sees it (`eth0`, `eth0.202`, `tun3`, `ether1`). Used verbatim as `-i` / `-o`. |
| `ip` | address or `skip` | — | The address. `skip` means the interface exists at layer 2 but has no address. |
| `netmask` | `16` | `24` | Prefix length. **Only absent (`/24`) and `16` are supported**; anything else throws. |
| `dns` | string | — | Space-separated DNS names for this address, resolvable from `url:`/`nat:`. |
| `vip` | address | — | A single VRRP virtual address. |
| `vips` | list of `{ip, names}` | — | Several virtual addresses, each with its own DNS names. |
| `vrrpPriority` | number | `100` | keepalived priority. Lower on the backup member. |
| `vlan` | number or `trunk` | — | VLAN of the port. Drives the L2 colouring and the switch configuration. |
| `port` | number | — | Physical switch port. Only meaningful on switches. |
| `link` | `<host>/<iface>` | — | The far end of the cable. Declare each cable on **one** side only. |
| `mss` | number | — | Emit a `TCPMSS --set-mss` clamp for traffic arriving from this address. |

### The first address wins

`getDefaultIp()` returns the address of the first interface that has one, skipping `skip`
interfaces. That address is the default bind address for every service whose `url:` does not name one
explicitly, and it is the address other hosts' rules will use — so **interface order matters**. On a
multi-homed firewall, list the management VLAN before the public one so that inherited management
services do not bind to the public address:

```yaml title="hosts/internal/fw-1.yml"
interfaces:

# Physical trunk to the core switch. All VLANs below are stacked on top of it, so it carries
# no address of its own.
- name: eth0
  ip:   skip

# The app VLAN is listed first, so it provides the host's default IP: management services
# inherited via services_links bind here rather than on the public address.
- name: eth0.202
  ip:   10.20.20.11
  vip:  10.20.20.1
  vlan: 202

- name: eth0.100
  ip:   198.51.100.11
  vlan: 100
  vips:
  - ip:    198.51.100.10
    names: gw.demo.example.com
```

### `ip: skip`

Use it whenever a port must appear in the diagrams and in the switch configuration but has no layer-3
address — server IPMI ports, the physical trunk under a stack of VLAN sub-interfaces, every port on a
switch. A service can never bind to a `skip` interface; if the *default* address resolves to `skip`
you get an explicit error.

### VRRP: `vip` and `vips`

Every virtual address must exist on **exactly two** hosts. The generator pairs them automatically and
uses the pair to suppress pointless FORWARD rules between the two members. Three kinds of mistake are
rejected outright:

- a `vip` that appears on no other host — `There no any additional virtual interface with ip address …`
- a `vip` that equals another host's *real* address — `Virtual ip address … has pair with bare interface …`
- the same `vip` on three or more hosts — `There are two additional virtual addresses …`

Use `vips` when one interface holds several service addresses, and give them DNS names so that
`url:` and `nat:` can refer to them by name:

```yaml
- name: eth0.100
  ip:   198.51.100.11
  vlan: 100
  vips:
  - ip:    198.51.100.10
    names: gw.demo.example.com
```

If you also generate `keepalived.conf`, the VRRP router id is taken from the VLAN suffix of the
interface name, so **VIP-bearing interfaces must be named `<iface>.<vlan>`** (`eth0.100` → router id
`100`). See [Host provisioning](/firewall-config/generators/host-provisioning/#keepalived).

### Cabling: `port`, `link`, `vlan`

Describe every cable exactly once, and by convention describe it **on the switch**, which is then the
single place where the physical layer lives:

```yaml title="hosts/ipmi/sw-core-1.yml"
- name: ether5
  ip:   skip
  port: 5
  link: web-1/eth0
  vlan: 202
```

Declaring the same cable on both sides makes the L2 builder fail with
`There are more than one interface … connected to …`. Interfaces whose name starts with `bond` are
excluded from the L2 diagram.

## Blocked addresses

Emitted at the top of both `INPUT` and `FORWARD`, before any `ACCEPT`:

```yaml
blockedIpAddresses:
- ip:     203.0.113.66
  reason: Repeated credential stuffing against the web application
  type:   DROP

- ip:     203.0.113.90
  reason: Scanner, reported by the upstream provider
  type:   REJECT
```

`type` is `REJECT` (the default) or `DROP`. `reason` becomes the comment above the rule:

```sh
# Repeated credential stuffing against the web application
-A INPUT -s 203.0.113.66 -j DROP
```

## Custom rules

The escape hatch for anything the service model cannot express — rate limits, port ranges, marking.
The `rule` string is emitted verbatim into the chain you name, so it must be a complete
`iptables-save` line, including its own `-A <CHAIN>`:

```yaml
customRules:
- chain:         INPUT
  rule:          -A INPUT -p udp -m udp --dport 33434:33534 -m limit --limit 5/min -j ACCEPT
  description:   Rate-limited traceroute probes
  justification: Network troubleshooting from the NOC; not expressible as a service

- chain:         POSTROUTING
  rule:          -A POSTROUTING -s 10.20.6.0/24 -o eth0.100 -j SNAT --to-source 198.51.100.10
  description:   Egress for the out-of-band management network
  justification: IPMI controllers need NTP and vendor firmware updates
```

`chain` is one of `INPUT`, `OUTPUT`, `FORWARD`, `PREROUTING`, `POSTROUTING` and is mandatory —
omitting it fails with `No chain type for host … and custom rule …`. Rules land at the end of their
chain's generated block, after everything derived. Nothing is validated: a typo becomes a broken
`iptables-restore`, and a custom rule is invisible to the diagrams and the audit pages. Treat each
one as a documented exception, which is what `description` and `justification` are there for.

## Next

- [Services and access](/firewall-config/configuration/services/) — what the host listens on and who may reach it.
- [YAML schema reference](/firewall-config/reference/yaml-schema/) — the same fields as flat tables.
