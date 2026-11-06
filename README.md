# Minecraft Mace Progression Plugin

Mace Ascension is a Minecraft PvP progression plugin for Paper servers. It gives players an ore-tiered mace that upgrades when they kill another player, adding enchantments and temporary abilities as it advances. Dying resets the player’s mace progression.

## Install

1. Build the project with Java 21 and Maven: `mvn package`.
2. Copy the generated `target/plugin.jar` into your Paper server’s `plugins` folder.
3. Start or restart the server.

## Use

1. Join the server. If you do not already have an OreMace progression mace in your inventory, you receive a Wooden Mace.
2. Kill another player to advance the mace by one tier. The mace is replaced in your inventory, and tier-based effects may be applied.
3. Die to lose your progression mace and return to the Wooden tier.
4. Continue earning player kills to advance through the tiers. Progression stops at Netherite.

The plugin has no player commands or configuration options.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/be8b9618-11ae-413f-a950-fb314ed5d92c?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=be8b9618-11ae-413f-a950-fb314ed5d92c
