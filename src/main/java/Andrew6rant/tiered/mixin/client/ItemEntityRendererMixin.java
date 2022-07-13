package Andrew6rant.tiered.mixin.client;

import com.google.common.collect.Multimap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity> {

    protected ItemEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void preRender(ItemEntity itemEntity, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        matrices.push();
        AtomicReference<Float> scale = new AtomicReference<>(1f);
        Identifier size = new Identifier("tiered","generic.size");

        for(EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers = itemEntity.getStack().getAttributeModifiers(slot);
            if(!attributeModifiers.isEmpty()) {
                attributeModifiers.keySet().forEach(attribute -> attributeModifiers.get(attribute).forEach(modifier -> {
                    float value = (float) modifier.getValue();
                    Identifier attributeId = Registry.ATTRIBUTE.getId(attribute);
                    if (attributeId.equals(size)) {
                        if (modifier.getOperation() == EntityAttributeModifier.Operation.ADDITION) {
                            scale.updateAndGet(v -> v + value);
                        } else {
                            scale.updateAndGet(v -> v * (value * 0.65f + 1.05f));
                        }
                    }
                }));
            }
        }
        matrices.scale(scale.get(), scale.get(), scale.get());
        matrices.push();
    }
    @Inject(at = @At("RETURN"), method = "render")
    public void postRender(ItemEntity itemEntity, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        matrices.pop();
        matrices.pop();
    }

}