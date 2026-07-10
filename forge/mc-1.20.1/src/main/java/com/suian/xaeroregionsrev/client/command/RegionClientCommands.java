package com.suian.xaeroregionsrev.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.suian.xaeroregionsrev.client.ClientLocalConfig;
import com.suian.xaeroregionsrev.client.xaero.MapProjectionAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 客户端命令入口。自动校准是纯客户端渲染功能，因此通过客户端命令热切换，
 * 无需服务端参与、无 OP 权限门槛。
 *
 * <pre>
 * /region autoCalibrate            查询当前状态
 * /region autoCalibrate true|false 切换开关
 * </pre>
 */
public final class RegionClientCommands {
    private RegionClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("region")
                .then(literal("autoCalibrate")
                        .executes(context -> queryAutoCalibrate(context.getSource()))
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setAutoCalibrate(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled"))))));
    }

    private static int queryAutoCalibrate(CommandSourceStack source) {
        boolean enabled = ClientLocalConfig.shared().isAutoCalibrateEnabled();
        source.sendSuccess(() -> Component.translatable(
                "command.xaeroregionsrev.auto_calibrate.status",
                translateState(enabled)), false);
        return 1;
    }

    private static int setAutoCalibrate(CommandSourceStack source, boolean enabled) {
        ClientLocalConfig.shared().setAutoCalibrateEnabled(enabled);
        MapProjectionAdapter.shared().setCalibrationEnabled(enabled);
        source.sendSuccess(() -> Component.translatable(
                "command.xaeroregionsrev.auto_calibrate.set",
                translateState(enabled)), false);
        return 1;
    }

    private static Component translateState(boolean enabled) {
        return Component.translatable(enabled
                ? "command.xaeroregionsrev.auto_calibrate.on"
                : "command.xaeroregionsrev.auto_calibrate.off");
    }
}
