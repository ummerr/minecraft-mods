# SportsCraft

Wii-Sports-style minigames for Minecraft. **Golf first**: charge-and-release
swings, auto-generated holes, and a real scorecard. Tennis is phase 2 and the
seams for it are already in place.

Fabric 1.21.4 · Java 21 · Yarn mappings · **no mixins, no custom packets**.

## Playing

```
/sports build golf <x y z> [seed] [force]   build a hole (op 2)
/sports golf start                          start a round on the nearest hole
/sports golf status                         strokes, penalties, distance to the pin
/sports golf quit                           abandon the round
/sports golf drop                           drop a fresh ball (if yours is stuck)
/sports info                                clubs and how many holes are built
/sports debug ball [driver|iron|putter]     launch a ball (op 2, physics probe)
```

Grab a **Driver**, **Iron** or **Putter** from the Tools tab. Hold right-click
to charge — the actionbar shows a power meter — and release to swing.

The important rule: **you set the aim, the club sets the trajectory.** Yaw comes
from where you are looking; elevation comes from the club's loft, and look pitch
is ignored entirely. That is the Wii Golf model, and it is what makes choosing a
club a real decision instead of something you can aim your way around.

With no ball in reach, a swing spawns one and hits it — that is driving-range
mode. In a round, a swing plays your own at-rest ball within 3 blocks.

## How it plays

Measured at full power on fairway, launched from the ground:

| Club | Loft | Carry | Total | Apex |
|---|---|---|---|---|
| Driver | 20° | ~34 | ~53 | ~3.2 |
| Iron | 35° | ~23 | ~34 | ~4.2 |
| Putter | 0° | — | ~8.5 (on the green) | 0 |

Holes are 40–80 blocks, so a par 3 is drivable and crossing a green takes two
putts. Surfaces matter: run-out goes SAND < ROUGH < FAIRWAY < GREEN.

## Layout

The repo convention is a **pure testable core plus thin Minecraft wrappers**.
`core/` and `course/plan/` have **zero Minecraft imports** and hold everything
worth testing.

```
com/sportscraft/
  SportsCraft  SportsCraftClient  SportsHooks
  core/physics/   Vec3c BallSpec BallState SurfaceType BallPhysics BallSimulator   (pure)
  core/swing/     ClubSpec Clubs SwingPower LaunchSolver                           (pure)
  core/golf/      GolfHoleDef GolfRound StrokeOutcome Scorecard                     (pure)
  course/plan/    SurfaceCell HoleBlueprint HolePlanner HoleValidator ParCalculator (pure)
  course/         CoursePlacer SurfaceClassifier
  entity/         SportsBallEntity ModEntities   entity/client/ SportsBallRenderer
  item/           ClubItem ModItems
  game/           GolfManager HoleRegistry SportsPersistentState SportsNotifier
  command/        SportsCommands
  integration/    SportsSmokeTest
```

Two design decisions worth knowing before changing anything:

**The ball extends `Entity` directly and implements `FlyingItemEntity`** rather
than building on `ThrownItemEntity`/`ProjectileEntity`. Those despawn on their
first hit and run client-side prediction, both of which fight bounce-and-roll
physics. The ball is server-authoritative; the client renders and simulates
nothing.

**The ball knows nothing about golf.** It fires `SportsHooks` events —
`BALL_AT_REST`, `BALL_IN_WATER`, `BALL_OUT_OF_BOUNDS` — and `GolfManager`
subscribes. This is the fix for the mistake LabsCraft v1 made when block
entities called `QuestManager` directly, and it is what lets tennis reuse the
same ball later.

### The tick order that matters

`SportsBallEntity` captures velocity **before** `Entity.move()`. `move()` zeroes
any component that collided, so reading velocity afterwards would always bounce
off zero and the ball would die on first contact. If you touch the tick, keep
that ordering.

## Building

```sh
cd sports-craft
./gradlew build      # compile + tests
./gradlew runClient  # play it
```

`gradle.properties` pins `org.gradle.java.home` to a local Homebrew JDK, per repo
convention. CI strips that line with `sed` before building; do the same if you
build somewhere that path does not exist.

## Testing

`./gradlew test` runs the pure layer:

- `BallPhysicsTest` — flight, bounce energy loss, surface ordering, rest
  hysteresis, and **per-club carry ranges**. Those ranges are the tuning lock:
  change gravity, drag or a restitution constant enough to matter and a test
  fails instead of golf quietly becoming unfun.
- `SwingPowerTest` / `LaunchSolverTest` — the charge curve and the aim/loft split.
- `HolePlannerTest` — determinism plus a **50-seed sweep** where every hole must
  validate clean.
- `HoleValidatorTest` — a sabotage suite; break one invariant, assert it is caught.
- `GolfRoundTest` — happy path, penalties, ace naming, illegal-phase rejection.

`SportsSmokeTest` is a headless runtime check, gated on `SPORTSCRAFT_SMOKETEST=true`
and inert otherwise, per labs-craft's `RuntimeSmokeTest`.

## Tennis (phase 2, not built)

The seams exist: `BallSpec.TENNIS` is a second constant into the same
`BallPhysics`; a sport-id byte slot is reserved in the entity's data tracker for
picking a render stack; the net is just blocks, and horizontal bounce already
handles it. An NPC opponent would mirror `JoshWoodwardEntity` with a pure
trajectory predictor iterating `BallSimulator`.
