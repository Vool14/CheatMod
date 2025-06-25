package com.example.cheatmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Mod("cheatmod")
public class CheatMod {
    private boolean killauraEnabled = false;
    private boolean flyEnabled = false;
    private boolean xrayEnabled = false;
    private boolean speedEnabled = false;
    private boolean antiKnockbackEnabled = false;
    private boolean espEnabled = false;
    private boolean itemSwapEnabled = false;
    private String targetItemId = "minecraft:diamond_sword";
    private TextFieldWidget itemInputField;
    private Minecraft mc;
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDir, "config/cheatmod_config.json");
    private final Random random = new Random();
    private int flyJumpTimer = 0;
    private int speedJumpTimer = 0;
    private boolean allowFlight = false;

    public CheatMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    private void clientSetup(FMLClientSetupEvent event) {
        mc = Minecraft.getInstance();
        ClientRegistry.registerKeyBinding(new KeyBinding(
            "key.cheatmod.menu",
            GLFW.GLFW_KEY_RIGHT_CONTROL,
            "category.cheatmod"
        ));
    }

    private void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                Gson gson = new Gson();
                Config config = gson.fromJson(reader, Config.class);
                killauraEnabled = config.killauraEnabled;
                flyEnabled = config.flyEnabled;
                xrayEnabled = config.xrayEnabled;
                speedEnabled = config.speedEnabled;
                antiKnockbackEnabled = config.antiKnockbackEnabled;
                espEnabled = config.espEnabled;
                itemSwapEnabled = config.itemSwapEnabled;
                targetItemId = config.targetItemId;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(CONFIG_FILE)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Config config = new Config();
                config.killauraEnabled = killauraEnabled;
                config.flyEnabled = flyEnabled;
                config.xrayEnabled = xrayEnabled;
                config.speedEnabled = speedEnabled;
                config.antiKnockbackEnabled = antiKnockbackEnabled;
                config.espEnabled = espEnabled;
                config.itemSwapEnabled = itemSwapEnabled;
                config.targetItemId = targetItemId;
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Config {
        boolean killauraEnabled;
        boolean flyEnabled;
        boolean xrayEnabled;
        boolean speedEnabled;
        boolean antiKnockbackEnabled;
        boolean espEnabled;
        boolean itemSwapEnabled;
        String targetItemId;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.player == null) return;

        if (mc.getKeyBinding("key.cheatmod.menu").isKeyDown()) {
            mc.setScreen(new CheatMenuScreen());
        }

        ClientPlayerEntity player = mc.player;

        // Killaura с рандомизацией
        if (killauraEnabled) {
            AxisAlignedBB box = new AxisAlignedBB(player.getPositionVec().subtract(4, 4, 4), player.getPositionVec().add(4, 4, 4));
            List<Entity> entities = mc.world.getEntitiesWithinAABB(LivingEntity.class, box, e -> e != player && e.isAlive());
            for (Entity entity : entities) {
                Vector3d playerPos = player.getPositionVec().add(0, player.getEyeHeight(), 0);
                Vector3d entityPos = entity.getPositionVec().add(0, entity.getHeight() / 2, 0);
                Vector3d lookVec = entityPos.subtract(playerPos);
                double yaw = Math.toDegrees(Math.atan2(lookVec.z, lookVec.x)) - 90 + random.nextFloat() * 10 - 5;
                double pitch = Math.toDegrees(-Math.atan2(lookVec.y, Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z))) + random.nextFloat() * 10 - 5;
                player.connection.sendPacket(new CPlayerPacket.Rotation((float) yaw, (float) pitch, player.isOnGround()));
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(50 + random.nextInt(50));
                        player.attackTargetEntityWithCurrentItem(entity);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        // Fly с имитацией прыжков
        if (flyEnabled && player != null) {
            if (!allowFlight) {
                flyJumpTimer++;
                if (flyJumpTimer >= 10 + random.nextInt(10)) {
                    player.setMotion(player.getMotion().add(0, 0.4, 0));
                    player.connection.sendPacket(new CPlayerPacket.PositionRotation(
                        player.getPosX(), player.getPosY(), player.getPosZ(),
                        player.rotationYaw, player.rotationPitch, true
                    ));
                    flyJumpTimer = 0;
                }
                if (player.isSprinting()) {
                    player.setMotion(player.getMotion().add(0, 0.1, 0));
                }
            } else {
                player.abilities.isFlying = true;
                player.abilities.setFlySpeed(0.05F);
            }
        } else if (player != null && !player.abilities.isCreativeMode) {
            player.abilities.isFlying = false;
        }

        // Speed с имитацией прыжков
        if (speedEnabled && player != null) {
            speedJumpTimer++;
            if (speedJumpTimer >= 15 + random.nextInt(10) && player.isOnGround()) {
                player.setMotion(player.getMotion().add(0, 0.3, 0));
                player.connection.sendPacket(new CPlayerPacket.PositionRotation(
                    player.getPosX(), player.getPosY(), player.getPosZ(),
                    player.rotationYaw, player.rotationPitch, true
                ));
                speedJumpTimer = 0;
            }
            player.setMotion(player.getMotion().scale(1.2));
        }

        // AntiKnockback с частичным отбрасыванием
        if (antiKnockbackEnabled && player != null) {
            player.setMotion(player.getMotion().x, player.getMotion().y * 0.2, player.getMotion().z);
        }

        // ItemSwap
        if (itemSwapEnabled && player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            Item targetItem = Registry.ITEM.getOrDefault(new ResourceLocation(targetItemId));
            if (heldItem.isEmpty() || heldItem.getItem() != targetItem || heldItem.getDamage() >= heldItem.getMaxDamage()) {
                swapItem(player, targetItem);
            }
        }
    }

    private void swapItem(ClientPlayerEntity player, Item targetItem) {
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem && stack.getDamage() < stack.getMaxDamage()) {
                validSlots.add(i);
            }
        }

        if (!validSlots.isEmpty() && random.nextFloat() < 0.8f) {
            int selectedSlot = validSlots.get(random.nextInt(validSlots.size()));
            CompletableFuture.runAsync(() -> {
                try {
                    double strafe = (random.nextFloat() - 0.5) * 0.1;
                    double forward = (random.nextFloat() - 0.5) * 0.1;
                    player.connection.sendPacket(new CPlayerPacket.PositionRotation(
                        player.getPosX() + strafe, player.getPosY(), player.getPosZ() + forward,
                        player.rotationYaw, player.rotationPitch, player.isOnGround()
                    ));
                    Thread.sleep(50 + random.nextInt(100));
                    player.inventory.currentItem = selectedSlot;
                    player.connection.sendPacket(new CHeldItemChangePacket(selectedSlot));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else if (targetItem != Items.AIR) {
            player.sendMessage(new StringTextComponent("No valid " + targetItemId + " found in inventory"), player.getUniqueID());
        } else {
            player.sendMessage(new StringTextComponent("Invalid item ID: " + targetItemId), player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.player == null || mc.world == null) return;

        // ESP: Подсветка сущностей
        if (espEnabled) {
            for (Entity entity : mc.world.getAllEntities()) {
                if (entity instanceof LivingEntity && entity != mc.player) {
                    RenderUtils.drawEntityBox(entity, 1.0F, 0.0F, 0.0F, 0.5F, event.getMatrixStack());
                }
            }
        }

        // XRay: Подсветка руд
        if (xrayEnabled) {
            ClientPlayerEntity player = mc.player;
            BlockPos playerPos = player.getPosition();
            int radius = 16; // Радиус сканирования
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        Block block = mc.world.getBlockState(pos).getBlock();
                        if (isValuableBlock(block)) {
                            RenderUtils.drawBlockBox(pos, 0.0F, 1.0F, 0.0F, 0.5F, event.getMatrixStack());
                        }
                    }
                }
            }
        }
    }

    private boolean isValuableBlock(Block block) {
        return block == Blocks.DIAMOND_ORE || block == Blocks.GOLD_ORE || block == Blocks.IRON_ORE ||
               block == Blocks.COAL_ORE || block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE ||
               block == Blocks.EMERALD_ORE;
    }

    public class CheatMenuScreen extends Screen {
        protected CheatMenuScreen() {
            super(new StringTextComponent("Cheat Menu"));
        }

        @Override
        protected void init() {
            int centerX = width / 2 - 60;
            int y = height / 2 - 100;
            addButton(new Button(centerX, y, 120, 20, new StringTextComponent("Killaura: " + (killauraEnabled ? "ON" : "OFF")),
                b -> {
                    killauraEnabled = !killauraEnabled;
                    b.setMessage(new StringTextComponent("Killaura: " + (killauraEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Auto-attack nearby entities"), x, m))
            ).setFGColor(killauraEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("Fly: " + (flyEnabled ? "ON" : "OFF")),
                b -> {
                    flyEnabled = !flyEnabled;
                    b.setMessage(new StringTextComponent("Fly: " + (flyEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Enable flying"), x, m))
            ).setFGColor(flyEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("XRay: " + (xrayEnabled ? "ON" : "OFF")),
                b -> {
                    xrayEnabled = !xrayEnabled;
                    b.setMessage(new StringTextComponent("XRay: " + (xrayEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("See ores through blocks"), x, m))
            ).setFGColor(xrayEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("Speed: " + (speedEnabled ? "ON" : "OFF")),
                b -> {
                    speedEnabled = !speedEnabled;
                    b.setMessage(new StringTextComponent("Speed: " + (speedEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Increase movement speed"), x, m))
            ).setFGColor(speedEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("AntiKnockback: " + (antiKnockbackEnabled ? "ON" : "OFF")),
                b -> {
                    antiKnockbackEnabled = !antiKnockbackEnabled;
                    b.setMessage(new StringTextComponent("AntiKnockback: " + (antiKnockbackEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Prevent knockback"), x, m))
            ).setFGColor(antiKnockbackEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("ESP: " + (espEnabled ? "ON" : "OFF")),
                b -> {
                    espEnabled = !espEnabled;
                    b.setMessage(new StringTextComponent("ESP: " + (espEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Highlight entities through walls"), x, m))
            ).setFGColor(espEnabled ? 0x00FF00 : 0xFF0000);
            addButton(new Button(centerX, y += 25, 120, 20, new StringTextComponent("ItemSwap: " + (itemSwapEnabled ? "ON" : "OFF")),
                b -> {
                    itemSwapEnabled = !itemSwapEnabled;
                    b.setMessage(new StringTextComponent("ItemSwap: " + (itemSwapEnabled ? "ON" : "OFF")));
                    saveConfig();
                }, (b, ms, x, m) -> renderTooltip(ms, new StringTextComponent("Auto-replace item in hand"), x, m))
            ).setFGColor(itemSwapEnabled ? 0x00FF00 : 0xFF0000);
            itemInputField = new TextFieldWidget(font, centerX, y += 25, 120, 20, new StringTextComponent("minecraft:diamond_sword"));
            itemInputField.setText(targetItemId);
            itemInputField.setResponder(text -> {
                targetItemId = text;
                saveConfig();
            });
            children().add(itemInputField);
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            RenderSystem.enableBlend();
            fillGradient(matrixStack, width / 2 - 100, height / 2 - 120, width / 2 + 100, height / 2 + 80, 0x80000000, 0x80333333);
            RenderSystem.disableBlend();
            drawCenteredString(matrixStack, font, "Cheat Menu", width / 2, height / 2 - 110, 0xFFFFFF);
            itemInputField.render(matrixStack, mouseX, mouseY, partialTicks);
            super.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (itemInputField.isFocused()) {
                itemInputField.keyPressed(keyCode, scanCode, modifiers);
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (itemInputField.isFocused()) {
                itemInputField.charTyped(codePoint, modifiers);
            }
            return super.charTyped(codePoint, modifiers);
        }

        @Override
        public void onClose() {
            saveConfig();
            super.onClose();
        }
    }
}
