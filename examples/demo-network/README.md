# demo-network

A small, completely fictional network description used by the
[documentation](https://evsinev.github.io/firewall-config/). Every YAML snippet and every piece of
generated output on the site comes from this directory, so it is also the fastest way to see what
`firewall-config` produces without exposing a real estate.

All addresses are from the documentation ranges reserved by RFC 5737
(`198.51.100.0/24`, `203.0.113.0/24`) and RFC 1918.

## Topology

```
                        internet 198.51.100.0/24
                                  |
                     +------------+------------+
                     |                         |
              198.51.100.11             198.51.100.12
                   fw-1  ==== VRRP ====  fw-2          partner-api.example.com
                     |                         |            198.51.100.50
       +---------+---+-------+--------+--------+
       |         |           |        |
   dmz .2.0   app .20.0   db .22.0   ipmi .6.0        (all 10.20.x.0/24)
       |         |           |        |
   proxy-1    web-1       db-1    sw-core-1
              adm-1
```

| Group      | Host                      | Role                                             |
|------------|---------------------------|--------------------------------------------------|
| `internal` | `fw-1`, `fw-2`            | VRRP pair of perimeter firewalls, five VLANs each |
| `internal` | `proxy-1`                 | DMZ egress proxy and internal resolver           |
| `internal` | `web-1`                   | Public web application                           |
| `internal` | `db-1`                    | PostgreSQL server                                |
| `internal` | `adm-1`                   | Jump host, Prometheus, owner of the shared service definitions |
| `ipmi`     | `sw-core-1`               | Core switch — the only place cabling is described |
| `external` | `partner-api.example.com` | Third-party peer on a public address             |

## What it exercises

| Feature | Where |
|---|---|
| VRRP pair, `vip` / `vips` with DNS names | `fw-1.yml`, `fw-2.yml` |
| Shared service definitions via `services_links` | declared in `adm-1.yml`, linked everywhere |
| `access:` by exact host, `group-internal`, `adm-*`, `web-app.service` | `proxy-1.yml`, `db-1.yml` |
| SNAT (private source → public destination) | `partner-api.example.com.yml` |
| DNAT (public source → private destination) | `web-1.yml` |
| `blockedIpAddresses`, `customRules` | `fw-1.yml` |
| `mss:` TCPMSS clamping | `partner-api.example.com.yml` |
| `ip: skip` L2-only interfaces, `port:` / `link:` / `vlan:` | `sw-core-1.yml`, servers' `ipmi` |
| Multi-homed source address selection | `fw-1` reaching `proxy-1` from `10.20.2.11` |

## Running it

From this directory, with the fat jar built (`./mvnw clean package` in the repo root):

```sh
JAR=../../target/firewall-config.jar
mkdir -p gen target

# iptables-save file per host, for every host in hosts/internal/
java -jar $JAR . group-internal gen

# L3 diagram source -> target/network.diag (add --run-nwdiag=true if nwdiag is installed)
java -cp $JAR com.payneteasy.firewall.MainL3Diagram --run-nwdiag=false --filter internal,ipmi . current

# L2 diagram -> current-l2.svg
java -cp $JAR com.payneteasy.firewall.MainL2SvgDiagram --filter internal,ipmi . current

# Redmine wiki pages, written to a directory instead of a Redmine instance
mkdir -p wiki-out && java -cp $JAR com.payneteasy.firewall.MainWiki ./wiki-out dummy-key .

# BIND zones
mkdir -p bind-out && java -cp $JAR com.payneteasy.firewall.MainBind . demo.example.com bind-out

# keepalived config for the master firewall
java -cp $JAR com.payneteasy.firewall.MainKeepalived . fw-1

# switch VLAN commands
java -cp $JAR com.payneteasy.firewall.MainMikrotik . sw-core-1
```

Everything under `gen/`, `target/`, `wiki-out/`, `bind-out/`, `current-l2.*` and
`pages_history.yml` is generated and ignored by git.

Interesting things to look at afterwards:

- `gen/fw-1` — FORWARD rules for every permitted flow, plus the `*nat` table with one SNAT and one
  DNAT rule, the blocked hosts and the custom rules.
- `gen/db-1` — INPUT rules whose sources came from `web-app.service` and `adm-1`.
- `gen/proxy-1` — the TCPMSS clamp and the `group-internal` fan-out.
- `target/network.diag` — five networks, `internet` first.
