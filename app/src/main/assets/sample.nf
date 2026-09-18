# NetForge config v1
[meta]
name = Sample Cloud SSH
version = 1

[server]
host = 198.51.100.1
port = 443

[payload]
mode = ssh
sni = cdn.cloudflare.net
payload = GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf][crlf]
ssh_user = netforge_user
ssh_pass = demo_password

[dns]
primary = 1.1.1.1
secondary = 8.8.8.8

[advanced]
keepalive = 60
mtu = 1500
udp = false
