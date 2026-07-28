---
title: Audit reports
description: Externally reachable services, container hardening results, critical software inventory and a packet graph.
sidebar:
  order: 7
---

Five reports that exist to answer recurring audit questions. Unlike the other generators, three of
them read data from *outside* the description — container scan results and software inventories — and
join it with the estate model.

## Externally reachable services

*"Which services can be reached from outside, and why?"*

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainExternalServices \
     [-t TAGS] [-a ACCESS] <dir> <prefix>
```

| Option | Default | Meaning |
|---|---|---|
| `-t`, `--tags` | `ingress-nginx` | Comma-separated `tags:` values that mark a service as external. |
| `-a`, `--access` | `internet` | Comma-separated substrings; a service matches if **any** `access:` entry contains one. |

A service is reported if it carries a matching tag **or** has a matching access entry. Textile rows go
to stdout:

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainExternalServices . current
```

```txt
| web-1 | Customer-facing web application | Primary business service | [partner-api.example.com, adm-*] | web-app | Customer-facing web application | Primary business service |  | https | https://gw.demo.example.com | https://www.demo.example.com/ |
```

Columns: host, host description, host justification, `access:`, service name, service description,
service justification, `program`, `url`, `nat`, `external`.

The `-a` match is a plain substring test against the access entries, so it only finds anything if your
groups or host names contain the word — `access: [group-internet]` matches, a list of external host
FQDNs does not. In practice the reliable mechanism is the tag:

```yaml
- url:      https
  name:     web-app
  nat:      https://gw.demo.example.com
  tags:     [ingress-nginx]
  external: https://www.demo.example.com/
  access:   [partner-api.example.com, adm-*]
```

Tag every deliberately-published service and this report becomes the authoritative list of your attack
surface. `external:` is free text and is where the public URL belongs.

## Container hardening

Two commands consume the JSON output of
[podman-security-bench](https://github.com/containers/podman-security-bench) and turn it into Redmine
textile.

### Fleet matrix

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainPodmanCheckTable \
     --results-dir <dir-of-zips> <dir> <prefix>
```

`--results-dir` holds one `<hostname>.zip` per scanned host, each containing
`log/podman-security-bench.log.json`. The output is a matrix — checks as rows, hosts as columns — with
colour-coded cells:

| Cell | Meaning |
|---|---|
| `%{background:lightgreen}PASS%` | check passed |
| `%{color:green}INFO%` | informational |
| `%{background:yellow}WARN%` | failed; links to the notes section |
| `%{background:magenta}NOTE%` | manual check required |

Below the matrix, an `h2. Notes` section lists, per host and per check, the remediation, its impact and
the affected items. One page then shows which hosts are behind on which control.

### Hardening guide

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainPodmanCheckHardningGuide \
     --result-file <file.json> \
     [--redmine-enabled --redmine-url URL --redmine-key KEY \
      --redmine-project PROJECT --redmine-parent-issue-id ID]
```

Reads a single result file and prints a wiki page grouped by benchmark section and check, each with its
status, remediation and impact.

With `--redmine-enabled` it additionally **creates a Redmine sub-issue** per `WARN`, `NOTE` or
`(Manual)` check under `--redmine-parent-issue-id`, via `POST /issues.json`. That writes to your tracker,
so run it once without the flag, read the output, and only then enable it — otherwise a re-run
duplicates every issue.

## Critical software inventory

```sh
java -cp firewall-config.jar com.payneteasy.firewall.critsoft.CritSoftCollectorMain \
     <redmine-url> <redmine-page> <redmine-key> <software-cards-dir>
```

Parses pipe-delimited "software cards" — one file per host, one line per package — and pushes a
consolidated *Critical Software In Use* table to a Redmine wiki page:

```txt title="one card line"
| srv-1 | Centos | CentOS Linux release 7.4.1708 (Core) | OS | https://path/to/hardening/Centos-7_hg |
```

Fields: host, product, version string, category, link to the hardening guide. The table aggregates by
product and version and counts the hosts on each, which is what makes "how many machines still run
that version" answerable. This is the one generator with thorough unit-test coverage
(`SoftwareCardHandlerTest`), and the only one with input fixtures committed to this repository.

Note that it pushes to Redmine unconditionally — there is no directory mode and no dry run.

## Packet graph

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainPacketsGraphviz <config-dir>
```

Graphviz edges on stdout, one per permitted flow:

```text
"adm-1" -> "db-1" [label="postgres"];
"adm-1" -> "proxy-1" [label="proxy"];
"web-1" -> "db-1" [label="postgres"];
"web-1" -> "proxy-1" [label="proxy"];
```

Wrap it in `digraph { … }` and pipe to `dot` for a service-dependency picture — a different view from
the L3 diagram, which shows topology rather than flows:

```sh
{ echo 'digraph G {'; java -cp firewall-config.jar com.payneteasy.firewall.MainPacketsGraphviz .; echo '}'; } \
  | dot -Tsvg > flows.svg
```

Two caveats: it only looks at group `internal`, and it applies **hard-coded name filters** (dropping
`ntp`, `dns` and some site-specific prefixes) to keep the graph readable. Expect to edit the source if
you rely on it.

## Next

- [Redmine wiki](/firewall-config/generators/redmine-wiki/) — the per-host audit pages.
- [CLI reference](/firewall-config/reference/cli/) — every entry point in one table.
