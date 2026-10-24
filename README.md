# Manbear – Forge 1.20.1 horror mod

Requires: Minecraft 1.20.1, Forge 47.x, **GeckoLib 4.4.9+** (4.x).

## Build
1. Install JDK 17.
2. Get a Gradle wrapper: copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the official
   Forge 1.20.1 MDK into this folder (or run `gradle wrapper --gradle-version 8.1.1` if you have Gradle).
3. `./gradlew build` -> jar in `build/libs/`.  `./gradlew runClient` to test.
4. Drop the jar + GeckoLib into `mods/`.

## Your assets
`geo/manbear.geo.json`, `textures/entity/manbear.png` and `manbear_eyes.png` are byte-for-byte your files.
`animations/manbear.animation.json` is your file plus two derived clips: `crouch_walk` (walk cycle in the
hunt posture) and `crawl` (slowed `run2` gait), and `walk` is set to loop. Nothing in the textures was touched.
Model scale is `ManbearRenderer.MODEL_SCALE` (0.7 -> ~3 blocks tall, matching the 5-block hitbox).

## Testing quickly (OP)
* Spawn egg in the creative tab, or `/manbear spawn`
* `/manbear mode stalk|hide|peek|fake_chase|chase|lay_trap|ambush|retreat`
* `/manbear spawn [chaser|faker|trapper|sleeper]` forces that kind of spawn (also `behind`, `lurer`, `path`, `fog`, `cave`, `lurker`, `petnapper`, `lightning`, `intimidation`)
* `/manbear info`, `/manbear profile` (what he has learned about you), `/manbear remove`, `/manbear reset`
* Config: `config/manbear-common.toml` (spawn odds, speeds, damage, griefing, ...)

## How the requested features are done
| Request | Where |
|---|---|
| Spawn 1/25 per minute, only one per world | `ServerEvents.spawnRoll`, `ManbearData.claim` (a second one discards itself) |
| Stalking, circling while facing you | `ManbearBrain.tickStalk` + `ManbearMoveControl` (strafe input relative to a body that always faces you) |
| Peek behind trees, hide behind bushes | `CoverFinder` (logs/leaves/terrain that break line of sight, plus a side spot to lean out), `tickHide`, `tickPeek` |
| Random mode switching (stalk -> peek -> fake -> real chase) | `ManbearBrain.spontaneous` / `pickNextMode` – weighted, checked every second |
| Fake chase | `tickFake` (charges, stops short, stares; ~10% turn out real) |
| Real chase, lunge, speed = player then slightly faster | `tickChase`, `tickLunge`, `refSpeedMod` (matches your actual sprint speed, up to +12% over 45 s) |
| Break blocks to reach you | `BlockBreaker` (respects mobGriefing + config) |
| Climb / crawl | `WallClimberNavigation` + climb flag; stance system (stand 5.0 / crouch 3.3 / crawl 0.9) picks a smaller body when a route needs it |
| Crouch to be stealthy | stance + `hunt` / `crouch_walk` animations |
| Bear traps, ambush when you step in one | `BearTrapBlock` (hunter_set) -> `ManbearEntity.onTrapSprung` -> `AMBUSH` mode |
| Directional bear noises | positional sound events (`ModSounds`) played from his body |
| Glowing eyes | `ManbearEyesLayer` (your eyes texture, full-bright). The texture only has 2 pixels, so the glow is small. |
| Adapts to the player | `PlayerProfile` (vigilance, panic, aggression, camping, sneak, sprint, base location) saved in the world |

### What adaptation does
Looks at him a lot -> hides/peeks/fakes more and freezes when stared at. Ignores him -> comes closer, chases sooner.
Runs when chased -> more real chases and lunges. Fights back -> more traps, hit-and-run. Camps/holes up -> more chases
with block breaking, traps near your base.

### Sounds
They alias vanilla polar-bear/ravager sounds pitched down (`assets/manbear/sounds.json`). To use your own, put `.ogg`
files in `assets/manbear/sounds/` and point the `sounds.json` entries at them.

## If it does not compile first time
This was written without access to the Forge/GeckoLib jars. The spots most likely to need a tweak if your GeckoLib
version differs slightly: `withScale` and `reRender` (renderer/eyes layer), `AnimationState.isMoving`. The brain, entity and
block code only use vanilla 1.20.1 (official mappings) APIs.

## Update 2: stealth, short appearances, peeking, animals, ambience
* **Stalking is farther away** (default 26-42 blocks, `stalkMinDistance` / `stalkMaxDistance`); hiding/peeking cover is searched at that range too. He only closes in when ambushing, chasing, fake chasing (or if you walk up to him).
* **He vanishes after 90-120 s** (`lifetimeMinSeconds` / `lifetimeMaxSeconds`). If you are not looking at him he disappears immediately; if you are, he backs away and fades. If a chase is running he finishes it first (max +40 s). Bear traps he laid stay in the world (capped by `maxTraps`).
  Because his life is short, the real-chase grace period is now 20 s and he switches modes more often.
* **Peeking** now stays behind the tree and *leans out* around the trunk with the new `peek_left` / `peek_right` animations (built from your `hunt` pose; legs planted, hips/torso/head swing out, one arm grips the tree).
  If he leans into the tree instead of out of it, flip `swapPeekSides` in the config.
* **Animals** panic and run when he is near, and scatter in random directions for the first ~12 s after he appears (`animalsFlee`, `animalFleeRange`).
* **Distorted pig / cow / sheep noises** (`mimicAnimalSounds`) come from him, or sometimes from behind you.

## Update 3: scarier chases
* **Only 1 in 10 spawns are chasers** (`chaseChanceDenominator`). The other 9 never start a real chase on their own: they stalk, hide, peek, fake-charge and lay traps (fake charges become more common). A chaser can still be provoked into attacking if you hit him, and a bear trap you step in still triggers an ambush.
* **Faster chases:** he starts 15% faster than your sprint (`chaseSpeedBonusBase`) and builds to 35% faster after 30 s (`chaseSpeedBonusMax`, `chaseRampSeconds`). Fake charges run 15% faster than you so they look real.
* Delete `config/manbear-common.toml` (or edit the values) so the new defaults apply.

## Update 4
* **Spawn kinds.** Every spawn rolls independently: 1/14 chaser (`chaseChanceDenominator`), 1/9 fake-chaser (`fakeChaseChanceDenominator`), 1/6 trap-layer (`trapChanceDenominator`), 1/13 sleeper (`sleeperChanceDenominator`).
  A spawn with none of those only stalks, hides and peeks. (Getting hit still provokes him, and a trap you step in still brings him.)
* **Sleeper.** Found slumped on the ground with his eyes dark, snoring (you can hear him). Vibrations wake him like a Warden: sprinting/walking nearby, landing jumps, breaking/placing blocks, explosions, projectiles. Sneaking is silent. Wakes -> roars and chases at once. Sleepers last 4-6 min (`sleeper*` settings). `sleepNoiseThreshold` tunes how easily he wakes.
* **Sound design.** Heavy double-layered footsteps whose volume rises steeply as he gets closer, deeper breathing when near, layered deep roars. The roars/footsteps are built from pitched-down vanilla bear/ravager/warden sounds. To use real recordings, drop `.ogg` files into `assets/manbear/sounds/` and point the entries in `sounds.json` at them (e.g. `{"name":"manbear:roar1"}`).
* **Peeking** keeps the leaning pose until he starts moving again (one long lean per peek instead of in-and-out).
* **Stun roar** during real chases: only when you are 16-50 blocks away with line of sight, max 2 per chase, 2 s freeze + camera forced onto him. He keeps running (12% faster) while you are frozen. Settings: `roarsPerChase`, `roarMinDistance`, `roarMaxDistance`, `roarStunTicks`.
* Delete `config/manbear-common.toml` so the new options and defaults are created.

## Update 5
* **"Behind you" event (rarest, 1/20 spawns, `behindChanceDenominator`).** He appears 11-16 blocks directly behind you and stands motionless and silent (you may hear him breathe). He quietly follows if you walk off. When you finally turn and see him he roars (very loud, also played in your ears) and charges at 5x your sprint speed (`behindRushMultiplier`) for the first ~2.5 s, then chases normally. Test: `/manbear spawn behind`.
* **Roar limit fixed.** The 2-per-chase limit used to reset whenever he re-entered chase mode. It now resets only after 30 s without any chase, so he can never roar more than `roarsPerChase` times in one engagement.
* **Peek animation** now runs on its own animation layer (`peek`), independent of the walk/idle logic, and is re-asserted every tick while he holds the lean.
* **Sleep pose** rebuilt: lying flat on his stomach, arms and legs sprawled, head turned with cheek on the ground, slow breathing. (Built on the same forward-pitch pose as your `run2`; the lying height/offset may need a small nudge once you see it: edit `Manbear.position` in the `sleep` animation.)
* **Tunnelling to you in a house.** When he cannot path to you he now walks straight toward you and breaks the first block in the line between you and him (walls, roof, floor), looking both low and high. The old version only checked the block right in front of him, which is why he stood outside.

## Update 6
* **Thick trunks (2x2, 3x3 jungle/dark-oak):** the cover finder now measures trunk width, uses only the back face, hides behind the MIDDLE of a wide trunk and searches further out for a peek spot. Wide trunks use the new `peek_left_wide` / `peek_right_wide` animations (a much deeper lean). Normal trunks are unchanged.
* **Behind-you pounce:** when you turn and see him he now does a real leap: a short coil, then a ballistic jump that lands on you in under half a second (leading a moving target), with a new `leap` animation (coil -> full stretch, jaw open), the loud roar, and then the 5x-speed run if he missed.
* **Swimming:** in deep water he swims straight at you, faster than you swim (`swimSpeedFactor`), with a new `swim` animation and a flatter swim hitbox. Shallow water he wades through. Swimming away no longer works.
* **Animal lure (1/6 spawns, `lureChanceDenominator`):** he slips into cover, a wolf appears between you and him (whining to get your attention). Once you see it, it stares at you for a moment, then sprints to him. When it reaches him he lets out a long roar (about 4 s) and starts a real chase if that spawn is a chaser, otherwise a fake chase. Test: `/manbear spawn lurer`.
* Delete `config/manbear-common.toml` to get the new options.

## Update 7
* **Lure animals:** the lure event now picks a random wolf, pig, sheep or cow (each calls with its own sound to get your attention).
* **Lure outcome (`lureTrapChance`, default 0.7):** 70% the animal runs to a bait spot a few blocks in front of his hiding place and waits there calling you in; the spot is ringed with his bear traps on your side. Step on one and the usual trap ambush starts. If nobody comes for ~45 s he gives up quietly. 30% it runs straight to him, he lets out the long roar and a chase (or fake chase) begins. His traps only catch players, so the animal can run over them. Test: `/manbear spawn lurer`.
* **Rivers:** he now wades/swims through any real water (even 1 block deep), and if there is no walkable path to you (and nothing to break) he charges straight at you instead of giving up. Crossing a river no longer saves you. (He will not charge straight into lava.)
* **Stalking sounds (`ambientTricks`, `trickMinIntervalTicks`, `trickMaxIntervalTicks`):** while he stalks/hides/peeks he occasionally plays a chest opening and closing, a door (wood or iron) opening and closing, a barrel, a torch fizzling out, or a low grumble, from random spots 7-18 blocks around you (often behind you).
* **Stealthier stalking:** he spends much less time walking around in the open. Stalk phases are shorter, hiding/peeking are much more likely, he ducks behind the next tree quickly (after ~2 s of being stared at, or randomly), and when he finishes hiding he usually hops to a DIFFERENT tree (6+ blocks away) instead of staying put, so he moves tree-to-tree.
* Delete `config/manbear-common.toml` for the new options.

## Update 8: mostly stalking and hiding
* New gate `eventChance` (default 0.3): only 30% of spawns have any special event at all. The other **70% are pure stalkers** that only stalk, hide, peek and make ambient sounds, never chase.
* A spawn with an event gets exactly ONE of: real chase, fake chase, trap-laying, sleeping, behind-you, animal lure. The old `...ChanceDenominator` settings are now just their RELATIVE rarity (behind-you stays the rarest). Of all spawns that works out to roughly: trap-laying 8%, lure 8%, fake chase 5%, sleeper 4%, chase 3%, behind-you 2%, and 70% nothing.
* The lure ends in a real chase only 30% of the time (otherwise a fake chase, or the trap bait).
* If you hit him he now chases only 35% of the time (otherwise he mostly backs off), and a real chase can still be triggered by a bear trap you step in.
* Raise `eventChance` for more action; set it to 0 for a pure stalker mod.
* Delete `config/manbear-common.toml` for the new option.

## Update 9: path blocker, fog watcher, cave event, lunge-only traps
* **Trap ambush is now only a lunge.** Step in one of his traps and he rushes you and lunges once; the moment the lunge (or a swing) hurts you he vanishes in a puff of smoke. No chase, no roar-stun. If he can't reach you within ~25 s he vanishes anyway. (Same for the animal-lure trap bait.)
* **Path blocker** (a weighted event, `pathBlockerChanceDenominator`): he learns your most-visited spots (the profile now remembers exact locations: your door, your farm, a cave mouth...). He appears standing at one of them 30-40 blocks away (`pathBlockerMinDistance`/`MaxDistance`), not in your view, and just stands there. Come within a random 4-12 blocks and he does the fast leap and starts a real chase. If you have no hot spots yet (or none in range) the spawn is just an ordinary stalker. Test: `/manbear spawn path` (spend a few minutes in a place first).
* **Fog watcher** (1 in 10 of ALL spawns, `fogWatcherChanceDenominator`): he stands at the very edge of YOUR render distance, in the direction you're looking, where the fog is thickest, so he's just a dark shape with glowing eyes. He never chases; if you walk toward him he backs away to keep the distance, and if you rush him he slips away. Needs sky above you (not underground). Test: `/manbear spawn fog`.
* **Cave event** (`caveEventChance`, default 0.35 of spawn rolls that happen while you're in a cave): he sits in the open middle of a cave, back to you, in a spatter of redstone dust (blood). Come within `caveFleeDistance` (12) and he bolts away at ~2.4x your speed with a startled growl, and despawns once out of sight (or after 15 s). The "blood" stays behind. New `sit` animation. Test: `/manbear spawn cave` while underground.
* Delete `config/manbear-common.toml` for the new options.

* Fog watcher distance uses the server view distance (your render distance in singleplayer); `fogEdgeChunks` overrides it for servers.

## Update 10
* **Fake chase** now runs at 3x your sprint speed (`fakeChaseSpeedMultiplier`), tears through blocks and wades rivers to reach you, and after it he sprints off and despawns as soon as he is out of sight (unless the 10% "it was real" roll turns it into a chase).
* **Villagers** join wolf/pig/sheep/cow as lure animals.
* **Grab:** during a chase, when he catches you (a swing or a landed lunge) there is a `grabChance` (0.5) that he grabs you instead. He holds you in front of his chest, 1 damage per second (`grabDamagePerSecond`; armor reduces it), and the exp bar becomes the escape bar: mash SPACE to fill it (`strugglePerPress`, it drains slowly if you stop). After 12 s he throws you off; if you break free he is dazed for 1.5 s and cannot grab again for 15 s. Needs the mod on both client and server (a tiny key-press packet).
* **Lurker** event (`lurkerChanceDenominator`): he appears 5-8 blocks right behind you and keeps scrambling to stay at your back and out of sight as you turn. If you look straight at him for about 0.6 s he bolts away very fast and despawns when out of sight. Test: `/manbear spawn lurker`.
* **Home raid** (`homeRaids`, `raidChanceDenominator` = 15, `raidAwayDistance` = 150): for every minute you spend more than 150 blocks from your base there is a 1/15 chance it gets rummaged. It is carried out when you return within ~100 blocks, so you find the aftermath: door(s) left open, chests/barrels broken with all their items spilled on the floor (those items never despawn), windows smashed, a few wall blocks knocked out, and trails of giant footprints (a new breakable `giant footprint` block) from the door in and around the broken chests. **Beds and anything touching a bed are never touched.** "Base" = the place you spend the most time. Test: `/manbear raid` (your base) or `/manbear raid here` (around you).
* Delete `config/manbear-common.toml` for the new options.

## Update 11
* Real chases now run at the same speed as fake chases: 3x your sprint speed (`chaseSpeedMultiplier`, default 3.0). The old gradual speed-up settings (`chaseSpeedBonusBase/Max`, `chaseRampSeconds`) are no longer used. Lower `chaseSpeedMultiplier` if that is too much.
* `manbear.animation.json` is now your updated file (new peeking, sleep, swim, sit and grab, plus your tweaks to walk, roar and leap). It contains every animation the mod uses.

## Update 12: phantom sounds and your own voice
* **Ambient phantom sounds** (`ambientSounds`, `ambientMinSeconds`/`ambientMaxSeconds`, default every 75-240 s, a bit more often at night): nothing spawns, you just hear it. Very distant bear roars/growls (sometimes answered closer), twigs snapping close by, a tree falling (cracks, rushing leaves, heavy crash), screams, animals crying out and dying. Underground you only get the roars and twigs. The sounds are built from pitched vanilla sounds (ghast scream, animal death sounds, wood/bamboo breaks...); you can swap in real recordings via `sounds.json`.
* **Voice mimicry, built into the mod** (no Simple Voice Chat needed): the client listens to your microphone while you are in a world, detects when you speak, and keeps up to 24 short phrases IN MEMORY on your own PC (never saved, never sent anywhere, cleared when you leave the world). Every 30 s (`voiceIntervalTicks` = 600) while he stalks, hides, peeks or chases there is a 1-in-5 (`voiceChance` = 0.2) chance the server tells your client to play one of YOUR phrases from his body, using OpenAL so it really comes from his direction (deeper pitch, `voicePitch` 0.88 in the client config).
* Client config `manbear-client.toml`: `micSampling` (set false to turn the microphone off completely), `micThreshold` (raise it if background noise gets recorded), `voicePitch`, `voiceVolume`. You also get a one-time chat notice when sampling starts. If Java cannot open your microphone (permissions, no device) it just logs a line and stays off.
* It only works for players who have the mod; each player only ever hears their own voice.

## Update 13: other players' voices (multiplayer)
* New: in multiplayer he can play **another player's** voice to you, so you hear your friend calling from the woods. 70% of voice triggers use someone else's phrase when any are available (otherwise your own).
* It is **opt-in per player**: sharing is OFF by default. Each player turns it on with `/manbearvoice share on` (off again with `/manbearvoice share off`, `/manbearvoice status` shows the state; or `shareVoiceWithServer` in `manbear-client.toml`). Only players who opt in have their voice sent to the server; everyone else just hears their own voice as before.
* Server side: phrases are squeezed to 8 kHz mu-law, kept in **server RAM only** (max 12 per player), never saved to disk, dropped when that player logs out or the server stops. Server owners can refuse them completely with `voiceSharing = false`. Please tell your players before turning it on.
* Because a phrase must be sent and relayed, shared voices sound a little more telephone-like than your own.

## Update 14
* `shareVoiceWithServer` is now ON by default (for a friends-only group where everyone knows their voice is mimicked). Players can still switch it off with `/manbearvoice share off`. If you already have a `manbear-client.toml` from before, delete it (or run `/manbearvoice share on`) because an existing file keeps the old OFF value.

## Update 15: aggression, stealth, bushes, strict despawn
**Rage (aggression), per player, 0-100** (`rageEnabled`). Shown by `/manbear profile`; `/manbear rage` shows it and `/manbear rage set <0-100>` sets it for testing. It cools off by 1 per minute (`rageDecayPerMinute`) and is raised by:
* cutting logs of any tree, vanilla or modded (any block in the logs tag, or named *_log / *_stem / *_hyphae / *_wood): +0.8 each (`rageLog`)
* getting within 12 blocks of him while he stalks, hides or peeks (+2 per second, double within 6) (`rageCloseToHim`)
* killing harmless creatures that give you nothing: rabbits, wolves, llamas, villagers, wandering traders, iron golems, bats, foxes, cats, ocelots, parrots, axolotls, frogs, turtles, plus ANY non-hostile mob (modded too) whose death drops nothing (baby animals count): +6 (`rageKillHarmless`)
* hitting him: +6 (`rageHitHim`)
* breaking one of his bear traps: +8 (`rageBreakTrap`)
* killing his lure animal: +14 (`rageKillLure`)
* staying within 24 blocks of the same spot for over 5 minutes: +3 per minute (`rageStayingPut`)

What more rage does: he spawns more often (up to 1 + `rageSpawnBonus` times, ignores the respawn cooldown above 70% rage, and picks the angriest player); events become far more likely and chases much more likely (above 55% rage even a non-chaser will chase, with a shorter grace period); he switches behaviour much more often (erratic); and he gets bolder: stalks and hides closer, no longer freezes when you look at him, pounces from further away, uses voice tricks and sound tricks more.

**Stealth**
* His eyes go dark while he is hiding, peeking, ducking or sleeping.
* New `ducking` animation (a squat, built from your `sit`). Behind a solid 2-3 block tall structure or terrain step he ducks (his head would show above it standing), and the eyes go dark.
* He hides INSIDE ground-level bushes (leaf clusters): he walks in, ducks, eyes dark. Bushes are preferred hiding spots.
* He now walks and runs straight through leaves (pathing allows it and movement glides through leaf blocks while following the ground).

**Spawning and despawning**
* Every spawn, of every kind, only happens where no player could see him (inside any player's field of view with a clear line, up to 256 blocks). The fog watcher now appears just outside your view at the edge of your render distance, so you find him when you turn.
* Every encounter lasts 70-120 s (`lifetimeMinSeconds` / `lifetimeMaxSeconds`) whatever he is doing (chase, grab, sleeping...). When time is up he vanishes at once if nobody can see him, otherwise he sprints for cover and vanishes the moment he is out of sight. Nothing extends his life any more (sleepers etc. now also use 70-120 s).

**Chase animation:** during a REAL chase the run animation switches irregularly between the real-chase run and the fake-chase run (about every 1.4 s), so you can't tell by his animation whether it is real. Fake chases only ever use the fake-chase run.

## Update 16
* **Animations:** `manbear.animation.json` is now your newest file (it includes your own `ducking`). Its `sleep` is byte-identical to the one already in the mod, so the sleep problem is probably not the animation data. The lying body is ~5.4 blocks long, so sleepers now only spawn where the whole strip under him (7x3 blocks) is flat, clear ground, so he can't clip into slopes, trees or walls.
* **Eyes:** no glow while hiding, peeking, sleeping, or ducking (the ducking STANCE as well as the ducking flag).
* **Pet kidnapping** (`petKidnapChanceDenominator`; only possible when you have a tamed pet within 40 blocks, anything owned by you, modded tamables included): he appears behind you, stays out of sight at your back, and once your back has been turned for ~2 s he dashes in, snatches the pet (it rides on him), and carries it 20-50 blocks away, preferably out of your sight. There he sets it down in a CLOSED bear trap: the pet cannot move or be hurt. Find it and RIGHT-CLICK it to free it (you get a message; it also raises his rage). If he spots you looking first, he leaves. If he despawns while carrying it, it is set down in a trap where he was. Test: `/manbear spawn petnapper` (needs a pet nearby).
* **Lightning event** (`lightningEvent`, `lightningChancePerMinute` = 0.12): at night, outside, a bolt strikes and invisible light blocks light up the area around you for about a second; in that light he stands 10-15 blocks in front of you, in view. When the light goes out he is gone. The light blocks are always removed (also on server stop). This event deliberately spawns in view. Test: `/manbear spawn lightning`.
* **Stunning him in a chase:**
  * an iron, diamond or netherite axe hit (any axe of tier iron or better, modded too) stuns him for 2 s (`axeStunTicks`), even breaking a grab;
  * animals near his path distract him: he runs to the nearest animal within ~7 blocks, kills it (not tamed pets, not lure animals) and resumes (`animalDistractTicks` = 40, then a few seconds before another distraction).
* **More rage sources:** stunning/distracting him (+5), rescuing a kidnapped pet (+10), placing campfires (+3), breaking grass BLOCKS (+0.3 each, not tall grass), escaping his grab (+8), and saying "man bear" or "bear" (+5).
* **Calming him down:** planting saplings (-1.5 each, modded saplings too), breeding animals (-4), planting crops (-1), tilling farmland with a hoe (-0.5). All in the config.
* **Saying his name:** with speech recognition on, if he is out there and you say "man bear" or "bear" he has a 5% chance to start a chase, 5% a fake chase, 5% to give up and despawn (`sayName...Chance`); at most once per 10 s. Saying it always raises his rage, even if he is not spawned.
  * It uses the speech recognition built into **Windows** (System.Speech through PowerShell): nothing is downloaded and the audio stays on your PC; only "the player said it" is sent to the server. On macOS/Linux it simply does nothing. It uses the general Windows dictation recognizer, so accuracy depends on your Windows speech setup and mic; it may miss you or occasionally mishear. Turn it off with `speechRecognition=false` in `manbear-client.toml`.
* Delete `config/manbear-common.toml` (and `manbear-client.toml`) for the new options.

## Update 17
* **New model / textures / animations installed** (`manbear_geo_1.json`, `manbear.png`, `manbear_eyes.png`, `manbear_animation_d67156.json`): the model adds two eyebrow bones and keeps the same size, so he is still 5 blocks tall. The new eyes texture also lights up some extra pixels (the eyebrows); they follow the same rule as the eyes (dark while hiding, peeking, ducking, sleeping). The animation file has every animation the mod uses.
* **Intimidation event** (`intimidationChanceDenominator` = 13, a 1-in-13 roll of all spawns): he appears out of sight and walks straight at you at a player's WALKING speed (sprinting outruns him), and does not care whether you look at him. No footsteps at all, only soft breathing that gets a little louder as he nears. He breaks blocks or walks through leaves to reach you. If he gets within ~3 blocks he grabs you and you cannot escape: the exp bar stays empty, space-bashing and axes do nothing, and after ~3.5 s you die (totems can't save you; `intimidationLethal=false` leaves you on 1 heart instead). Escape by outrunning him until his 70-120 s are up. Test: `/manbear spawn intimidation`.
* **Head bug:** his head is now aimed directly at you by the mod itself (vanilla's look control aims ~20 degrees off and pulls the head back towards the body, which is what left it turned away and tilted). When he has nobody to look at, the head straightens out and levels off. No head tilt while ducking, sitting or swimming.
* **Leaves are air:** any part of his body touching leaves (the whole 5-block height now, not just the legs, so low canopies no longer trap him) switches him to a leaf-walking mode that ignores leaves, follows the ground below, and finds ground up to 8 blocks down.
* **Pathfinding:** much less getting stuck.
  * He plans routes for a slim 1-wide, 3-high body instead of his real size (the old size made most gaps between trees, doors and rooms count as "too small", so he found no path). His hitbox is now 0.95 wide (was 1.3).
  * His real body adapts along the way: he picks the biggest stance (stand, crouch, duck, crawl) that fits where he is AND one step ahead, re-checked every 3 ticks.
  * He opens wooden doors on his way.
  * When he is wedged he cycles through jumping, sidestepping left, sidestepping right, recomputing the path, and backing off to run at it again, before giving up.
* Delete `config/manbear-common.toml` for the new options.
