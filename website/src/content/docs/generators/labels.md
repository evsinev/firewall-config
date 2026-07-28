---
title: Labels
description: Printable host stickers, Ethernet port labels and patch-cord labels, coloured per VLAN.
sidebar:
  order: 6
---

Three generators aimed at a physical rack: stickers for the front of a server, labels for switch
ports, and labels for both ends of every patch cord. They exist so that the cabling in the rack is
labelled from the same description that produces the firewall rules, which is what keeps the two in
agreement.

All three write to a **fixed path** relative to the current directory, and `target/` must exist.

| Command | Output | Source |
|---|---|---|
| `MainHostLabels` | `target/hosts.html` | hosts, their `description` and `color` |
| `MainL2Labels` | `target/labels.html` | the L2 cable graph, coloured from `vlanColors` |
| `MainL2WireLabels` | `test.svg` | the same cable graph, as an A4 sheet |

## Host stickers

```sh
mkdir -p target
java -cp firewall-config.jar com.payneteasy.firewall.MainHostLabels <config-dir>
```

```
fw-1
fw-2
proxy-1
adm-1
db-1
web-1
sw-core-1
Creating rows ...
```

`target/hosts.html` is a print-oriented page: six cells per row, sized in millimetres, background
colour taken from each host's `color:` field, and the text mirrored so that a sticker folded over a
rack rail reads correctly from both sides. Print it, cut, fold.

Set `color:` per *role* rather than per host — firewalls one colour, application servers another — and
the rack becomes readable from across the room.

## Ethernet port labels

```sh
mkdir -p target
java -cp firewall-config.jar com.payneteasy.firewall.MainL2Labels [-f FILTER] <dir> <prefix>
```

Same arguments as the [L2 diagram](/firewall-config/generators/l2-diagram/) commands. Writes
`target/labels.html`: one label per cable end, coloured by the port's VLAN through `vlanColors`, so a
mis-patched port is visible at a glance.

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL2Labels --filter internal,ipmi . current
```

The command echoes the cables it used, the same listing as the diagram commands — a quick check that
every port you expect to label is actually described.

Note that this generator contains **site-specific host-name rewriting** (shortening long names to fit
a label), so names in the output may not match your host files exactly. See
[Limitations](/firewall-config/internals/limitations/#site-specific-hard-coding).

## Patch-cord labels

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL2WireLabels <config-dir>
```

Writes `test.svg`: an A4 sheet of small labels, eight per row, each carrying **both** ends of one cable
(`sw-core-1/ether5` ↔ `web-1/eth0`) and the VLAN colour. Wrap one around each end of the cord and both
ends tell you where the other one goes.

This command takes only the configuration directory — no `--filter`, no prefix — so it labels every
cable in the description. The output name is fixed (`test.svg`); rename it after generating.

## Next

- [L2 diagram](/firewall-config/generators/l2-diagram/) — the graph these labels come from.
- [L2 additions and positions](/firewall-config/configuration/l2-additions/) — `vlanColors`.
