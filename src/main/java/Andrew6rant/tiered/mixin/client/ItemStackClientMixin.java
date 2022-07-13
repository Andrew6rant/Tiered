package Andrew6rant.tiered.mixin.client;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import Andrew6rant.tiered.Tiered;
import Andrew6rant.tiered.api.PotentialAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

import static Andrew6rant.tiered.TieredClient.roundFloat;
import static Andrew6rant.tiered.TieredClient.trailZeros;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Shadow
    public abstract NbtCompound getOrCreateSubNbt(String key);

    @Shadow
    public abstract boolean hasNbt();

    @Shadow
    public abstract NbtCompound getSubNbt(String key);

    private boolean isTiered = false;

    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/EntityAttributeModifier;getValue()D"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void storeAttributeModifier(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List> cir, List list, MutableText mutableText, int i, EquipmentSlot var6[], int var7,
                                        int var8, EquipmentSlot equipmentSlot, Multimap multimap, Iterator var11, Map.Entry entry, EntityAttributeModifier entityAttributeModifier) {
        isTiered = entityAttributeModifier.getName().contains("tiered:");
    }

    @Redirect(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/MutableText;formatted(Lnet/minecraft/util/Formatting;)Lnet/minecraft/text/MutableText;", ordinal = 2))
    private MutableText getFormatting(MutableText instance, Formatting formatting) {
        if (this.hasNbt() && this.getSubNbt(Tiered.NBT_SUBTAG_KEY) != null && isTiered) {
            Identifier tier = new Identifier(this.getOrCreateSubNbt(Tiered.NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            return instance.setStyle(attribute.getStyle());
        } else {
            return instance.formatted(formatting);
        }
    }

    @ModifyVariable(method = "getTooltip", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;isEmpty()Z"), remap = false, index = 10)
    private Multimap<EntityAttribute, EntityAttributeModifier> sort(Multimap<EntityAttribute, EntityAttributeModifier> map) {

        Multimap<EntityAttribute, EntityAttributeModifier> vanillaFirst = LinkedListMultimap.create();
        Multimap<EntityAttribute, EntityAttributeModifier> remaining = LinkedListMultimap.create();

        map.forEach((entityAttribute, entityAttributeModifier) -> {
            if (!entityAttributeModifier.getName().contains("tiered")) {
                vanillaFirst.put(entityAttribute, entityAttributeModifier);
            }
            else {
                remaining.put(entityAttribute, entityAttributeModifier);
            }
        });
        vanillaFirst.putAll(remaining);
        return vanillaFirst;
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Text> info) {
        if (this.hasNbt() && this.getSubNbt("display") == null && this.getSubNbt(Tiered.NBT_SUBTAG_KEY) != null) {
            Identifier tier = new Identifier(getOrCreateSubNbt(Tiered.NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));

            // attempt to display attribute if it is valid
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                if(!potentialAttribute.getAttributes().get(0).getAttributeTypeID().equals("none")) {
                    info.setReturnValue(Text.translatable(potentialAttribute.getID() + ".label").append(" ").append(info.getReturnValue()).setStyle(potentialAttribute.getStyle()));
                }
            }

        }
    }
    @Inject(
            method = "getTooltip",
            at = @At(value = "RETURN", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void test(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if (isTiered && this.hasNbt() && this.getSubNbt(Tiered.NBT_SUBTAG_KEY) != null) { // only run on tiered items
            List<Text> list = cir.getReturnValue();
            List<Text> badlyFormattedList = new ArrayList<>();
            List<MutableText> modifierList = new ArrayList<>();
            Set<String> set = new HashSet<>();
            Set<MutableText> noDuplicates = new HashSet<>();
            list.removeIf(text -> (!(text.getContent() instanceof TranslatableTextContent) && text.getSiblings().size() == 0)); // remove blank tooltip lines
            for (Text textComponent : list) {
                if (!(textComponent.getContent() instanceof TranslatableTextContent)) {
                    badlyFormattedList.add(textComponent);
                }
            }
            badlyFormattedList.remove(0); // preserve the name of the item
            for (Text badtext : badlyFormattedList) {
                // reformat badly formatted tooltip lines into TranslatableTexts, as the first two lines
                // of most held weapons are blank TextComponents with sibling TranslatableComponents
                //System.out.println("BADLY_FORMATTED: "+badtext);
                //System.out.println("siblings: "+badtext.getSiblings().get(0));
                TextContent textContent = badtext.getSiblings().get(0).getContent();
                //System.out.println("content: "+textContent);
                list.add(2, MutableText.of(textContent));
                //list.add(MutableText(badtext.getSiblings().get(0).getKey()));
                //MutableText mutableText = (MutableText) badtext.getSiblings().get(0);
                //MutableText newText = mutableText
                //TranslatableTextContent translatableText = (TranslatableTextContent) badtext.getSiblings().get(0);
                //TranslatableTextContent newText = (TranslatableTextContent) translatableText.getArgs()[1];
                //list.add(2, Text.translatable(translatableText.getKey(), trailZeros(Float.parseFloat(String.valueOf(translatableText.getArgs()[0]))), Text.translatable(newText.getKey())).formatted(Formatting.DARK_GREEN));
            }
            list.removeAll(badlyFormattedList); // remove badly formatted lines
            for (int i = 0; i < list.size(); i++) {
                var translatableText = list.get(i);
                if (translatableText instanceof MutableText mutableText && translatableText.getContent() instanceof TranslatableTextContent translatableTextContent) {

                    if (translatableTextContent.getKey().startsWith("item.modifiers")) {
                        modifierList.add(mutableText);
                    }
                    if (modifierList.size() > 1) {
                        list.subList(i, list.size()).clear();
                    }
                }
            }
            if (modifierList.size() > 1) {
                list.removeAll(modifierList);
                Object[] args = new Object[modifierList.size()];
                for (int i = 0; i < modifierList.size(); i++) {
                    args[i] = Text.translatable("tooltip.tiered." + ((TranslatableTextContent) modifierList.get(i).getContent()).getKey());
                }
                list.add(1, Text.translatable("tooltip.tiered.modifier."+modifierList.size(), args).formatted(Formatting.GRAY));
            }

            for (Text text : list) {
                if (text instanceof MutableText mutableText
                        && mutableText.getContent() instanceof TranslatableTextContent listText
                        && listText.getKey().startsWith("attribute.modifier")) {
                    Object[] args = listText.getArgs();
                    TranslatableTextContent argText = (TranslatableTextContent) ((MutableText)args[1]).getContent();
                    if (!set.add(argText.getKey())) { // if there is more than one modifier with the same key
                        for (Text mutableTextCompare : list) {
                            if (mutableTextCompare instanceof MutableText textCompare
                                    && textCompare.getContent() instanceof TranslatableTextContent listTextCompare
                                    && listTextCompare.getKey().startsWith("attribute.modifier")) {
                                Object[] args2 = listTextCompare.getArgs();
                                TranslatableTextContent argText2 = (TranslatableTextContent) ((MutableText)args2[1]).getContent();;
                                if(argText.getKey().equals(argText2.getKey())) {
                                    noDuplicates.add(mutableText);
                                    noDuplicates.add(textCompare);
                                }
                            }
                        }
                    }

                }
            }
            MutableText[] noDuplicatesArray = new MutableText[noDuplicates.size()];
            noDuplicatesArray = noDuplicates.toArray(noDuplicatesArray);
            for (int i = 0; i < noDuplicates.size(); i ++) {
                for (int j = i + 1; j < noDuplicates.size(); j ++) {
                    TranslatableTextContent text = (TranslatableTextContent) noDuplicatesArray[i].getContent();
                    TranslatableTextContent text_compare = (TranslatableTextContent) noDuplicatesArray[j].getContent();
                    if (text.getArgs()[1] instanceof MutableText) {
                        var keyText = (MutableText)text.getArgs()[1];
                        TranslatableTextContent key = (TranslatableTextContent) keyText.getContent();
                        if(text_compare.getArgs()[1] instanceof MutableText) {
                            TranslatableTextContent key_compare = (TranslatableTextContent) ((MutableText)text_compare.getArgs()[1]).getContent();
                            float val1 = Float.parseFloat(String.valueOf(text.getArgs()[0]));
                            float val2 = Float.parseFloat(String.valueOf(text_compare.getArgs()[0]));
                            String val1Str = trailZeros(val1);
                            String val2Str = trailZeros(val2);
                            if (key.getKey().equals(key_compare.getKey())) {
                                Identifier tier = new Identifier(this.getOrCreateSubNbt(Tiered.NBT_SUBTAG_KEY).getString(Tiered.NBT_SUBTAG_DATA_KEY));
                                PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
                                list.remove(noDuplicatesArray[i]);
                                list.remove(noDuplicatesArray[j]);
                                switch (text.getKey() + text_compare.getKey()) {
                                    // I will turn this into a proper lookup table later lol
                                    case "attribute.modifier.plus.0attribute.modifier.plus.0" -> list.add(2, Text.translatable("tooltip.tiered.add", trailZeros(roundFloat(val1 + val2)), keyText, val1Str, val2Str).setStyle(attribute.getStyle()));
                                    case "attribute.modifier.plus.1attribute.modifier.equals.0", "attribute.modifier.plus.2attribute.modifier.equals.0", "attribute.modifier.plus.1attribute.modifier.plus.0", "attribute.modifier.plus.2attribute.modifier.plus.0" -> list.add(2, Text.translatable("tooltip.tiered.multiply", trailZeros(roundFloat((val2 * (val1 / 100.0f)) + val2)), keyText, val2Str, val1Str).setStyle(attribute.getStyle()));
                                    case "attribute.modifier.take.0attribute.modifier.plus.0" -> list.add(2, Text.translatable("tooltip.tiered.subtract", trailZeros(roundFloat(val2 - val1)), keyText, val2Str, val1Str).formatted(Formatting.RED));
                                    case "attribute.modifier.take.1attribute.modifier.equals.0", "attribute.modifier.take.2attribute.modifier.equals.0" -> list.add(2, Text.translatable("tooltip.tiered.divide", trailZeros(roundFloat(val2 - (val2 * (val1 / 100.0f)))), keyText, val2Str, val1Str).formatted(Formatting.RED));
                                    case "attribute.modifier.equals.0attribute.modifier.plus.1", "attribute.modifier.equals.0attribute.modifier.plus.2" -> list.add(2, Text.translatable("tooltip.tiered.multiply", trailZeros(roundFloat((val1 * (val2 / 100.0f)) + val1)), keyText, val1Str, val2Str).setStyle(attribute.getStyle()));
                                    case "attribute.modifier.equals.0attribute.modifier.take.1", "attribute.modifier.equals.0attribute.modifier.take.2" -> list.add(2, Text.translatable("tooltip.tiered.divide", trailZeros(roundFloat(val1 - (val1 * (val2 / 100.0f)))), keyText, val1Str, val2Str).formatted(Formatting.RED));
                                    default -> System.out.println("The combination of "+text.getKey()+" and "+text_compare.getKey()+" is not supported yet. Please make an issue on GitHub: https://github.com/Andrew6rant/tiered");
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
