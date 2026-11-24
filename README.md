# Blue Client (multi-version source)

One source tree for every supported Minecraft version.

```
groups/yarn/src     shared code for 1.21.x   (Yarn names, remapping Loom)
groups/mojmap/src   shared code for 26.x     (unobfuscated / Mojang names)
versions/<mc>/src   files that differ for that version (they replace the group copy)
versions/<mc>/exclude.txt   group files that do NOT exist for that version
versions/<mc>/version.properties   Minecraft / Fabric API / loader versions
```

Build: `.\build.ps1` (all) or `.\build.ps1 -Version 26.3`; jars go to `dist\`.
Or directly: `gradlew -Pmc=26.3 build`. Each version builds into `build\<mc>\`.

Add a version: create `versions\<mc>\version.properties` (copy a neighbor), put any differing files under `versions\<mc>\src\`.
Fixing a bug for every version: edit the file in `groups\<group>\src` (and any `versions\*\src` copy of the same file).
