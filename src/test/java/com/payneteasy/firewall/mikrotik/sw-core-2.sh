#@IgnoreInspection BashAddShebang
[admin@sw-core-02] > /export
# aug/08/2015 02:11:48 by RouterOS 6.27
# software id = W0Q4-3EVH
#
/interface ethernet
set [ find default-name=ether2 ] master-port=ether1
set [ find default-name=ether3 ] master-port=ether1
set [ find default-name=ether4 ] master-port=ether1
set [ find default-name=ether5 ] master-port=ether1
set [ find default-name=ether6 ] master-port=ether1
set [ find default-name=ether7 ] master-port=ether1
set [ find default-name=ether8 ] master-port=ether1
set [ find default-name=ether9 ] master-port=ether1
set [ find default-name=ether10 ] master-port=ether1
set [ find default-name=ether11 ] master-port=ether1
set [ find default-name=ether12 ] master-port=ether1
set [ find default-name=ether13 ] master-port=ether1
set [ find default-name=ether14 ] master-port=ether1
set [ find default-name=ether15 ] master-port=ether1
set [ find default-name=ether16 ] master-port=ether1
set [ find default-name=ether17 ] master-port=ether1
set [ find default-name=ether18 ] master-port=ether1
set [ find default-name=ether19 ] master-port=ether1
set [ find default-name=ether20 ] master-port=ether1
set [ find default-name=ether21 ] master-port=ether1
set [ find default-name=ether22 ] master-port=ether1
set [ find default-name=ether23 ] master-port=ether1
set [ find default-name=sfp1 ] disabled=yes
/interface ethernet switch
set drop-if-invalid-or-src-port-not-member-of-vlan-on-ports=ether1,ether23
/port
set 0 name=serial0
/interface ethernet switch egress-vlan-tag
add tagged-ports=ether23 vlan-id=200
add tagged-ports=ether23 vlan-id=100
add tagged-ports=ether23 vlan-id=300
/interface ethernet switch ingress-vlan-translation
add new-customer-vid=200 ports=ether1 sa-learning=yes
add new-customer-vid=100 ports=ether2 sa-learning=yes
add new-customer-vid=200 ports=ether3 sa-learning=yes
add new-customer-vid=300 ports=ether11 sa-learning=yes
add new-customer-vid=300 ports=ether12 sa-learning=yes
/interface ethernet switch vlan
add ports=ether2,ether23 vlan-id=100
add ports=ether1,ether3,ether23 vlan-id=200
add ports=ether11,ether12,ether23 vlan-id=300
/ip address
add address=10.0.0.106/16 interface=ether24 network=10.0.0.0
/ip route
add distance=1 gateway=10.0.0.1
/lcd
set backlight-timeout=never
/system clock
set time-zone-name=Europe/Moscow
/system identity
set name=sw-core-02

# =====================

