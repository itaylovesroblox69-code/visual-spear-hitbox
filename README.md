# Visual Spear Hitbox

A client-side Fabric mod for Minecraft 1.21.11 that does one thing: while you hold a spear, it draws
that spear's hitbox with F3+B as an extra overlay. It is the hitbox for *using* the spear, not for
attacking with it.

It only changes the geometry the F3+B debug renderer draws. It does not touch the real hitbox,
attack range, reach, targeting, hit detection, entity size, collision or movement, and it never sends
anything to the server.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 or newer |
| Java | 21 |
| Fabric API | not required |
| Mod Menu and Cloth Config | optional, only for the in-game toggle |

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put the `visualspearhitbox-*.jar` from `build/libs` in your `mods` folder. See [Building](#building).
   Delete any older `spearhitboxdebug-*.jar` or `visualspearhitbox` jar first, two copies can silently
   load the wrong one.
3. Optional: install Fabric API, Mod Menu and Cloth Config for 1.21.11 to get the in-game toggle.
4. Start the game and press F3+B.

## Configuration

With Mod Menu and Cloth Config: mod list, Visual Spear Hitbox, config button, `Enabled`. It applies
straight away.

Without them, edit `config/visualspearhitbox.json`:

```json
{
  "enabled": true
}
```

## Building

```bash
./gradlew build    # or build1.21.11.bat on Windows
./gradlew test     # tests only
```

Needs JDK 21. The jar lands in `build/libs` as
`visualspearhitbox-<version>+1.21.11.jar`, using `mod_version` from `gradle.properties`.

## How a spear is detected

The held item is checked against the vanilla `#minecraft:spears` tag, which holds exactly the seven
spears (wooden, stone, copper, iron, golden, diamond, netherite). Item ids always reach the client,
so this holds up on any server. An item carrying `minecraft:attack_range` also counts, as a fallback
for modded spears outside the tag.

The `attack_range` component on its own was not enough. It is static item data, and the protocol
only sends components that change per stack, so on a vanilla server a held spear arrives with no
`attack_range` at all. The mod then thought it was not a spear and drew nothing, which is why it
worked on some servers and not others.

The box size is still the game's own number, `entityAttackRange().hitboxMargin()`, which is 0.125 for
vanilla spears. If it was never synced the game reports 0, and the mod draws at 0.125 instead of
giving up. Holding the spear is the gate, the margin only decides the size.

The trident is not a spear. It is not in the tag and has no `attack_range` in 1.21.11.

## What shows up in the log

One line per held spear, nothing at all for any other item:

```
[visualspearhitbox] F3+B spear box: held=minecraft:iron_spear margin=0.125
```

That is enough to work out why F3+B drew nothing. A `margin=0.0` means the server never sent
`attack_range` for that item, and the mod is drawing at 0.125 anyway.

The held item is re-read from the game every frame instead of being watched for swaps, because a swap
is not the only way to end up holding a spear. A server or another mod can rewrite the slot you are
already holding, and since `ItemStack` is mutable that can arrive as the same stack object with
different contents. So the "did the held item change" bookkeeping compares contents
(`ItemStack.matches`: item, count, components) and not stack identity. Identity got both cases wrong,
the same object with new contents and a new object with identical contents.

## Multiplayer

Vanilla hides every debug screen entry while the server has `reducedDebugInfo` on, which a lot of
public and PvP servers force. F3+B's `entity_hitboxes` entry goes with it, the renderer is never
built, and there is nothing to hook into.

One mixin puts that single entry back, and only when you turned it on yourself. Every other entry
still follows the server's setting.

## Scripts

| | |
|---|---|
| `build1.21.11.bat` | build the jar, `clean` to wipe outputs first, `run` for the dev client |
| `commit.bat` | asks for a message, then stages everything, commits and pushes. `commit.bat --no-push` skips the push |
| `backup.bat` | zips the project into `backups\` so a change can be undone |
| `restore-backup.bat` | puts a backup back over the project, newest first |

`backups\` is in `.gitignore`, so snapshots never end up in a commit.

## Tests

`./gradlew test` runs 18 JUnit 5 tests on a plain JVM.

`HitboxLogicTest` covers the inflate decision, including the 0 margin fallback for servers that do not
send `attack_range`.

`SpearDetectionTest` checks the vanilla tag data file lists exactly the seven spears, then runs the
detection against the real 1.21.11 registries: every non-spear case, the trident, the `attack_range`
fallback, and the held item change detection that catches a slot becoming a spear with no swap.

## License

MIT. See [LICENSE](LICENSE).
