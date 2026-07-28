---
title: L3 diagram
description: Render every network and the hosts on it through nwdiag.
sidebar:
  order: 2
---

Groups every interface address into its `/24`, names each network from `networks.yml`, and writes an
[nwdiag](https://github.com/blockdiag/nwdiag) source file — optionally rendering and opening it.

## Usage

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL3Diagram [options] <dir> <prefix>
```

| Option / argument | Default | Meaning |
|---|---|---|
| `<dir>` | — | The configuration directory. |
| `<prefix>` | — | Config file prefix. Unused by this command, but positionally required — pass `current`. |
| `-f`, `--filter` | `internal,ipmi,internet` | Comma-separated groups **and/or** host-name prefixes to include. |
| `--run-nwdiag` | `true` | Run `nwdiag` on the result and `open` the PNG. |
| `-h`, `--help` | | Usage. |

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainL3Diagram \
     --run-nwdiag=false --filter internal,ipmi . current
```

The source is printed to stdout **and** written to `target/network.diag`, relative to the current
directory — a fixed path, so `target/` must exist.

The `--filter` values are matched first against group names and then as host-name prefixes, so
`--filter internal,ipmi,fw-` is legal and additive. Filtering is how you get a readable picture: an
estate with dozens of IPMI controllers is best drawn without them.

## Output

```text title="target/network.diag"
nwdiag {

    group {
        color = "#FFCDD2";
        fw-1;
        fw-2;
    }

    network "internet" {
        address = "198.51.100.0/24";

            fw-2 [address = "198.51.100.12"];
            fw-1 [address = "198.51.100.11"];
    }
    network "app" {
        address = "10.20.20.0/24";

            adm-1 [address = "10.20.20.31"];
            web-1 [address = "10.20.20.21"];
            fw-2 [address = "10.20.20.12"];
            fw-1 [address = "10.20.20.11"];
    }
    network "db" {
        address = "10.20.22.0/24";

            fw-2 [address = "10.20.22.12"];
            fw-1 [address = "10.20.22.11"];
            db-1 [address = "10.20.22.21"];
    }
    …
}
```

`internet` is always emitted first; the rest follow alphabetically. Interfaces with `ip: skip` are
absent, since they have no layer-3 address. A host appears once per network it has an address in,
which is what makes multi-homed firewalls read as the junctions they are.

## `nwdiag-custom.diag`

The contents of this file are injected verbatim at the top of the `nwdiag { … }` block, before any
generated network. Use it for anything the model does not carry — visual grouping, colours, notes:

```text title="nwdiag-custom.diag"
    group {
        color = "#FFCDD2";
        fw-1;
        fw-2;
    }
```

The file must exist; leave it empty if you have nothing to add. Its contents are not validated, so an
nwdiag syntax error here surfaces only when `nwdiag` runs.

## Rendering

The generator only produces the source. To get a picture you need `nwdiag` installed
(`apt install python3-nwdiag`, after which the binary is `nwdiag3`):

```sh
# let the generator do it (and open the PNG)
java -cp firewall-config.jar com.payneteasy.firewall.MainL3Diagram --filter internal,ipmi . current

# or render yourself, e.g. to SVG for committing
mkdir -p target
java -cp firewall-config.jar com.payneteasy.firewall.MainL3Diagram \
     --run-nwdiag=false --filter internal,ipmi . current
cd target && nwdiag3 -a -T svg network.diag
cp target/network.svg images/l3.svg
```

With `--run-nwdiag=true` the command shells out to `nwdiag -a --no-transparency` and then `open` —
convenient on a workstation, wrong in CI. Pass `--run-nwdiag=false` in any pipeline, and note that CI
needs `mkdir -p target` first.

Committing the rendered SVG into the description repository is worth the noise in diffs: it gives
reviewers a picture next to the change, and gives the wiki something to embed.

## Next

- [L2 diagram](/firewall-config/generators/l2-diagram/) — the physical layer.
- [Protocols and networks](/firewall-config/configuration/protocols-and-networks/) — `networks.yml`, which names what you see here.
