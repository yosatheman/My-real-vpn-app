# NetForge — Clean-Room HTTP Injector-Style VPN Client for Android

**NetForge** is an original, clean-room Android VPN client engineered in Kotlin and Jetpack Compose (Material 3). It enables custom tunnel connectivity, proxy chaining, HTTP payload injection, TLS Server Name Indication (SNI) fronting, WebSocket upgrades, and secure encrypted configuration management via the proprietary `.nf` format.

> **Clean-Room Guarantee**: NetForge contains zero code, strings, asset references, or logic borrowed from existing injection applications (e.g., HTTP Injector, HTTP Custom, NetMod, SSC, Dark Tunnel, NPVT). Every module was designed and written from first principles.

---

## 1. Architectural Overview

NetForge follows Clean Architecture and MVVM principles with strict isolation of cryptographic materials:

```
NetForge Architecture:
├── crypto/             # Cryptographic primitives (Argon2id KDF, AES-256-GCM, KeystoreHelper)
├── data/
│   ├── db/             # Room Database with AES-256-GCM encrypted payload storage
│   ├── file/           # .nf binary serializer & deserializer (NfFileReader, NfFileWriter)
│   ├── prefs/          # DataStore settings & DNS preferences (SecurePrefs)
│   └── repo/           # ConfigRepository
├── domain/
│   ├── model/          # Config, Payload, TunnelMode, DnsConfig, TunnelState
│   ├── payload/        # PayloadEngine with longest-first placeholder processing & UA rotation
│   └── tunnel/         # TunnelEngine orchestrator (SSH, HTTP CONNECT, SNI, WS, SOCKS5, Direct)
├── service/            # NetForgeVpnService (systemExempted), TunnelNotification
└── ui/
    ├── components/     # StatusCard, ConfigCard, PasswordField, StrengthMeter
    ├── screens/        # HomeScreen, ConfigListScreen, PayloadEditorScreen, Detail, Export, Logs, Settings
    ├── nav/            # NetForgeNavHost & 4-tab bottom navigation
    └── theme/          # Violet (#8B5CF6) & Dark Background (#0B0B0F) M3 Theme
```

### Zero-Telemetry & Zero-Tracking
- No analytics SDKs (no Firebase Analytics, Google Analytics, Flurry, Mixpanel, etc.).
- No ad networks or monetization libraries.
- No remote telemetry reporting or crash tracking servers.
- All tunnel handshake and socket data stay local on the device.

---

## 2. The `.nf` Encrypted Config Specification

Configurations can be exported and imported as encrypted `.nf` files.

### Binary Layout (36-Byte Header + Ciphertext + Tag)

| Offset   | Field         | Size     | Value / Description |
|----------|---------------|----------|---------------------|
| `0..3`   | Magic Bytes   | 4 bytes  | `0x4E 0x46 0x52 0x47` (`"NFRG"`) |
| `4`      | Version       | 1 byte   | `0x01` |
| `5`      | KDF ID        | 1 byte   | `0x01` (`Argon2id` v1.3) |
| `6`      | Cipher ID     | 1 byte   | `0x01` (`AES-256-GCM` with 128-bit auth tag) |
| `7`      | Reserved      | 1 byte   | `0x00` |
| `8..23`  | Salt          | 16 bytes | Cryptographically secure random salt |
| `24..35` | Nonce / IV    | 12 bytes | Cryptographically secure random nonce |
| `36..N`  | Ciphertext    | Variable | `AES-256-GCM(gzip(plaintext_ini))` |
| `N..N+15`| Auth Tag      | 16 bytes | GCM Poly1305 authentication tag |

### Authenticated Additional Data (AAD)
To prevent header truncation, bit-flipping, or cipher downgrades, the AAD is bound to the GCM authentication tag:
```
AAD = magic (4) || version (1) || kdf_id (1) || cipher_id (1)  (7 bytes total)
```

### Key Derivation (Argon2id)
```
Key = Argon2id(
  passphrase  = user_passphrase (UTF-8 bytes),
  salt        = salt (16 bytes),
  iterations  = 3,
  memory      = 65536 KiB (64 MiB),
  parallelism = 2 lanes,
  output_len  = 32 bytes (256-bit key)
)
```

---

## 3. Plaintext Config Schema

Before encryption, config contents are represented in standard INI format:

```ini
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
```

---

## 4. Payload Injection Syntax Reference

The `PayloadEngine` processes raw HTTP templates using a strict **longest-first substitution order** to prevent partial matches:

| Tag | Replacement Value | Example / Description |
|-----|-------------------|-----------------------|
| `[host_port]` | Remote host:port | `198.51.100.1:443` |
| `[front_host]`| SNI / Fronting host if set, else target host | `cdn.cloudflare.net` |
| `[real_host]` | Remote server host | `198.51.100.1` |
| `[host]` | Remote server host | `198.51.100.1` |
| `[port]` | Remote server port | `443` |
| `[protocol]`| `https` if TLS is active, else `http` | `https` |
| `[ssh_user]`| SSH username configured in payload | `netforge_user` |
| `[ssh_pass]`| SSH password configured in payload | `demo_password` |
| `[random]` | 16-character alphanumeric string | `k8Fa92B1mPx4LQz7` |
| `[ua]` | Rotated modern desktop/mobile User-Agent | `Mozilla/5.0 (Linux; Android 14; Pixel 8)...` |
| `[crlf]` | Carriage return + line feed (`\r\n`) | Standard HTTP header terminator |
| `[cr]` | Carriage return (`\r`) | `\r` |
| `[lf]` | Line feed (`\n`) | `\n` |

### Payload Examples

**1. WebSocket Upgrade over SNI:**
```
GET / HTTP/1.1[crlf]Host: [front_host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: [ua][crlf][crlf]
```

**2. Proxy CONNECT Handshake:**
```
CONNECT [host_port] HTTP/1.1[crlf]Host: [host_port][crlf]Proxy-Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]
```

---

## 5. Supported Tunneling Modes

1. **SSH**: Full SSHv2 tunnel with password authentication and socket protection.
2. **HTTP CONNECT**: Proxy chain handshake with HTTP status code validation (200 OK / Connection Established).
3. **SNI Fronting**: TLS connection with custom `server_names` indication via `SNIHostName`.
4. **WebSocket Upgrade**: RFC 6455 upgrade handshake with base64 `Sec-WebSocket-Key` generation and `101 Switching Protocols` verification.
5. **SOCKS5 Chain**: RFC 1928 SOCKS5 protocol with authentication and IPv4/domain CONNECT command forwarding.
6. **Direct TCP**: Raw socket streaming with initial payload injection.

---

## 6. How to Import the Sample Config

1. Open NetForge on your Android device or streaming emulator.
2. Navigate to the **Configs** tab using the bottom navigation bar.
3. Tap the **"Import .nf"** floating action button.
4. Select the included `sample_encrypted.nf` file.
5. In the passphrase prompt, enter:
   ```
   netforge2024
   ```
6. Tap **"Decrypt & Import"**. The Argon2id key derivation will run, the GCM auth tag will be verified, and the config will be decrypted and stored in your device's encrypted database.
7. Return to the **Home** tab and tap the big circular **"CONNECT"** button to start the VPN tunnel!

---

## 7. Threat Model & Cryptographic Guarantees

| Threat | Mitigation in NetForge |
|--------|------------------------|
| **Device Extraction / Physical theft** | Sensitive configs are encrypted with AES-256-GCM using hardware-backed keys via Android KeyStore. Plaintext is never stored in plain files or unencrypted SQLite tables. |
| **Tampered Config Files** | 128-bit GCM tag verification rejects any bit manipulation in header, ciphertext, or AAD with an exception before decompression. |
| **Passphrase Cracking** | Argon2id v1.3 memory-hard KDF with 64 MB memory cost and 3 iterations renders GPU and ASIC brute-force attacks infeasible. |
| **Memory Dump Analysis** | All cryptographic key `ByteArray` buffers and user passphrase `CharArray` buffers are wiped using `Arrays.fill(..., 0)` immediately after use. Config data is purged from memory on disconnect. |
| **Log Leakage** | No cryptographic keys, plaintext passwords, or raw auth tokens are emitted to Android Logcat. ProGuard/R8 strips debug logs in release APK builds. |
| **VPN Routing Loops** | All outgoing sockets created by the tunneling engine invoke `VpnService.protect(socket)` so outbound traffic bypasses the VPN tunnel interface. |

---

## 8. Build & Verification

To compile and verify the Android application:

```bash
# Clean compilation
gradle assembleDebug

# Run complete unit tests (AesGcmTest, Argon2KdfTest, NfFileTest, PayloadEngineTest)
gradle testDebugUnitTest
```
