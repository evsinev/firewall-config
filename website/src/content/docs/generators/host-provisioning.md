---
title: Host provisioning
description: BIND zones, keepalived, network-interface files and RouterOS VLAN commands from the same description.
sidebar:
  order: 5
---

Five smaller generators that emit host and switch configuration from the same model. All take
positional arguments in the older style — no `--help`, and a one-line error if the count is wrong.
Each has hard-coded assumptions worth knowing before you use it.

## BIND

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainBind <config-dir> <domain-name> <output-dir>
```

```sh
mkdir -p bind-out
java -cp firewall-config.jar com.payneteasy.firewall.MainBind . demo.example.com bind-out
```

Writes into `<output-dir>`:

| File | Contents |
|---|---|
| `<domain>.zone` | An `A` record per host in group `internal`, at its default address |
| `<reversed-/24>.zone` | A `PTR` zone per `/24` in use — `20.20.10.zone`, `100.51.198.zone`, … |
| `static.zone` | An `A` record per **service `name`**, at the default address of the host running it |
| `zones.conf` | `zone { … }` stanzas for all of the above |

```text title="bind-out/demo.example.com.zone"
adm-1         IN A 10.20.20.31
db-1          IN A 10.20.22.21
fw-1          IN A 10.20.20.11
fw-2          IN A 10.20.20.12
proxy-1       IN A 10.20.2.21
web-1         IN A 10.20.20.21
```

`static.zone` is the interesting one: it gives every named service a DNS name, so an application can
connect to `main-db` rather than to an address.

Two caveats. **Only group `internal` is considered** — the group name is hard-coded, so hosts in
`ipmi`, `external` or anything else are absent. And because
[`services_links`](/firewall-config/configuration/services/#sharing-definitions-with-services_links)
copies a *named* definition onto many hosts, shared names produce one `A` record per host:

```text title="bind-out/static.zone"
ssh       IN A 10.20.20.11
ssh       IN A 10.20.20.12
ssh       IN A 10.20.2.21
node-exporter       IN A 10.20.22.21
main-db       IN A 10.20.22.21
web-app       IN A 10.20.20.21
```

That is valid (round-robin) but almost certainly not what you want for `ssh`. Serve `static.zone`
selectively, or keep shared base services out of it.

The generated zones also contain a `;; todo find DNS servers!` marker and point `NS` at a placeholder —
review before serving.

## keepalived

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainKeepalived <config-dir> <host>
```

Prints a shell script to stdout that backs up and rewrites `/etc/keepalived/keepalived.conf`, with one
`vrrp_instance` per interface that carries a `vip` or `vips`:

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainKeepalived . fw-1
```

```text
vrrp_instance V_100 {
    state             MASTER
    interface         eth0.100
    virtual_router_id 100
    priority          {{vrrp_priority}}
    advert_int        1

    authentication {
        auth_type PASS
        auth_pass 1111
    }

    virtual_ipaddress {
        198.51.100.10 # gw.demo.example.com
    }
}
```

The instance name and `virtual_router_id` come from the **VLAN suffix of the interface name**, so
VIP-bearing interfaces must be named `<iface>.<vlan>` — `eth0.100` gives router id `100`.

The literal `{{vrrp_priority}}` in the output is **deliberate**: the template switches Mustache
delimiters around that line so the placeholder survives into the generated file, to be filled in by
whatever templating runs next (Ansible, Jinja). The consequence is that the `vrrpPriority:` values in
your description do **not** reach `keepalived.conf` — you set the priority per host in your
provisioning layer instead. `auth_pass` is likewise a hard-coded placeholder (`1111`), so the output
is a starting point rather than a deployable file. See
[Limitations](/firewall-config/internals/limitations/#vrrppriority-does-not-reach-keepalivedconf).

## Linux interface files

```sh
# RHEL / CentOS: /etc/sysconfig/network-scripts/ifcfg-<iface>
java -cp firewall-config.jar com.payneteasy.firewall.MainLinuxNetworkScript <config-dir> <host> <iface>

# Debian / Ubuntu: /etc/network/interfaces.d/<iface>, plus /etc/hosts, /etc/hostname, /etc/resolv.conf
java -cp firewall-config.jar com.payneteasy.firewall.MainUbuntuBaseSetup <config-dir> <host> <iface>
```

Both print a shell script that backs up the existing file and `tee`s the new one:

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainLinuxNetworkScript . fw-1 eth0.202
```

```text
# fw-1 eth0.202
cp /etc/sysconfig/network-scripts/ifcfg-eth0.202 /tmp/ifcfg-eth0.202-$(date +%s)
tee /etc/sysconfig/network-scripts/ifcfg-eth0.202 <<EOF
DEVICE=eth0.202
BOOTPROTO=none
ONBOOT=yes
IPADDR=10.20.20.11
NETMASK=255.255.255.0
# vlan = 202
VLAN=yes
EOF
```

`MainLinuxNetworkScript` also checks that the interface-name suffix matches the VLAN found through the
linked switch port, which catches a `eth0.202` cabled to a port carrying VLAN 203.

`MainUbuntuBaseSetup` additionally emits `/etc/hosts`, `/etc/hostname` and `/etc/resolv.conf` — but the
resolver settings are **hard-coded** to one site (`search idea`, `nameserver 10.2.2.21/22`) and ignore
your description. Edit the output, or take only the interface part.

## Mikrotik

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainMikrotik <config-dir> <host> [vlan]
```

RouterOS commands for a switch, derived from `port:`, `vlan:` and `link:`. Pass a VLAN to emit only
that one:

```sh
java -cp firewall-config.jar com.payneteasy.firewall.MainMikrotik . sw-core-1
```

```text
# sw-core-1 206

/interface ethernet switch egress-vlan-tag
add tagged-ports=ether1 vlan-id=206

/interface ethernet switch ingress-vlan-translation
add new-customer-vid=206 ports=ether4,ether6,ether8,ether10 sa-learning=yes

/interface ethernet switch vlan
add ports=ether1,ether4,ether6,ether8,ether10 vlan-id=206
```

Trunk ports are tagged, access ports get ingress translation, and a port with no VLAN of its own
inherits the VLAN of the switch port it is linked to. The commands *add* configuration — they do not
remove what is already on the device, so apply them to a switch whose switch-chip VLAN configuration
you have cleared, or reconcile by hand.

## Next

- [Labels](/firewall-config/generators/labels/) — printable host and cable labels.
- [Limitations](/firewall-config/internals/limitations/) — the hard-coded assumptions collected in one place.
