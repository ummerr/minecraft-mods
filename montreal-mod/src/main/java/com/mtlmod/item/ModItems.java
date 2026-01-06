package com.mtlmod.item;

import com.mtlmod.MtlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * All the custom items in our Montreal mod.
 */
public class ModItems {
    
    // Register items
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, MtlMod.MOD_ID);
    
    // Register creative tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MtlMod.MOD_ID);

    // ==================== FOOD ITEMS ====================
    
    public static final RegistryObject<Item> POUTINE = ITEMS.register("poutine",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(10)
                            .saturationMod(0.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1.0f)
                            .build())
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BAGEL = ITEMS.register("bagel",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(5)
                            .saturationMod(0.6f)
                            .fast()
                            .build())));

    public static final RegistryObject<Item> SMOKED_MEAT = ITEMS.register("smoked_meat",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(0.7f)
                            .meat()
                            .build())));

    public static final RegistryObject<Item> CAFE = ITEMS.register("cafe",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(1)
                            .saturationMod(0.1f)
                            .alwaysEat()
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 0), 1.0f)
                            .build())
                    .stacksTo(16)));

    public static final RegistryObject<Item> STEAMIE = ITEMS.register("steamie",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(6)
                            .saturationMod(0.5f)
                            .fast()
                            .build())));

    // ==================== COLLECTIBLES & QUEST ITEMS ====================

    public static final RegistryObject<Item> ORANGE_CONE = ITEMS.register("orange_cone",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> METRO_TICKET = ITEMS.register("metro_ticket",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> OPUS_CARD = ITEMS.register("opus_card",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> TAM_TAM_DRUM = ITEMS.register("tam_tam_drum",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> HABS_JERSEY = ITEMS.register("habs_jersey",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TEXTBOOK = ITEMS.register("textbook",
            () -> new TextbookItem(new Item.Properties()
                    .stacksTo(1)));

    public static final RegistryObject<Item> GERTS_TOKEN = ITEMS.register("gerts_token",
            () -> new Item(new Item.Properties()
                    .stacksTo(16)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> ARCADE_FIRE_VINYL = ITEMS.register("arcade_fire_vinyl",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> QUEST_JOURNAL = ITEMS.register("quest_journal",
            () -> new QuestJournalItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    // ==================== CREATIVE TAB ====================
    
    public static final RegistryObject<CreativeModeTab> MTL_TAB = CREATIVE_MODE_TABS.register("mtl_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(POUTINE.get()))
                    .title(Component.translatable("itemGroup.mtlmod"))
                    .displayItems((parameters, output) -> {
                        // Add all our items to the creative tab
                        output.accept(POUTINE.get());
                        output.accept(BAGEL.get());
                        output.accept(SMOKED_MEAT.get());
                        output.accept(CAFE.get());
                        output.accept(STEAMIE.get());
                        output.accept(ORANGE_CONE.get());
                        output.accept(METRO_TICKET.get());
                        output.accept(OPUS_CARD.get());
                        output.accept(TAM_TAM_DRUM.get());
                        output.accept(HABS_JERSEY.get());
                        output.accept(TEXTBOOK.get());
                        output.accept(GERTS_TOKEN.get());
                        output.accept(ARCADE_FIRE_VINYL.get());
                        output.accept(QUEST_JOURNAL.get());
                    })
                    .build());

    /**
     * Register all items and tabs with Forge
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}