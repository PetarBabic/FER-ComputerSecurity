#! /bin/sh
#
# Dodajte ili modificirajte pravila na oznacenim mjestima ili po potrebi (i želji) na 
# nekom drugom odgovarajucem mjestu (pazite: pravila se obrađuju slijedno!)
#
IPT=/sbin/iptables

$IPT -P INPUT DROP
$IPT -P OUTPUT DROP
$IPT -P FORWARD DROP

$IPT -F INPUT
$IPT -F OUTPUT
$IPT -F FORWARD

$IPT -A INPUT   -m state --state ESTABLISHED,RELATED -j ACCEPT
$IPT -A OUTPUT  -m state --state ESTABLISHED,RELATED -j ACCEPT
$IPT -A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT

#
# za potrebe testiranja dozvoljen je ICMP (ping i sve ostalo)
#
$IPT -A INPUT   -p icmp -j ACCEPT
$IPT -A FORWARD -p icmp -j ACCEPT
$IPT -A OUTPUT  -p icmp -j ACCEPT

#
# Primjer "anti spoofing" pravila na sucelju eth0
#
#$IPT -A INPUT   -i eth0 -s 127.0.0.0/8  -j DROP
#$IPT -A FORWARD -i eth0 -s 127.0.0.0/8  -j DROP
#$IPT -A INPUT   -i eth0 -s 192.0.2.0/24  -j DROP
#$IPT -A FORWARD -i eth0 -s 192.0.2.0/24  -j DROP
#$IPT -A INPUT   -i eth0 -s 192.168.0.0/24  -j DROP
#$IPT -A FORWARD -i eth0 -s 192.168.0.0/24  -j DROP
#$IPT -A INPUT   -i eth0 -s 192.168.1.2  -j DROP
#$IPT -A FORWARD -i eth0 -s 192.168.1.2  -j DROP

#
# Web poslužitelju (tcp/80 i tcp/443) pokrenutom na www se može 
# pristupiti s bilo koje adrese (iz Interneta i iz lokalne mreže), ...
#
$IPT -A FORWARD -i eth0 -o eth2 -d 192.0.2.10 -p tcp --dport 80 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth0 -o eth2 -d 192.0.2.10 -p tcp --dport 443 -m state --state NEW -j ACCEPT

$IPT -A FORWARD -i eth1 -o eth2 -d 192.0.2.10 -p tcp --dport 80 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth1 -o eth2 -d 192.0.2.10 -p tcp --dport 443 -m state --state NEW -j ACCEPT


#
# DNS poslužitelju (udp/53 i tcp/53) pokrenutom na dns se može 
# pristupiti s bilo koje adrese (iz Interneta i iz lokalne mreže), ...
#

$IPT -A FORWARD -o eth2 -d 192.0.2.20 -p udp --dport 53 -j ACCEPT
$IPT -A FORWARD -o eth2 -d 192.0.2.20 -p tcp --dport 53 -j ACCEPT

#
# ... a SSH poslužiteljima na www i dns samo s admin iz lokalne mreže "Private"
#

$IPT -A FORWARD -i eth1 -o eth2 -s 192.168.0.20 -d 192.0.2.10 -p tcp --dport 22 -m state --state NEW -j ACCEPT

$IPT -A FORWARD -i eth1 -o eth2 -s 192.168.0.20 -d 192.0.2.20 -p tcp --dport 22 -m state --state NEW -j ACCEPT

#
# S www je dozvoljen pristup poslužitelju database (Private) na TCP portu 10000 te pristup 
# DNS poslužiteljima u Internetu (UDP i TCP port 53).
#

$IPT -A FORWARD -i eth2 -o eth1 -s 192.0.2.10 -d 192.168.0.10 -p tcp --dport 10000 -m state --state NEW -j ACCEPT

$IPT -A FORWARD -i eth2 -o eth0 -s 192.0.2.10 -p tcp --dport 53 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth2 -o eth0 -s 192.0.2.10 -p udp --dport 53 -m state --state NEW -j ACCEPT

$IPT -A FORWARD -s 192.0.2.20 -o eth0 -p tcp --dport 53 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -s 192.0.2.20 -o eth0 -p tcp --dport 53 -m state --state NEW -j ACCEPT

#
# ... S www je zabranjen pristup svim ostalim adresama i poslužiteljima.
#
# <--- Dodajte pravila (ako je potrebno)

#
#
# Pristup svim ostalim adresama i poslužiteljima u DMZ je zabranjen.
#
# <--- Dodajte pravila (ako je potrebno)

#
# Pristup SSH poslužitelju na cvoru database, koji se nalazi u lokalnoj mreži "Private", 
# dozvoljen je samo racunalima iz mreže "Private".
#
# <--- Dodajte pravila (ako je potrebno)


#
# Web poslužitelju na cvoru database, koji sluša na TCP portu 10000, može se pristupiti
# iskljucivo s racunala www koje se nalazi u DMZ (i s racunala iz mreže "Private").
#
# <--- Dodajte pravila (ako je potrebno)

#
# S racunala database je zabranjen pristup svim uslugama u Internetu i u DMZ.
#

#$IPT -A FORWARD -s 192.168.0.10 -o eth2 -j DROP
#$IPT -A FORWARD -s 192.168.0.10 -o eth0 -j DROP

# <--- Na odgovarajuce mjesto dodajte pravila (ako je potrebno)

# Zabranjen je pristup svim ostalim uslugama na poslužitelju database (iz Interneta i iz DMZ)
#
# <--- Na odgovarajuce mjesto dodajte pravila (ako je potrebno)

#
# S racunala iz lokalne mreže "Private" (osim s database) se može pristupati svim racunalima 
# u Internetu ali samo korištenjem protokola HTTP (tcp/80 i tcp/443) i DNS (udp/53 i tcp/53).
#

$IPT -A FORWARD -o eth0 -s 192.168.0.10 -j DROP

$IPT -A FORWARD -i eth1 -o eth0 -s 192.168.0.0/24 -p tcp --dport 80 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth1 -o eth0 -s 192.168.0.0/24 -p tcp --dport 443 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth1 -o eth0 -s 192.168.0.0/24 -p udp --dport 53 -m state --state NEW -j ACCEPT
$IPT -A FORWARD -i eth1 -o eth0 -s 192.168.0.0/24 -p tcp --dport 53 -m state --state NEW -j ACCEPT

#$IPT -A FORWARD -o eth0 -s 192.168.0.10 -j DROP

# Za potrebe administriranja, s admin se može pristupiti SSH poslužitljima na www i dns.
#
# <--- Dodajte pravila (ako je potrebno)


#
# Pristup iz vanjske mreže u lokalnu LAN mrežu je zabranjen.
#
# <--- Dodajte pravila (ako je potrebno)

#
# Na FW je pokrenut SSH poslužitelj kojem se može pristupiti samo iz lokalne mreže "Private"
# i to samo sa cvora admin.
#

$IPT -A INPUT -i eth1 -s 192.168.0.20 -p tcp --dport 22 -m state --state NEW -j ACCEPT

#
# Pristup svim ostalim uslugama (portovima) na cvoru FW je zabranjen.
#
# <--- Dodajte pravila (ako je potrebno)
