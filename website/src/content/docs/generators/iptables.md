---
title: iptables
description: The main generator — one iptables-save file per host, and the structure of what it emits.
sidebar:
  order: 1
---

The primary generator. It writes one `iptables-save` file per host, named exactly as the host, into a
directory you choose.

## Usage

```sh
java -jar firewall-config.jar <config-dir> <host|group-NAME> <output-dir>
```

| Argument | Meaning |
|---|---|
| `<config-dir>` | The [configuration directory](/firewall-config/configuration/config-directory/). |
| `<host>` | A single host name, or `group-<name>` to generate for every host in that group. |
| `<output-dir>` | Where to write. **Must already exist.** |

```sh
# every host under hosts/internal/, plus one host from another group
java -jar firewall-config.jar . group-internal ../ansible-msk/roles/iptables/files/gen
java -jar firewall-config.jar . fw-ipmi-1      ../ansible-msk/roles/iptables/files/gen
```

It prints one `.` per host and nothing else. This is `com.payneteasy.firewall.Main`, the jar's
`Main-Class`, which is why it needs no `-cp`.

Generate only for hosts that actually run `iptables`. Switches, IPMI controllers and external peers
must still be *described* — their services are what produce rules on the hosts and firewalls around
them — but generating a rule set for them makes no sense.

## What it emits

The file is a complete `iptables-save` document with two tables. Everything is derived; the only
verbatim content is what you put in
[`customRules`](/firewall-config/configuration/hosts/#custom-rules).

```
*filter
:INPUT   DROP [0:0]
:FORWARD DROP [0:0]
:OUTPUT  DROP [0:0]
```

| Block | Contents |
|---|---|
| Loopback | `-i lo` / `-o lo` accept. |
| Anti-scan | A fixed set of rules dropping malformed TCP flag combinations (NULL, XMAS, SYN/RST, FIN scans) and fragments, with rate-limited logging. Identical in every file. |
| Blocked hosts | `blockedIpAddresses`, at the top of `INPUT`, before any accept. |
| INPUT packets | One accept pair per (service, permitted source): the `INPUT` rule plus its stateful `OUTPUT` reply. |
| MSS | `TCPMSS --set-mss` clamps derived from `mss:` on a source interface. |
| ICMP | `echo-reply`, `destination-unreachable`, `echo-request`, `time-exceeded` accepted on all interfaces. |
| Custom INPUT rules | Verbatim. |
| OUTPUT packets | One pair per service *elsewhere* that lists this host in its `access:`. |
| Custom OUTPUT rules | Verbatim. |
| Blocked hosts | Again, at the top of `FORWARD`. |
| FORWARD packets | One pair per flow this host routes. Only non-empty on hosts that are somebody's `gw`. |
| Custom FORWARD rules | Verbatim. |
| Tail | `LOG` on all three chains, then `REJECT --reject-with tcp-reset` for TCP; everything else is dropped by the chain policy. |
| `*nat` | SNAT / DNAT derived from `nat:`, then custom `PREROUTING` / `POSTROUTING` rules. Policies are `ACCEPT`. |

### The comments are the point

Every rule pair is preceded by a comment naming the source, the protocol, the destination and — where
they exist — the service names on both ends:

```sh title="gen/db-1"
# web-1:web-app -> postgres:main-db
-A INPUT  -i eth0 -p tcp -m tcp -s 10.20.20.21 --dport 5432 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT
-A OUTPUT -o eth0 -p tcp -m tcp -d 10.20.20.21 --sport 5432 -m state --state RELATED,ESTABLISHED -j ACCEPT
```

```sh title="gen/fw-1"
# web-1 -> postgres://db-1   web-app --> main-db
-A FORWARD -s 10.20.20.21 -d 10.20.22.21 -i eth0.202 -o eth0.203 -p tcp -m tcp --dport 5432 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT
-A FORWARD -s 10.20.22.21 -d 10.20.20.21 -i eth0.203 -o eth0.202 -p tcp -m tcp --sport 5432 -m state --state RELATED,ESTABLISHED -j ACCEPT
```

A reviewer can point at any line and get back to the declaration that caused it. That is also why
diffing generated output across runs is a useful review step in itself.

### Stateful, in both directions

Each flow becomes two rules: `NEW,RELATED,ESTABLISHED` in the request direction and
`RELATED,ESTABLISHED` in the reply direction. Because the default policy on all three chains is
`DROP`, the reply rule is not redundant — `OUTPUT` is filtered too.

### `*nat`

```sh title="gen/fw-1"
*nat
:PREROUTING  ACCEPT [0:0]
:POSTROUTING ACCEPT [0:0]
:OUTPUT      ACCEPT [0:0]

# proxy-1 -> https://partner-api.example.com    * --> partner-api
-A POSTROUTING -s 10.20.2.21  -d 198.51.100.50 -p tcp  --dport 443 -o eth0.100 -j SNAT --to-source 198.51.100.10

# partner-api.example.com -> https://web-1     * --> web-app
-A PREROUTING -d 198.51.100.10 -p tcp -m tcp --dport 443 -j DNAT --to-destination 10.20.20.21:443
```

The NAT rules are a projection of the same FORWARD packets — a flow needing translation appears in
both tables. See [Services and access](/firewall-config/configuration/services/#nat) for how the
direction is chosen.

## Deploying it

The output is an `iptables-restore` document, so deployment is a file copy plus a restore. The usual
arrangement is to point `<output-dir>` at an Ansible role in a separate repository:

```yaml title="roles/iptables/tasks/main.yml (sketch)"
- name: install rules
  copy:
    src: "gen/{{ inventory_hostname }}"
    dest: /etc/sysconfig/iptables
  notify: restore iptables

# handler
- name: restore iptables
  command: iptables-restore /etc/sysconfig/iptables
```

Two things worth building into that pipeline:

- **Validate before applying.** `iptables-restore --test` catches a broken `customRules` line before
  it locks you out.
- **Review the diff.** Because the generator is deterministic, a description change produces a
  minimal, readable diff of the rule set. That diff is the best review artifact the tool offers.

Note that the whole rule set is replaced atomically by `iptables-restore`, so there is no partial
state to clean up — but also no preservation of anything added by hand on the host.

## Errors you will meet

| Message | Cause |
|---|---|
| `Host or service 'X' not found in access list for …` | Typo in `access:`, or a service `name` that no host declares. |
| `Can't find interface at host X which connected to Y` | `gw` on `Y` does not match any address on `X`. |
| `Direction … wants to use NAT address but no NAT address was found` | Public destination, missing `nat:`. |
| `No nat for service … at host …` | Public source, missing `nat:`. |
| `Trying to config both SNAT and DNAT with …` | Both ends public — not expressible. |
| `Protocol X not found in protocols.yml` | Unknown protocol name in `url:` or `nat:`. |
| `Service … has skip address at host …` | The service's bind address resolved to a `skip` interface. |
| `There no any additional virtual interface with ip address …` | A `vip` with no VRRP peer. |

## Next

- [Rule derivation](/firewall-config/internals/packet-derivation/) — the algorithm behind all of this.
- [Redmine wiki](/firewall-config/generators/redmine-wiki/) — the same facts as audit documentation.
