package net.hexpowers;

import net.hexpowers.core.HexPath;
import net.hexpowers.power.FX;
import net.hexpowers.power.Vfx;
import net.hexpowers.power.PowerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Right-click: cycle Scarlet Witch -> Wiccan -> Agatha -> none. Sneak + right-click: give the powers back. */
public class DarkholdItem extends Item {
    public DarkholdItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw && user instanceof ServerPlayerEntity sp) {
            HexPath np = user.isSneaking() ? HexPath.NONE : PowerManager.get(sp).path.next();
            PowerManager.setPath(sp, np);
            sp.getItemCooldownManager().set(this, 20);
            if (np.active()) {
                FX.burst(sw, sp.getPos().add(0, 1, 0), np.color, 50);
                Vfx.ground(sw, np, sp.getPos(), 2.6f, 56, 0, sp);
                Vfx.pillar(sw, np, sp.getPos(), 1.0f, 4.5f, 44, 0, sp);
                Vfx.ringGround(sw, np, sp.getPos(), 5.5f, 16, 0);
                Vfx.flash(sw, np, sp.getPos().add(0, 1, 0), 3.4f, 10, 0);
                FX.puff(sw, ParticleTypes.ENCHANT, sp.getPos().add(0, 1.5, 0), 40, 0.6, 0.6);
                FX.sound(sw, sp.getPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
                FX.sound(sw, sp.getPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
            } else {
                FX.puff(sw, ParticleTypes.SMOKE, sp.getPos().add(0, 1, 0), 30, 0.5, 0.05);
                FX.sound(sw, sp.getPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            }
            sp.sendMessage(Text.translatable("message.hexpowers.path_set",
                    Text.translatable(np.translationKey()).styled(st -> st.withColor(np.color))), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hexpowers.darkhold.tooltip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.hexpowers.darkhold.tooltip2").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("item.hexpowers.darkhold.tooltip3").formatted(Formatting.DARK_GRAY));
    }
}
