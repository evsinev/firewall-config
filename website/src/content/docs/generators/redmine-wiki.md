---
title: Redmine wiki
description: Push per-host, per-group and per-service audit pages to a Redmine wiki — or to a directory.
sidebar:
  order: 4
---

Turns the same model that produces the rule set into Redmine wiki pages: one pair per host, one
overview per group, and a protocol catalogue. This is the audit half of the tool — it is why
`description` and `justification` are required almost everywhere.

## Usage

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainWiki \
     <wiki-url|directory> <redmine-api-key> <config-dir> [-f|--force]
```

| Argument | Meaning |
|---|---|
| `<wiki-url\|directory>` | A Redmine wiki URL, or — if it does **not** start with `http` — a local directory to write files into. |
| `<redmine-api-key>` | Redmine API key. Required positionally even in directory mode; pass any placeholder. |
| `<config-dir>` | The configuration directory. |
| `-f`, `--force` | Ignore the hash cache and rewrite every page. |

```sh
# to Redmine
java -cp firewall-config.jar com.payneteasy.firewall.MainWiki \
     https://redmine.example.com/projects/nm-msk/wiki "$REDMINE_KEY" .

# to a directory — no Redmine needed, ideal for review and for a first look
mkdir -p wiki-out
java -cp firewall-config.jar com.payneteasy.firewall.MainWiki ./wiki-out dummy-key .
```

Directory mode is worth knowing about: it makes the audit output diffable in git, so a change to the
description shows up as a change to the documentation in the same review.

A host whose pages cannot be built is logged as a warning and skipped rather than aborting the run —
so check the output for `Cannot process host [...]` instead of relying on the exit code.

## Pages

| Page | One per | Contents |
|---|---|---|
| `<host>_details` | host | Description, business goal, and includes of the other two host pages |
| `<host>_packets` | host | Interfaces, services with their input packets, output packets, forward packets |
| `<group>_group` | group | Table of the group's hosts with addresses and justifications |
| `services` | configuration | The `protocols.yml` catalogue |

All content is Redmine **textile**. Host references become wiki links, so the pages form a navigable
graph of the estate.

```txt title="wiki-out/db-1_details.wiki"
h1. db-1

Primary PostgreSQL server

h2. Business goal

Stores customer and transaction data
{{include(db-1_packets)}}
{{include(db-1_run)}}
```

`<host>_run` is *not* generated — it is a hand-written page for runbook notes, included if it exists.
That is the intended split: generated facts on one page, human context on another, so neither
overwrites the other.

```txt title="wiki-out/db-1_packets.wiki"
h2. Interfaces

|_.Name|_.IP|_.Virtual IPs|_.DNS|
|eth0|10.20.22.21|||
|ipmi|skip|||

h2. Services and input packets

|_.Service name|_.Bound interface|_.Interface address|_.Port|_.Accessed from|_.Description|_.Justification|
|ssh:ssh|eth0|10.20.22.21|22|[[adm-1 details|adm-1]] |Administrative SSH access|PCI DSS 2.3 - all administrative access is encrypted|
|postgres:main-db|eth0|10.20.22.21|5432|[[adm-1 details|adm-1]] [[web-1 details|web-1:web-app]] |Primary PostgreSQL cluster|The web application stores transaction data here|

h2. Output packets

|_.Remote hostname|_.Remote ip address|_.Protocol|_.Port|_.Service name|_.Description|_.Justification|
|[[proxy-1 details|proxy-1]]|10.20.2.21|udp internal-dns|53|dns|Recursive resolver for the whole estate|Name resolution without exposing internal names outside|
```

```txt title="wiki-out/internal_group.wiki"
h2. internal group

|_.Host|_.IP address|_.Description|_.Justification|
|[[fw-1 details|fw-1]]|10.20.20.11|Perimeter firewall, master|Controls all traffic between the internet, the DMZ and the internal networks|
|[[db-1 details|db-1]]|10.20.22.21|Primary PostgreSQL server|Stores customer and transaction data|
```

Firewalls additionally get a **forward packets** table with `SNAT` and `DNAT` columns.

## The hash cache

Every page's content is hashed and the hash stored in `pages_history.yml` in the configuration
directory. On the next run, unchanged pages are skipped — which keeps Redmine's own page history
meaningful instead of one bulk edit per CI run.

```yaml title="pages_history.yml (written by the tool)"
!!com.payneteasy.firewall.dao.model.TPagesHistory
lastUpdateDate: 2026-07-29T00:23:11.902Z
pageHistories:
- {pageHash: -1811445625, pageName: db-1_packets}
```

Do not edit it, and do not commit it — it is local state. Use `--force` after changing a *template*
(rather than the description), since the hash only covers the rendered content.

## Uploading hand-written pages

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainWikiDir <redmine-url> <redmine-key> <wiki-dir>
```

Uploads every file in a directory as a wiki page, named after the file. Useful for keeping the
hand-written `<host>_run` pages under version control next to the description.

## Security notes

Two things to be aware of before pointing this at a production Redmine:

- `RedmineEasyClient` installs a **trust-all** `X509TrustManager` — TLS certificates are not verified.
  Treat the connection as unauthenticated at the transport level.
- The API key is a positional argument, so it lands in shell history, in `ps` output and — in the usual
  arrangement — in a wrapper script inside the description repository. Pass it from a CI secret
  (`"$REDMINE_KEY"`), and give the key the narrowest Redmine permissions that work.

See [Limitations](/firewall-config/internals/limitations/) for the rest.

## Next

- [Audit reports](/firewall-config/generators/audit-reports/) — external services, container hardening, critical software.
- [Rule derivation](/firewall-config/internals/packet-derivation/) — where the packet tables come from.
