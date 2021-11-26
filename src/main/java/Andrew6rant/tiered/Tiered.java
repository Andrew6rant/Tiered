package Andrew6rant.tiered;

import Andrew6rant.tiered.api.CustomEntityAttributes;
import Andrew6rant.tiered.api.PotentialAttribute;
import Andrew6rant.tiered.api.TieredItemTags;
import Andrew6rant.tiered.block.ReforgingStation;
import Andrew6rant.tiered.block.ReforgingStationBlockEntity;
import Andrew6rant.tiered.data.AttributeDataLoader;
import Andrew6rant.tiered.mixin.ServerResourceManagerMixin;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.client.ItemTooltipCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class Tiered implements ModInitializer {

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(
            new Identifier("tiered", "general"),
            () -> new ItemStack(Tiered.REFORGING_STATION));

    /**
     * Attribute Data Loader instance which handles loading attribute .json files from "data/modid/item_attributes".
     * <p> This field is registered to the server's data manager in {@link ServerResourceManagerMixin}
     */
    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final UUID[] MODIFIERS = new UUID[] {
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"),
            UUID.fromString("4a88bc27-9563-4eeb-96d5-fe50917cc24f"),
            UUID.fromString("fee48d8c-1b51-4c46-9f4b-c58162623a7a")
            //UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf")
    };

    public static final Logger LOGGER = LogManager.getLogger();

    public static final Identifier ATTRIBUTE_SYNC_PACKET = new Identifier("attribute_sync");
    public static final String NBT_SUBTAG_KEY = "Tiered";
    public static final String NBT_SUBTAG_DATA_KEY = "Tier";

    public static final ReforgingStation REFORGING_STATION = new ReforgingStation(FabricBlockSettings.copyOf(Blocks.SPRUCE_PLANKS));
    public static BlockEntityType<ReforgingStationBlockEntity> REFORGING_STATION_BLOCK_ENTITY;

    @Override
    public void onInitialize() {
        TieredItemTags.init();
        CustomEntityAttributes.init();
        registerAttributeSyncer();

        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
//            setupModifierLabel();
        }
        //Registry.register(Registry.BLOCK, new Identifier("tiered", "reforging_station"), REFORGING_STATION);
        Registry.register(Registry.BLOCK, new Identifier("tiered", "reforging_station"), REFORGING_STATION);
        REFORGING_STATION_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "tiered:reforging_station_block_entity", FabricBlockEntityTypeBuilder.create(ReforgingStationBlockEntity::new, REFORGING_STATION).build(null));
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
                    lines.add(1, new TranslatableText(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle().get(0)));
                }
            }
        });
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if(stack.getItem() instanceof ArmorItem item) {
            return item.getSlotType().equals(slot);
            //ArmorItem item = (ArmorItem) stack.getItem();
            //return item.getSlotType().equals(slot);
        }

        return slot == EquipmentSlot.MAINHAND;
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
}
