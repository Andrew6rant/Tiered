package Andrew6rant.tiered;

import Andrew6rant.tiered.api.CustomEntityAttributes;
import Andrew6rant.tiered.data.AttributeDataLoader;
import Andrew6rant.tiered.api.PotentialAttribute;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.*;

import static Andrew6rant.tiered.Tiered.NBT_SUBTAG_KEY;

public class TieredClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<Identifier, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    @Override
    public void onInitializeClient() {
        registerAttributeSyncHandler();
        RenderTooltipEvents.POST.register(TieredClient::onPostTooltipEvent);
    }

    public static void registerAttributeSyncHandler() {
        ClientPlayNetworking.registerGlobalReceiver(Tiered.ATTRIBUTE_SYNC_PACKET, (client, play, packet, packetSender) -> {
            // save old attributes
            CACHED_ATTRIBUTES.putAll(Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes());
            Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().clear();

            // for each id/attribute pair, load it
            int size = packet.readInt();
            for(int i = 0; i < size; i++) {
                Identifier id = new Identifier(packet.readString());
                PotentialAttribute pa = AttributeDataLoader.GSON.fromJson(packet.readString(), PotentialAttribute.class);
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().put(id, pa);
            }
        });
    }
    public static float getSize(LivingEntity entity, float f) {
        EntityAttributeInstance instance = entity.getAttributeInstance(CustomEntityAttributes.SIZE);

        if(instance != null) {
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.getValue();

                if (modifier.getOperation() == EntityAttributeModifier.Operation.ADDITION) {
                    f += amount;
                } else {
                    f *= (amount + 1);
                }
            }
        }

        return f;
    }
    public static String trailZeros(float f) {
        int i = (int) f;
        return f == i ? String.valueOf(i) : String.valueOf(f);
    }
    public static float roundFloat(float f) {
        return ((int) ((f + (f >= 0 ? 1 : -1) * 0.005f) * 100)) / 100f;
    }

    public static void onPostTooltipEvent(ItemStack stack, List<TooltipComponent> components, MatrixStack matrixStack, int x, int y, TextRenderer font, int width, int height, boolean comparison) {
        if(stack.getSubNbt(NBT_SUBTAG_KEY) != null) {
            Identifier tier = new Identifier(stack.getOrCreateSubNbt(NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            Tooltip.drawBorder(matrixStack, x, y, width, height, potentialAttribute.getTooltip_image(), potentialAttribute.getTooltip_border(0), potentialAttribute.getTooltip_border(1));
        }
    }
}
