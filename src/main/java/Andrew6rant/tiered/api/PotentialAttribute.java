package Andrew6rant.tiered.api;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.List;

public class PotentialAttribute {

    private final String id;
    private final List<ItemVerifier> verifiers;
    private final Style style;
    private final int[] tooltip_image;
    private final String[] tooltip_border;
    private final int weight;
    private final int reforge_cost;
    private final List<AttributeTemplate> attributes;

    public PotentialAttribute(String id, List<ItemVerifier> verifiers, Style style, int[] tooltip_image, String[] tooltip_border, int weight, int reforge_cost, List<AttributeTemplate> attributes) {
        this.id = id;
        this.verifiers = verifiers;
        this.style = style;
        this.tooltip_image = tooltip_image;
        this.tooltip_border = tooltip_border;
        this.weight = weight;
        this.reforge_cost = reforge_cost;
        this.attributes = attributes;
    }

    public String getID() {
        return id;
    }

    public int[] getTooltip_image() {
        return tooltip_image;
    }

    public int getTooltip_border(int i) {
        if (tooltip_border.length > 1) {
            return (int)Long.parseUnsignedLong(tooltip_border[i], 16);
        } else {
            return (int)Long.parseUnsignedLong(tooltip_border[0], 16);
        }
    }

    public int getWeight() {
        return weight;
    }

    public int getReforge_cost() {
        return reforge_cost;
    }

    public List<ItemVerifier> getVerifiers() {
        return verifiers;
    }

    public boolean isValid(Identifier id) {
        for(ItemVerifier verifier : verifiers) {
            if(verifier.isValid(id)) {
                return true;
            }
        }
        return false;
    }

    public Style getStyle() {
        return style;
    }

    public List<AttributeTemplate> getAttributes() {
        return attributes;
    }
}
