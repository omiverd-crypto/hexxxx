package net.hexpowers;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
    private ModItems() {}

    public static final Item DARKHOLD = Registry.register(Registries.ITEM, HexPowers.id("darkhold"),
            new DarkholdItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(DARKHOLD));
    }
}
