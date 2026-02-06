# ChatProtect

**Intelligent anti-spam plugin for Minecraft 1.21.x with smart filtering and auto-moderation**

[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/MistaSoup/ChatProtect) [![Folia](https://img.shields.io/badge/folia-compatible-brightgreen)](https://papermc.io/software/folia)

---

## Features

### 🛡️ Smart Spam Detection
- **Similarity Matching** - Detects spam even with typos using Levenshtein distance algorithm
  - `"hello everyone"` vs `"helo everyone"` → 93% similar → Blocked
- **Blocked Words** - Auto-detects variations: `fuck`, `f*ck`, `fvck`, `fu<k` all blocked
  - Supports leetspeak & substitutions: `@→a`, `$→s`, `0→o`, `3→e`
- **Duplicate Prevention** - Blocks repeated messages with configurable threshold
- **Anti-Spam Kick** - Auto-kicks rapid spammers (default: 7 msgs in 5 sec)

### ⚖️ Auto-Mute System
- Automatically mutes repeat offenders (default: 3 kicks in 10 min = 5 min mute)
- **Persistent** - Survives server restarts
- **Smart Timer** - Pauses when player disconnects, resumes on rejoin
- Configurable thresholds and durations

### 💬 Private Messaging
- Built-in PM commands: `/msg`, `/w`, `/tell`, `/pm`, `/dm`
- Reply tracking: `/r` and `/reply` remember last conversation
- All anti-spam checks apply to PMs
- Customizable colors and formats
- Option to let muted players receive (but not send) messages

### 🎨 Chat Features
- **Colored Messages** - Prefix messages with `>` for custom color (default: green)
- **Silent Blocking** - No notification when messages are blocked
- **Permission Bypass** - Trusted players skip all checks

### ⚙️ Technical
- **Folia Compatible** - Full region-based threading support
- **Performance** - <1ms overhead per message
- **Hot Reload** - `/cp reload` updates config instantly
- **No Database** - Lightweight YAML storage

---

## Quick Start

1. Download `ChatProtect-1.0.0.jar`
2. Place in `plugins/` folder  
3. Restart server
4. Configure `plugins/ChatProtect/config.yml`
5. Run `/cp reload`

**Requirements:** Minecraft 1.21.x • Folia/Paper • Java 21+

---

## Commands

| Command | Description |
|---------|-------------|
| `/cp reload` | Reload config |
| `/msg <player> <msg>` | Send private message |
| `/r <msg>` | Reply to last PM |

---

## Permissions

| Permission | Description |
|------------|-------------|
| `chatprotect.admin` | Use `/cp reload` |
| `chatprotect.bypass` | Skip all anti-spam checks |

---

## Configuration Examples

**Similarity Detection:**
```yaml
similarity-threshold: 75      # 0-100%, how similar = duplicate
max-repeats: 2                # Allow 2 repeats before blocking
cooldown-seconds: 30          # Block duration
```

**Auto-Mute:**
```yaml
kick-threshold: 3             # 3 kicks...
kick-window-minutes: 10       # ...in 10 min...
mute-duration-seconds: 300    # ...= 5 min mute
allow-receive-pm: true        # Can muted players receive PMs?
```

**Blocked Words:**
```yaml
blocked-words:
  enabled: true
  word-list:
    - fuck
    # Detects: f*ck, fvck, fu<k, f@ck, etc.
```

---

## How It Works

**Message Flow:**
```
Message → Bypass? → Muted? → Spam Kick? → Blocked Word? → Duplicate? → Send/Block
```

**Similarity Example:**
```
"hello" vs "helo" = 1 char different = 80% similar
Threshold: 75%
Result: 80% ≥ 75% → BLOCKED
```

**Auto-Mute Timeline:**
```
10:00 - Spam → Kicked (1st)
10:05 - Spam → Kicked (2nd)  
10:08 - Spam → Kicked (3rd) + Muted 5min
10:09 - Try chat → "Muted. 291s remaining"
10:13 - Disconnect (timer pauses at 51s)
11:00 - Reconnect (timer resumes at 51s)
11:01 - Mute expires ✓
```

---

## Examples

**Duplicate Detection:**
```
✅ "hello"     (sent)
✅ "hello"     (1st repeat)
✅ "hello"     (2nd repeat)
❌ "hello"     (3rd repeat - BLOCKED)
```

**Blocked Words:**
```
❌ fuck, f*ck, fvck, fu<k, f@ck, F.U.C.K
✅ f, off, focus  (too short or not similar)
```

**Private Messages:**
```
/msg Steve hey     → [You -> Steve] hey
/r hi there        → [You -> Steve] hi there
```

**Colored Chat:**
```
hello              → white text
>hello             → green text
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Messages not blocked | Check `chatprotect.bypass` permission, lower threshold to 70 |
| Players kicked too easily | Increase `message-threshold` or `time-window-seconds` |
| Mutes not saving | Check file permissions on `mutes.yml` |
| Single chars blocked | Update to v1.0.0+ |

---

## Building

```bash
mvn clean package
```
Output: `target/ChatProtect-1.0.0.jar`

---

## Support

- **Issues:** [GitHub Issues](https://github.com/yourusername/ChatProtect/issues)
- **Source:** [GitHub](https://github.com/yourusername/ChatProtect)

---

**Author:** MistaSoup | **Version:** 1.0.0 | **License:** MIT
