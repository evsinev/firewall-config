---
title: Services and access
description: 'The url:, access:, nat: and services_links mini-languages — where every generated rule comes from.'
sidebar:
  order: 3
---

A **service** is something a host listens on. Everything the tool generates — `INPUT`, `OUTPUT`,
`FORWARD`, `SNAT`, `DNAT`, the diagrams' edges, the audit tables — is derived from services and their
access lists. This is the page that matters.

```yaml title="hosts/internal/db-1.yml"
services:
- url:           postgres
  name:          main-db
  description:   Primary PostgreSQL cluster
  justification: The web application stores transaction data here
  access:        [web-app.service, adm-1]
```

Read as: *this host listens on the `postgres` protocol at its default address; the service is globally
known as `main-db`; whoever runs `web-app`, plus `adm-1`, may reach it.*

## Service fields

| Field | Required | Purpose |
|---|---|---|
| `url` | **yes** | Protocol, port and bind address — see [the `url:` grammar](#the-url-grammar). |
| `access` | **yes** | Who may reach it — see [the `access:` grammar](#the-access-grammar). An absent list is an error, an empty one is legal and means nobody. |
| `name` | no | A **globally unique** id, so other hosts can grant access to the service instead of to an address. Required if the definition is to be reused through `services_links`. |
| `description` | recommended | Falls back to the protocol's description. Printed above the rule and in the wiki. |
| `justification` | recommended | Why this service is exposed. This is the audit evidence. |
| `nat` | when NAT applies | The public URL to translate through — see [NAT](#nat). |
| `program` | no | Overrides the protocol's `program` in the audit tables. |
| `tags` | no | Only read by [`MainExternalServices`](/firewall-config/generators/audit-reports/#externally-reachable-services). |
| `external` | no | Free text (usually the public URL), printed by `MainExternalServices`. |
| `dip` | — | Parsed but unused. Do not use it. |

Two services on one host may not share a `name` or a `url`, and a `name` may not be reused across
hosts — both are hard errors at load time.

## The `url:` grammar

`url` is tokenised on spaces, colons and semicolons. Four shapes come out of that:

| Form | Protocol | Port | Bind address |
|---|---|---|---|
| `ssh` | `ssh` | from `protocols.yml` | host's default address |
| `http 9093` | `http` | `9093` | host's default address |
| `munin://10.20.6.1` | `munin` | from `protocols.yml` | `10.20.6.1` |
| `stunnel://gw.demo.example.com:5000` | `stunnel` | `5000` | resolved from the DNS name |

The protocol name must exist in [`protocols.yml`](/firewall-config/configuration/protocols-and-networks/);
an unknown name fails with `Protocol X not found in protocols.yml`.

Use the `//address` form to pin a service to one interface of a multi-homed host — for example a
management daemon that must only listen on the out-of-band leg. A non-numeric address is resolved
against every `dns:` and `vips.names` in the whole configuration, so you can refer to a virtual
address by name and let it move:

```yaml
# Resolves through fw-1/fw-2 eth0.100 vips.names: gw.demo.example.com
- url: stunnel://gw.demo.example.com:5000
```

The address must be reachable on one of *this* host's interfaces (real address or `vip`), otherwise
`Can't find ip … in interfaces for host …`. A name that maps to two different addresses is also an
error.

## The `access:` grammar

Each entry in the list is one of four things, tried in this order:

| Form | Example | Means |
|---|---|---|
| `group-<group>` | `group-internal` | every host in `hosts/internal/` |
| exact host name | `adm-1` | that host |
| service name | `web-app` or `web-app.service` | every host declaring a service with that `name` (the `.service` suffix is cosmetic and stripped) |
| prefix wildcard | `adm-*` | every host whose name starts with `adm` |

An entry that matches nothing fails with
`Host or service 'X' not found in access list for …`. Exact host names are checked **before** service
names, so a host and a service sharing a name resolves to the host — avoid that collision.

Only a *trailing* `-*` is a wildcard; `*-db` and `web-*-1` do not work. The prefix is the pattern with
`-*` removed, so `adm-*` matches `adm-1` **and** `administration-3` — keep prefixes distinctive.

### Prefer service references to host names

`access: [web-app.service]` says *whoever runs the web application*. Move the application to another
host and both rule sets regenerate correctly with no edit here. `access: [web-1]` says *that box*,
which silently becomes wrong the day the application moves. Use the service form for
application-to-application flows and host names for administrative access.

## Sharing definitions with `services_links`

Base services — SSH, metrics, backup agents — are declared **once** on one host, with a `name:`, and
referenced from everywhere else:

```yaml title="hosts/internal/adm-1.yml — declares them"
services:
- url:           ssh
  name:          ssh
  description:   Administrative SSH access
  justification: PCI DSS 2.3 - all administrative access is encrypted
  access:        [adm-*]

- url:           node-exporter
  name:          node-exporter
  description:   Prometheus node metrics endpoint
  justification: Capacity and availability monitoring
  access:        [adm-1]
```

```yaml title="hosts/internal/db-1.yml — reuses them"
services_links: ssh, node-exporter
```

Each referenced definition is appended to this host's own service list, then evaluated against *this*
host — so `url: ssh` binds to `db-1`'s default address, not `adm-1`'s. The result in `gen/db-1`:

```sh
# adm-1 -> ssh:ssh
-A INPUT  -i eth0 -p tcp -m tcp -s 10.20.20.31 --dport 22 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT
-A OUTPUT -o eth0 -p tcp -m tcp -d 10.20.20.31 --sport 22 -m state --state RELATED,ESTABLISHED -j ACCEPT
```

Separators are space, comma, tab or semicolon. Referencing an unknown name, or a name the host already
has, is an error. Note that reused definitions are shared objects: changing `access:` on the
declaration changes it for every host that links it — which is the point, and also the thing to
remember before editing one.

## NAT

`nat:` holds the **public** URL of a flow, in the same grammar as `url:`. You never say "SNAT" or
"DNAT"; the direction follows from which side is public.

### SNAT — a private host reaching a public destination

Declared on the *external* peer, because the peer is the one publishing the service:

```yaml title="hosts/external/partner-api.example.com.yml"
gw: 198.51.100.10          # our external virtual address

interfaces:
- name: eth0
  ip:   198.51.100.50

services:
- url:           https
  name:          partner-api
  justification: The proxy submits settlement requests on behalf of the application
  nat:           https://gw.demo.example.com
  access:        [proxy-1]
```

The destination is public, so the firewall between the two translates our source address:

```sh title="gen/fw-1"
-A POSTROUTING -s 10.20.2.21  -d 198.51.100.50 -p tcp  --dport 443 -o eth0.100 -j SNAT --to-source 198.51.100.10
```

### SNAT — a private peer that is not ours

A partner reached over a tunnel has an RFC 1918 address, so nothing about it *looks* public — but
traffic towards it still has to be translated. Mark the peer with `external_peer: true` and it is
treated exactly like a public destination:

```yaml title="hosts/external/partner-vpn.example.com.yml"
gw: 10.20.2.1              # the tunnel terminates on the DMZ concentrator
external_peer: true        # not part of our address space, despite the private address

interfaces:
- name: eth0
  ip:   172.16.4.50

services:
- url:           https
  name:          partner-settlement
  justification: The application submits settlement requests to the partner
  nat:           https://10.20.2.1
  access:        [web-1]
```

```sh title="gen/fw-1"
-A POSTROUTING -s 10.20.20.21  -d 172.16.4.50 -p tcp  --dport 443 -o eth0.201 -j SNAT --to-source 10.20.2.1
```

The flag is per host, and it only changes the *destination* side of the decision — see
[Limitations](/firewall-config/internals/limitations/#snat-to-a-private-peer-is-a-per-host-flag).

### DNAT — a public host reaching a private service

Declared on *our* host, whose service is published at a public address:

```yaml title="hosts/internal/web-1.yml"
services:
- url:           https
  name:          web-app
  nat:           https://gw.demo.example.com
  access:        [partner-api.example.com, adm-*]
```

```sh title="gen/fw-1"
-A PREROUTING -d 198.51.100.10 -p tcp -m tcp --dport 443 -j DNAT --to-destination 10.20.20.21:443
```

The same service can serve both a public and a private access entry, as here: the `adm-*` flow needs
no translation and gets none.

### Rules of thumb

- A flow whose **destination** is public needs `nat:`, or the run fails with
  `Direction … wants to use NAT address but no NAT address was found`.
- A flow whose **source** is public needs `nat:`, or it fails with `No nat for service … at host …`.
- A flow where **both** ends are public cannot be expressed and raises
  `Trying to config both SNAT and DNAT with …`.
- "Public" means: not `10.`, not `172.16`, not `192.168` — plus any host marked
  `external_peer: true`, which counts as public when it is the destination.
- Give the NAT address a DNS name via `vips.names` and refer to it by name, so re-addressing the
  perimeter is a one-line change.

## Next

- [Protocols and networks](/firewall-config/configuration/protocols-and-networks/) — the catalogue `url:` refers to.
- [Rule derivation](/firewall-config/internals/packet-derivation/) — how these declarations become chains.
