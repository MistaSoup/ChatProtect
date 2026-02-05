# ChatProtect

A powerful, Folia-compatible anti-spam plugin for Minecraft 1.21.x with intelligent filtering, auto-mute capabilities, and private messaging integration.

## Features

- **Blocked Word Filter** - Automatic variation detection (leetspeak, symbols, etc.)
- **Duplicate Message Detection** - Using Levenshtein distance algorithm
- **Anti-Spam Kick** - Automatically kicks spammers
- **Auto-Mute System** - Mutes repeat offenders
- **Private Messaging** - Built-in /msg, /w, /tell, /pm, /dm, /r, /reply
- **Colored Chat** - Messages with `>` prefix appear in custom colors
- **Persistent Storage** - Mutes survive server restarts
- **Folia Compatible** - Full region-based threading support

## Installation

1. Place `ChatProtect-1.0.0.jar` in `plugins/` folder
2. Start server
3. Configure `plugins/ChatProtect/config.yml`
4. Use `/cp reload` to apply changes

## Commands

- `/chatprotect reload` (aliases: `/cp`, `/antispam`) - Reload config
- `/msg <player> <message>` - Send private message
- `/r <message>` - Reply to last PM

## Permissions

- `chatprotect.admin` - Admin commands (default: op)
- `chatprotect.bypass` - Bypass all checks (default: false)

## Building

```bash
mvn clean package
```

Output: `target/ChatProtect-1.0.0.jar`

---

**Author:** MistaSoup  
**Version:** 1.0.0  
**Minecraft:** 1.21.x  
**API:** Folia/Paper
