package com.enderdevelopment.plugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class Main extends JavaPlugin implements Listener {
    private static final List<Tier> TIERS = List.of(
        new Tier("Wooden", Material.OAK_PLANKS, NamedTextColor.GOLD),
        new Tier("Coal", Material.COAL_BLOCK, NamedTextColor.DARK_GRAY),
        new Tier("Copper", Material.COPPER_BLOCK, NamedTextColor.GOLD),
        new Tier("Iron", Material.IRON_BLOCK, NamedTextColor.WHITE),
        new Tier("Gold", Material.GOLD_BLOCK, NamedTextColor.YELLOW),
        new Tier("Redstone", Material.REDSTONE_BLOCK, NamedTextColor.RED),
        new Tier("Lapis", Material.LAPIS_BLOCK, NamedTextColor.BLUE),
        new Tier("Emerald", Material.EMERALD_BLOCK, NamedTextColor.GREEN),
        new Tier("Diamond", Material.DIAMOND_BLOCK, NamedTextColor.AQUA),
        new Tier("Quartz", Material.QUARTZ_BLOCK, NamedTextColor.WHITE),
        new Tier("Ancient Debris", Material.ANCIENT_DEBRIS, NamedTextColor.DARK_PURPLE),
        new Tier("Netherite", Material.NETHERITE_BLOCK, NamedTextColor.DARK_GRAY)
    );

    private NamespacedKey tierKey;

    @Override
    public void onEnable() {
        tierKey = new NamespacedKey(this, "mace_tier");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("OreMace progression enabled with " + TIERS.size() + " tiers.");
    }

    @Override
    public void onDisable() {
        getLogger().info("OreMace progression disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (findMace(player) == null) {
            giveMace(player, 0);
            player.sendMessage(Component.text("You received a Wooden Mace. Kill players to upgrade it.", NamedTextColor.AQUA));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        event.getDrops().removeIf(this::isProgressionMace);

        Player killer = victim.getKiller();
        if (killer != null) {
            upgradeMace(killer);
        }

        Bukkit.getScheduler().runTask(this, () -> {
            removeMaces(victim);
            giveMace(victim, 0);
            victim.sendMessage(Component.text("Your mace progress reset to Wooden.", NamedTextColor.RED));
        });
    }

    @EventHandler
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack mace = findMace(player);
        if (mace == null || !isHeldMace(player, mace)) {
            return;
        }
        int tier = getTier(mace);
        if (tier >= 5 && ThreadLocalRandom.current().nextDouble() < Math.min(0.06 + tier * 0.015, 0.22)) {
            event.setDamage(event.getDamage() + 1.0 + tier * 0.35);
        }
    }

    private void upgradeMace(Player player) {
        ItemStack mace = findMace(player);
        if (mace == null) {
            return;
        }
        int oldTier = getTier(mace);
        int newTier = Math.min(oldTier + 1, TIERS.size() - 1);
        replaceMace(player, mace, createMace(newTier));
        applyTierAbility(player, newTier);
        if (newTier == oldTier) {
            player.sendMessage(Component.text("Your Netherite Mace is fully upgraded.", NamedTextColor.LIGHT_PURPLE));
            return;
        }
        Tier tier = TIERS.get(newTier);
        player.sendMessage(Component.text("Mace upgraded: ", NamedTextColor.GRAY)
            .append(Component.text(tier.name() + " Mace", tier.color())));
    }

    private void applyTierAbility(Player player, int tier) {
        if (tier >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, tier >= 7 ? 1 : 0, true, true));
        }
        if (tier >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, tier >= 9 ? 1 : 0, true, true));
        }
        if (tier >= 6) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, tier >= 10 ? 1 : 0, true, true));
        }
        if (tier >= 8) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, true));
        }
    }

    private ItemStack createMace(int tier) {
        int safeTier = Math.max(0, Math.min(tier, TIERS.size() - 1));
        Tier data = TIERS.get(safeTier);
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text(data.name() + " Mace", data.color()));
        meta.lore(List.of(
            Component.text("Player kills: " + safeTier + " / " + (TIERS.size() - 1), NamedTextColor.GRAY),
            Component.text("Death resets this mace to Wooden.", NamedTextColor.DARK_GRAY),
            Component.text("Ore tier: " + data.name(), NamedTextColor.DARK_AQUA)
        ));
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, safeTier);
        addEnchant(meta, "density", Math.min(1 + safeTier / 2, 5));
        if (safeTier >= 2) {
            addEnchant(meta, "breach", Math.min(1 + safeTier / 3, 4));
        }
        if (safeTier >= 4) {
            addEnchant(meta, "wind_burst", Math.min(1 + (safeTier - 4) / 2, 3));
        }
        if (safeTier >= 7) {
            addEnchant(meta, "mending", 1);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addEnchant(ItemMeta meta, String key, int level) {
        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
    }

    private void giveMace(Player player, int tier) {
        player.getInventory().addItem(createMace(tier));
    }

    private void replaceMace(Player player, ItemStack oldMace, ItemStack newMace) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (player.getInventory().getItem(slot) == oldMace) {
                player.getInventory().setItem(slot, newMace);
                return;
            }
        }
        giveMace(player, getTier(newMace));
    }

    private int getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return tier == null ? 0 : Math.max(0, Math.min(tier, TIERS.size() - 1));
    }

    private boolean isProgressionMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER);
    }

    private boolean isHeldMace(Player player, ItemStack mace) {
        return player.getInventory().getItemInMainHand() == mace || player.getInventory().getItemInOffHand() == mace;
    }

    private ItemStack findMace(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isProgressionMace(item)) {
                return item;
            }
        }
        return null;
    }

    private void removeMaces(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (isProgressionMace(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private record Tier(String name, Material ore, NamedTextColor color) {
    }
}
