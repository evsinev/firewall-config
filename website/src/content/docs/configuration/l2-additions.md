---
title: L2 additions and positions
description: The two prefix-scoped files that drive the L2 diagram — VLAN colours, manual cables, and saved node positions.
sidebar:
  order: 5
---

The L2 commands take a **prefix** as their second positional argument and read two files named after
it. With prefix `current`:

```
current-l2-additions.yml            # required: VLAN colours, manual cables
current-l2-positions.properties     # optional: node layout, written by the editor
```

The prefix exists so one description can carry several layouts of the same estate — for example
`current` and a `planned` rewiring — without duplicating the host files.

## `<prefix>-l2-additions.yml`

```yaml title="current-l2-additions.yml"
# Colour per VLAN, applied to ports and cables in the diagram and on the printed labels.
vlanColors: {

    trunk: 0xEF9A9A,

    100:   0xFFCC80,
    201:   0xFFF59D,
    202:   0xA5D6A7,
    203:   0x90CAF9,
    206:   0xE1BEE7
}

# Cables that exist but are not (or cannot be) described in hosts/.
addedLinks: []

# Cables described in hosts/ that should not be drawn.
removedLinks: []
```

| Key | Purpose |
|---|---|
| `vlanColors` | VLAN (as in `vlan:` on an interface, so a number or `trunk`) → colour. |
| `addedLinks` | Cables to draw that are not in `hosts/`. |
| `removedLinks` | Cables from `hosts/` to leave out. |

All three are optional in the sense that they may be empty, but **the file itself must exist** or the
L2 commands fail.

### Colours

Values are anything `java.awt.Color.decode` accepts — in practice `0xRRGGBB`. A VLAN with no entry is
drawn in the default colour, so the map is also a way of highlighting: colour only the VLANs under
discussion and everything else recedes. Keep `trunk` visually distinct, since trunk ports are where
mistakes hide.

### Manual cables

Both link lists use the same syntax, with the VLAN in parentheses optional on either side:

```
<host>/<port> (<vlan>) >>>> <host>/<port> (<vlan>)
```

```yaml
addedLinks:
  - sw-core-1/ether11 (206) >>>> ipmi-kvm-1/eth0 (206)

removedLinks:
  - sw-core-1/ether10 (206) >>>> db-1/ipmi (206)
```

Separators are `/`, space and `>`, so `>>>>` is only a convention — any run of `>` parses. `addedLinks`
creates the hosts and ports it mentions if they do not exist, which is how you draw equipment that has
no host file at all: a KVM, a PDU, a provider's handoff.

Use `addedLinks` for things genuinely outside the description and `removedLinks` to keep a diagram
readable (dropping IPMI cabling from an L3-oriented picture, for instance). Anything you add here is
invisible to the firewall generator — if a cable carries traffic that needs rules, it belongs in
`hosts/`.

## `<prefix>-l2-positions.properties`

Written by the [interactive editor](/firewall-config/generators/l2-diagram/#the-editor), read by the
SVG and PNG generators. Keys are `<host>` for a host box and `<host>.<port>` for an individual port;
the value is `x, y`:

```properties title="current-l2-positions.properties"
#
#Thu Mar 26 11:44:51 EET 2026
sw-core-1=13, 1583
fw-1.eth0=16, 304
db-1.eth0=13, 122
```

If the file is missing, the generators say so and fall back to an automatic layout:

```
Properties file …/current-l2-positions.properties does not exit
```

That layout is legible for a handful of hosts and unusable for a rack of them, so for any real estate
the workflow is: run the editor once, arrange the boxes, press `s`, and commit the file. It is
plain text and merges reasonably, but two people rearranging the same diagram will conflict — treat it
as a single-owner file.

A legacy fallback name `l2positions.properties` (no prefix) is also honoured; prefer the prefixed form.

## Next

- [L2 diagram](/firewall-config/generators/l2-diagram/) — the commands that read these files.
- [Labels](/firewall-config/generators/labels/) — printable cable and host labels, coloured from `vlanColors`.
