---
title: L2 diagram
description: Draw the physical cabling as SVG or PNG, and lay it out with the interactive editor.
sidebar:
  order: 3
---

Builds a graph of ports and cables from the `link:`, `port:` and `vlan:` fields, colours it per VLAN,
and renders it. Three commands share the same graph: two batch renderers and an interactive editor
for the layout.

## Usage

All three take the same arguments (`AbstractDirPrefixFilterCommand`):

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL2SvgDiagram    [-f FILTER] <dir> <prefix>
java -cp firewall-config.jar com.payneteasy.firewall.MainL2PngDiagram    [-f FILTER] <dir> <prefix>
java -cp firewall-config.jar com.payneteasy.firewall.MainL2DiagramEditor [-f FILTER] <dir> <prefix>
```

| Argument | Meaning |
|---|---|
| `<dir>` | The configuration directory. |
| `<prefix>` | Selects `<prefix>-l2-additions.yml` and `<prefix>-l2-positions.properties`, and names the output file. |
| `-f`, `--filter` | Groups and/or host-name prefixes to include. Default `internal,ipmi,internet`. |

| Command | Writes |
|---|---|
| `MainL2SvgDiagram` | `<prefix>-l2.svg` in the current directory |
| `MainL2PngDiagram` | `<prefix>-l2.png` in the current directory |
| `MainL2DiagramEditor` | `<prefix>-l2-positions.properties`, when you press `s` |

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL2SvgDiagram --filter internal,ipmi . current
cp current-l2.svg images/l2.svg
```

The renderers run headless; only the editor needs a desktop session.

## How the graph is built

- **Ports** come from every interface of every host that passes the filter, except those whose name
  starts with `bond`.
- **Cables** come from `link: <host>/<iface>`. The far end may also be addressed by port number
  (`<host>/<port>`).
- **Colour** comes from the port's `vlan:` looked up in `vlanColors`.
- **Extra and suppressed cables** come from `addedLinks` / `removedLinks`.

Declare each cable on one side only — normally on the switch, which makes it the single place the
physical layer is described:

```yaml title="hosts/ipmi/sw-core-1.yml"
- name: ether5
  ip:   skip
  port: 5
  link: web-1/eth0
  vlan: 202
```

Declaring both ends fails with `There are more than one interface … connected to …`.

The command echoes the cables it found, which is a quick way to catch a typo in a `link:`:

```
sw-core-1/ether1-> fw-1/eth0
sw-core-1/ether5-> web-1/eth0
sw-core-1/ether6-> web-1/ipmi
…
Wrote to file …/current-l2.svg
```

A cable whose far end does not exist is simply not drawn — no error. If a host or port is missing from
the picture, check this listing first.

## Layout

Node positions live in
[`<prefix>-l2-positions.properties`](/firewall-config/configuration/l2-additions/#prefix-l2-positionsproperties).
Without that file the renderers report

```
Properties file …/current-l2-positions.properties does not exit
```

and fall back to an automatic layout that is fine for a handful of hosts and unreadable for a rack of
them. For any real estate, arrange it once in the editor and commit the file.

## The editor

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL2DiagramEditor --filter internal,ipmi . current
```

A Swing window. Drag hosts and ports with the mouse; the rest is keyboard:

| Key | Action |
|---|---|
| `s` | Save positions to `<prefix>-l2-positions.properties` |
| `=` | Zoom in |
| `-` | Zoom out |
| `r` | Reload the configuration from disk |
| Shift + arrows | Shift the whole scheme |
| `q` | Quit (without saving) |

`q` does **not** save — press `s` first. `r` re-reads the host files, so you can edit YAML in another
window and see the effect without restarting. Because the file is written wholesale, two people
rearranging the same diagram will conflict; treat it as single-owner.

Once the layout is committed, the SVG and PNG commands are deterministic and belong in CI.

## Next

- [L2 additions and positions](/firewall-config/configuration/l2-additions/) — VLAN colours and manual cables.
- [Labels](/firewall-config/generators/labels/) — printable cable labels from the same graph.
- [Host provisioning](/firewall-config/generators/host-provisioning/#mikrotik) — switch VLAN commands from the same fields.
