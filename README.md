# RaindanceMC/invsnap

- **`invsnap-module`** — Rain's Flexible Modules — Inventory Snapshots: Takes a snapshot of every online player's inventory on a timer, keeps the newest few per player, and lets an admin browse and restore one back into a live inventory.
- **`invsnap-standalone`** — Rain's Inventory Snapshots: Rain's Inventory Snapshots, as a plugin of its own. Nothing here but a build file and a descriptor: the invsnap module and the standard wrapper are shaded together, and that is the whole plugin.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `invsnap-standalone/target/RainsInventorySnapshots-1.9.2.jar` — also published as `de.raindancer.modules:invsnap-standalone:1.9.2`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
