package com.tac5studios.codexarmorstands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.authlib.properties.PropertyMap;
import com.tac5studios.codexarmorstands.CodexArmorStands;
import com.tac5studios.codexarmorstands.config.Features;
import com.tac5studios.codexarmorstands.config.ToolConfig;
import com.tac5studios.codexarmorstands.edit.Presets;
import com.tac5studios.codexarmorstands.edit.Protection;
import com.tac5studios.codexarmorstands.edit.Stands;
import com.tac5studios.codexarmorstands.edit.Targeting;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.perms.Perm;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * /armorstand tool [player]   give the edit tool
 * /armorstand head <name>     give a player head
 * /armorstand info            owner and state of the stand you're looking at
 * /armorstand reload          reload messages.toml and the preset files
 * Also /as as a short alias.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Commands {

    private Commands() {}

    @SubscribeEvent
    @SuppressWarnings("unused") // called by the event bus
    public static void onRegister(RegisterCommandsEvent e) {
        if (!Features.editor()) return;
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        var root = d.register(build());
        d.register(net.minecraft.commands.Commands.literal("as").requires(s -> Perm.has(s, Perm.USE)).redirect(root));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = net.minecraft.commands.Commands.literal("armorstand")
                .requires(s -> Perm.has(s, Perm.USE) || Perm.has(s, Perm.ADMIN));

        if (Features.GIVE_TOOL.get()) {
            root.then(net.minecraft.commands.Commands.literal("tool")
                    .requires(s -> Perm.has(s, Perm.TOOL))
                    .executes(c -> giveTool(c, c.getSource().getPlayerOrException()))
                    .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                            .requires(s -> Perm.has(s, Perm.ADMIN))
                            .executes(c -> giveTool(c, EntityArgument.getPlayer(c, "player")))));
        }
        if (Features.PLAYER_HEADS.get()) {
            root.then(net.minecraft.commands.Commands.literal("head")
                    .requires(s -> Perm.has(s, Perm.HEAD))
                    .then(net.minecraft.commands.Commands.argument("name", StringArgumentType.word())
                            .executes(Commands::head)));
        }
        root.then(net.minecraft.commands.Commands.literal("info").executes(Commands::info));
        root.then(net.minecraft.commands.Commands.literal("reload")
                .requires(s -> Perm.has(s, Perm.ADMIN))
                .executes(Commands::reload));
        return root;
    }

    private static int giveTool(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        ItemStack tool = ToolConfig.create();
        if (!target.getInventory().add(tool)) target.drop(tool, false);
        Msg.send(c.getSource(), "cmd.tool_given", "player", target.getGameProfile().getName());
        return 1;
    }

    private static int head(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            Msg.fail(c.getSource(), "cmd.head_bad_name");
            return 0;
        }
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        // The client fills in the skin from the name, so this works for players who never joined.
        head.set(DataComponents.PROFILE, new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap()));
        if (!p.getInventory().add(head)) p.drop(head, false);
        Msg.send(c.getSource(), "cmd.head_given", "name", name);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ArmorStand s = Targeting.lookedAt(p, 6);
        if (s == null) {
            Msg.fail(c.getSource(), "cmd.info_none");
            return 0;
        }
        UUID owner = Protection.owner(s);
        String ownerName = Msg.raw("cmd.info_no_owner");
        if (owner != null) {
            ServerPlayer online = p.server.getPlayerList().getPlayer(owner);
            ownerName = online != null ? online.getGameProfile().getName()
                    : p.server.getProfileCache() == null ? owner.toString()
                    : p.server.getProfileCache().get(owner).map(com.mojang.authlib.GameProfile::getName).orElse(owner.toString());
        }
        Msg.send(c.getSource(), "cmd.info",
                "owner", ownerName,
                "locked", Msg.raw(Stands.locked(s) ? "state.on" : "state.off"),
                "editable", Msg.raw(Protection.canEdit(p, s) ? "state.yes" : "state.no"),
                "scale", String.format(java.util.Locale.ROOT, "%.2f", Stands.scale(s)));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        Msg.load();
        Presets.load();
        Msg.send(c.getSource(), "cmd.reloaded", "presets", Presets.all().size());
        return 1;
    }
}
