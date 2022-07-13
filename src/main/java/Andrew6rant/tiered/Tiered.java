package Andrew6rant.tiered;

import Andrew6rant.tiered.api.CustomEntityAttributes;
import Andrew6rant.tiered.api.PotentialAttribute;
import Andrew6rant.tiered.api.TieredItemTags;
import Andrew6rant.tiered.block.ReforgingStation;
import Andrew6rant.tiered.data.AttributeDataLoader;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Tiered implements ModInitializer {

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(
            new Identifier("tiered", "general"),
            () -> new ItemStack(Tiered.REFORGING_STATION));

    /**
     * Attribute Data Loader instance which handles loading attribute .json files from "data/modid/item_attributes".
     * <p> This field is registered to the fapi's resource manager in onInitialize().
     */
    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final UUID[] MODIFIERS = new UUID[] {
            UUID.fromString("145DB27C-C624-495F-8C9F-6020A9A58B6B"),
            UUID.fromString("28499B04-0E66-4726-AB29-64469D734E0D"),
            UUID.fromString("3F3D476D-C118-4544-8365-64846904B48E"),
            UUID.fromString("4AD3F246-FEE1-4E67-B886-69FD380BB150"),
            UUID.fromString("5a88bc27-9563-4eeb-96d5-fe50917cc24f"),
            UUID.fromString("6ee48d8c-1b51-4c46-9f4b-c58162623a7a")
            //UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf")
    };

    public static final Logger LOGGER = LogManager.getLogger();

    public static final Identifier ATTRIBUTE_SYNC_PACKET = new Identifier("attribute_sync");
    public static final String NBT_SUBTAG_KEY = "Tiered";
    public static final String NBT_SUBTAG_DATA_KEY = "Tier";

    public static final ReforgingStation REFORGING_STATION = new ReforgingStation(FabricBlockSettings.copyOf(Blocks.SPRUCE_PLANKS));

    @Override
    public void onInitialize() {
        TieredItemTags.init();
        CustomEntityAttributes.init();
        registerAttributeSyncer();
        registerAttributeModifier();

        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
//            setupModifierLabel();
        }

        // Register the data pack contents listener
        var manager = ResourceManagerHelper.get(ResourceType.SERVER_DATA);
        manager.registerReloadListener(ATTRIBUTE_DATA_LOADER);

        //Registry.register(Registry.BLOCK, new Identifier("tiered", "reforging_station"), REFORGING_STATION);
        Registry.register(Registry.BLOCK, new Identifier("tiered", "reforging_station"), REFORGING_STATION);
        //Registry.register(Registry.ITEM, new Identifier("tiered", "reforging_station"), new BlockItem(REFORGING_STATION, new FabricItemSettings().group(Tiered.ITEM_GROUP)));
        Registry.register(Registry.ITEM, new Identifier("tiered", "reforging_station"), new BlockItem(REFORGING_STATION, new FabricItemSettings().group(Tiered.ITEM_GROUP)));
    }

    /**
     * Returns an {@link Identifier} namespaced with this mod's modid ("tiered").
     *
     * @param path  path of identifier (eg. apple in "minecraft:apple")
     * @return  Identifier created with a namespace of this mod's modid ("tiered") and provided path
     */
    public static Identifier id(String path) {
        return new Identifier("tiered", path);
    }

    /**
     * Creates an {@link ItemTooltipCallback} listener that adds the modifier name at the top of an Item tooltip.
     * <p>A tool name is only displayed if the item has a modifier.
     */
    public static void setupModifierLabel() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, lines) -> {
            // has tier
            if(stack.getSubNbt(NBT_SUBTAG_KEY) != null) {
                // get tier
                Identifier tier = new Identifier(stack.getOrCreateSubNbt(NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));

                // attempt to display attribute if it is valid
                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

                if(potentialAttribute != null) {
                    lines.add(1, Text.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle()));
                }
            }
        });
    }

    public static int reforgeCostGetter(ItemStack stack) {
        Identifier tier = new Identifier(stack.getOrCreateSubNbt(NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));
        PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
        if(potentialAttribute != null) {
            return potentialAttribute.getReforge_cost();
        }
        return 0; // this should never be called
    }
    public static int[] levelGetter(ItemStack stack, String key) {
        // get tier
        Identifier tier = new Identifier(stack.getOrCreateSubNbt(NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));
        // attempt to display attribute if it is valid
        PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
        return potentialAttribute.getTooltip_image();
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if(stack.getItem() instanceof ArmorItem item) {
            return item.getSlotType().equals(slot);
            //ArmorItem item = (ArmorItem) stack.getItem();
            //return item.getSlotType().equals(slot);
        }

        //return slot == EquipmentSlot.MAINHAND;
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    public static void registerAttributeSyncer() {
        ServerPlayConnectionEvents.JOIN.register((network, packetSender, minecraftServer) -> {
            PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());

            // serialize each attribute file as a string to the packet
            packet.writeInt(ATTRIBUTE_DATA_LOADER.getItemAttributes().size());

            // write each value
            ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                packet.writeString(id.toString());
                packet.writeString(AttributeDataLoader.GSON.toJson(attribute));
            });

            // send packet with attributes to client
            packetSender.sendPacket(ATTRIBUTE_SYNC_PACKET, packet);
        });
    }

    public static void registerAttributeModifier() {
        // Registering an event instead of using a mixin.
        // because why try to work around fapi's modifications when you can just use them?
        ModifyItemAttributeModifiersCallback.EVENT.register((itemStack, slot, modifiers) -> {
            if(itemStack.getSubNbt(Tiered.NBT_SUBTAG_KEY) != null) {
                Identifier tier = new Identifier(itemStack.getOrCreateSubNbt(Tiered.NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));

                if(!itemStack.hasNbt() || !itemStack.getNbt().contains("AttributeModifiers", 9)) {
                    PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

                    if(potentialAttribute != null) {
                        potentialAttribute.getAttributes().forEach(template -> {
                            // get required equipment slots
                            if(template.getRequiredEquipmentSlots() != null) {
                                List<EquipmentSlot> requiredEquipmentSlots = new ArrayList<>(Arrays.asList(template.getRequiredEquipmentSlots()));

                                if(requiredEquipmentSlots.contains(slot)) {
                                    template.realize(modifiers, slot);
                                }
                            }

                            // get optional equipment slots
                            if(template.getOptionalEquipmentSlots() != null) {
                                List<EquipmentSlot> optionalEquipmentSlots = new ArrayList<>(Arrays.asList(template.getOptionalEquipmentSlots()));

                                // optional equipment slots are valid ONLY IF the equipment slot is valid for the thing
                                if(optionalEquipmentSlots.contains(slot) && Tiered.isPreferredEquipmentSlot(itemStack, slot)) {
                                    template.realize(modifiers, slot);
                                }
                            }
                        });
                    }
                }
            }
        });
    }
}
