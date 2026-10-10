package net.hexpowers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.hexpowers.core.HexPath;
import net.hexpowers.core.PlayerState;
import net.hexpowers.power.PowerManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

/**
 * /hex path <none|scarlet|wiccan|agatha> [targets]   (op)
 * /hex refill [targets]                              (op)
 * /hex info                                          (everyone)
 */
public final class HexCommands {
    private HexCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("hex");

        LiteralArgumentBuilder<ServerCommandSource> path = CommandManager.literal("path")
                .requires(src -> src.hasPermissionLevel(2));
        for (HexPath h : HexPath.values()) {
            path.then(CommandManager.literal(h.id)
                    .executes(ctx -> setPath(ctx, h, List.of(ctx.getSource().getPlayerOrThrow())))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ctx -> setPath(ctx, h, EntityArgumentType.getPlayers(ctx, "targets")))));
        }
        root.then(path);

        root.then(CommandManager.literal("refill")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> refill(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(ctx -> refill(ctx, EntityArgumentType.getPlayers(ctx, "targets")))));

        root.then(CommandManager.literal("info").executes(HexCommands::info));
        dispatcher.register(root);
    }

    private static int setPath(CommandContext<ServerCommandSource> ctx, HexPath h, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            PowerManager.setPath(p, h);
            p.sendMessage(Text.translatable("message.hexpowers.path_set",
                    Text.translatable(h.translationKey()).styled(st -> st.withColor(h.color))), true);
        }
        ctx.getSource().sendFeedback(() -> Text.translatable("commands.hexpowers.path", targets.size(),
                Text.translatable(h.translationKey())), true);
        return targets.size();
    }

    private static int refill(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) PowerManager.refill(p);
        ctx.getSource().sendFeedback(() -> Text.translatable("commands.hexpowers.refill", targets.size()), true);
        return targets.size();
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        PlayerState s = PowerManager.get(p);
        ctx.getSource().sendFeedback(() -> Text.translatable("commands.hexpowers.info",
                Text.translatable(s.path.translationKey()).styled(st -> st.withColor(s.path.color)),
                String.valueOf(Math.round(s.energy)), String.valueOf(Math.round(s.path.maxEnergy))), false);
        return 1;
    }
}
