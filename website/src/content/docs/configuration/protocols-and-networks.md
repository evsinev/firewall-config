---
title: Protocols and networks
description: protocols.yml — the named port catalogue — and networks.yml, which names every /24.
sidebar:
  order: 4
---

Two small files at the root of the configuration directory. Both are required; `protocols.yml` is
read by every command, `networks.yml` only by the L3 diagram.

## `protocols.yml`

The catalogue that every `url:` and `nat:` refers to by name. It exists so that a rule set reads
`--dport 5432 # postgres:main-db` instead of a bare number, and so that ports are defined once.

```yaml title="protocols.yml"
protocols:

- name:          ssh
  protocol:      tcp
  port:          22
  program:       sshd
  description:   Secure Shell
  justification: Encrypted administrative access

- name:          postgres
  protocol:      tcp
  port:          5432
  program:       postgres
  description:   PostgreSQL wire protocol
  justification: Application access to the database

- name:          dns
  protocol:      udp
  port:          53
  program:       unbound
  description:   Domain Name System
  justification: Name resolution for internal hosts
```

| Field | Required | Purpose |
|---|---|---|
| `name` | **yes** | What `url:` refers to. Free-form: application-level, not IANA. |
| `protocol` | **yes** | `tcp` or `udp`. Becomes `-p tcp -m tcp`. |
| `port` | **yes**, non-zero | Default port. Overridable inline (`url: http 3128`). |
| `program` | recommended | The daemon that listens. Audit evidence — reviewers check the running process against it. |
| `description` | recommended | Inherited by services that set none of their own. |
| `justification` | recommended | Same, and printed in the wiki's protocol table. |

Both `protocol` and a non-zero `port` are validated at load time (`protocol is null for X`,
`Port is empty for X`).

### Name protocols after the application, not the port

The name is the vocabulary of your whole description, so pick names a reader recognises. Several
entries may share a port with different names and programs — that is the intended use:

```yaml
- name: proxy            # squid on 3128
  protocol: tcp
  port:     3128
  program:  squid

- name: prometheus       # prometheus on 9090
  protocol: tcp
  port:     9090
  program:  prometheus
```

A service that runs on a non-default port keeps the protocol's semantics and overrides only the
number: `url: http 3128`. Add a new protocol entry when the *application* is different, not when the
port is.

## `networks.yml`

Maps each `/24` to a human-readable name, used as the network label in the
[L3 diagram](/firewall-config/generators/l3-diagram/).

```yaml title="networks.yml"
networks: {
  198.51.100.0 : internet,
  10.20.2.0    : dmz,
  10.20.20.0   : app,
  10.20.22.0   : db,
  10.20.6.0    : ipmi
}
```

- The key is the **network address** of a `/24`, not a host address.
- Every `/24` that appears on any interface must be listed. A missing one is a hard error:
  `Network X has no name, please add it to networks.yml file`.
- A name starting with `skip` excludes the network from the diagram — useful for transit and
  point-to-point ranges that would only add noise:

  ```yaml
    10.20.99.0 : skip-transit,
  ```
- `internet` is always drawn first; every other network follows alphabetically. Naming your public
  range `internet` is therefore worth doing.
- The generator works in `/24` units throughout, so a larger allocation has to be listed as its
  constituent `/24`s.

The flow-style braces above are just YAML — a block mapping works identically. Keep the file sorted
the way you think about the estate; nothing depends on the order.

## Next

- [L2 additions and positions](/firewall-config/configuration/l2-additions/)
- [L3 diagram](/firewall-config/generators/l3-diagram/)
