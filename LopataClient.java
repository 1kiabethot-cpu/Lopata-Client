package com.lopata.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class LopataClient implements ClientModInitializer {
    private static final int PINK = 0xFFFF35B8;
    private static final int PANEL = 0xB9100A12;
    private static final int BORDER = 0xFFFF35B8;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        int x = 8;
        int y = 8;
        int width = 154;
        int line = 11;

        // Main watermark panel
        panel(ctx, x, y, width, 25);
        ctx.drawTextWithShadow(client.textRenderer,
                Text.literal("LOPATA").formatted(Formatting.WHITE, Formatting.BOLD)
                        .append(Text.literal(" CLIENT").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)),
                x + 7, y + 5, PINK);
        String stats = "FPS " + client.getCurrentFps() + "  |  " + LocalTime.now().format(CLOCK);
        ctx.drawTextWithShadow(client.textRenderer, stats, x + 7, y + 15, 0xFFE8DCE8);

        // Coordinates and facing
        y += 30;
        panel(ctx, x, y, width, 35);
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        ctx.drawTextWithShadow(client.textRenderer,
                String.format("XYZ  %.0f  %.0f  %.0f", px, py, pz),
                x + 7, y + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(client.textRenderer,
                "Facing  " + direction(client.player.getYaw()),
                x + 7, y + 17, PINK);

        // Active potion effects
        var effects = client.player.getStatusEffects();
        if (!effects.isEmpty()) {
            y += 40;
            int count = Math.min(effects.size(), 5);
            panel(ctx, x, y, width, 8 + count * line);
            ctx.drawTextWithShadow(client.textRenderer, "EFFECTS", x + 7, y + 4, PINK);
            int row = y + 16;
            int shown = 0;
            for (StatusEffectInstance effect : effects) {
                if (shown++ >= 5) break;
                String name = effect.getEffectType().value().getName().getString();
                int seconds = Math.max(0, effect.getDuration() / 20);
                String timer = String.format("%d:%02d", seconds / 60, seconds % 60);
                ctx.drawTextWithShadow(client.textRenderer,
                        name + "  " + timer, x + 7, row, 0xFFE8DCE8);
                row += line;
            }
        }

        // Armor durability list on the right side
        int rightX = Math.max(8, client.getWindow().getScaledWidth() - 92);
        int rightY = 8;
        var armor = client.player.getInventory().armor;
        for (int i = armor.size() - 1; i >= 0; i--) {
            var stack = armor.get(i);
            if (stack.isEmpty()) continue;
            int remaining = stack.getMaxDamage() - stack.getDamage();
            int max = Math.max(1, stack.getMaxDamage());
            int color = remaining * 100 / max < 25 ? 0xFFFF5555 : PINK;
            panel(ctx, rightX, rightY, 84, 17);
            ctx.drawTextWithShadow(client.textRenderer,
                    stack.getName().getString(), rightX + 5, rightY + 4, 0xFFFFFFFF);
            String percent = (remaining * 100 / max) + "%";
            int textWidth = client.textRenderer.getWidth(percent);
            ctx.drawTextWithShadow(client.textRenderer, percent,
                    rightX + 79 - textWidth, rightY + 4, color);
            rightY += 20;
        }
    }

    private static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + 2, y + h, BORDER);
        ctx.fill(x + 2, y, x + w, y + 1, 0x66FF35B8);
    }

    private static String direction(float yaw) {
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod(Math.round(yaw / 45.0f), 8);
        return dirs[index];
    }
}
