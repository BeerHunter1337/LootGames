package ru.timeconqueror.lootgames.client.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import ru.timeconqueror.lootgames.LootGames;
import ru.timeconqueror.lootgames.api.block.tile.BoardGameMasterTile;
import ru.timeconqueror.lootgames.api.util.Pos2i;
import ru.timeconqueror.lootgames.common.block.tile.SudokuTile;
import ru.timeconqueror.lootgames.minigame.sudoku.GameSudoku;
import ru.timeconqueror.lootgames.minigame.sudoku.SudokuBoard;
import ru.timeconqueror.timecore.api.util.client.DrawHelper;

public class SudokuRenderer extends TileEntitySpecialRenderer {

    public static ResourceLocation BOARD = new ResourceLocation(LootGames.MODID, "textures/game/sdk_board.png");

    // Border variant bitmask: bit0=right, bit1=bottom, bit2=left, bit3=top
    private static final int BORDER_RIGHT = 1;
    private static final int BORDER_BOTTOM = 2;
    private static final int BORDER_LEFT = 4;
    private static final int BORDER_TOP = 8;

    // 16 texture variants (indexed by bitmask); null = not yet initialised
    private static int[] boardTextures;

    @Override
    public void renderTileEntityAt(TileEntity teIn, double x, double y, double z, float partialTicks) {
        SudokuTile te = (SudokuTile) teIn;
        GameSudoku game = te.getGame();
        SudokuBoard board = game.getBoard();
        int size = game.getCurrentBoardSize();

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        BoardGameMasterTile.prepareMatrix(te);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        ensureBoardTextures();

        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                int variant = borderVariant(cx, cz, size);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, boardTextures[variant]);
                DrawHelper.drawTexturedRectByParts(cx, cz, 1, 1, -0.005f, 0, 0, 1, 1, 1);
            }
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Map<Integer, Integer> numberCounts = new HashMap<>();

        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                int puzzleVal = board.getPuzzleValue(cx, cz);
                int playerVal = board.getPlayerValue(new Pos2i(cx, cz));
                int val = puzzleVal != 0 ? puzzleVal : playerVal;
                if (val != 0) numberCounts.put(val, numberCounts.getOrDefault(val, 0) + 1);
            }
        }

        Set<Pos2i> duplicatePositions = new HashSet<>();
        Set<Pos2i> correctCompletedPositions = new HashSet<>();

        for (int row = 0; row < size; row++) {
            Set<Pos2i> section = new HashSet<>();
            Map<Integer, Integer> counts = new HashMap<>();
            boolean valid = true;

            for (int col = 0; col < size; col++) {
                Pos2i pos = new Pos2i(row, col);
                int val = board.getPuzzleValue(pos);
                if (val == 0) val = board.getPlayerValue(pos);
                section.add(pos);

                if (val < 1 || val > 9) valid = false;
                if (val != 0) {
                    counts.put(val, counts.getOrDefault(val, 0) + 1);
                }
            }

            for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                if (e.getValue() > 1) {
                    valid = false;
                    for (int col = 0; col < size; col++) {
                        Pos2i pos = new Pos2i(row, col);
                        int val = board.getPuzzleValue(pos);
                        if (val == 0) val = board.getPlayerValue(pos);
                        if (val == e.getKey()) duplicatePositions.add(pos);
                    }
                }
            }

            if (valid && counts.size() == 9) correctCompletedPositions.addAll(section);
        }

        for (int col = 0; col < size; col++) {
            Set<Pos2i> section = new HashSet<>();
            Map<Integer, Integer> counts = new HashMap<>();
            boolean valid = true;

            for (int row = 0; row < size; row++) {
                Pos2i pos = new Pos2i(row, col);
                int val = board.getPuzzleValue(pos);
                if (val == 0) val = board.getPlayerValue(pos);
                section.add(pos);

                if (val < 1 || val > 9) valid = false;
                if (val != 0) {
                    counts.put(val, counts.getOrDefault(val, 0) + 1);
                }
            }

            for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                if (e.getValue() > 1) {
                    valid = false;
                    for (int row = 0; row < size; row++) {
                        Pos2i pos = new Pos2i(row, col);
                        int val = board.getPuzzleValue(pos);
                        if (val == 0) val = board.getPlayerValue(pos);
                        if (val == e.getKey()) duplicatePositions.add(pos);
                    }
                }
            }

            if (valid && counts.size() == 9) correctCompletedPositions.addAll(section);
        }

        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<Pos2i> section = new HashSet<>();
                Map<Integer, Integer> counts = new HashMap<>();
                boolean valid = true;

                for (int dy = 0; dy < 3; dy++) {
                    for (int dx = 0; dx < 3; dx++) {
                        int row = boxRow * 3 + dy;
                        int col = boxCol * 3 + dx;
                        Pos2i pos = new Pos2i(row, col);
                        int val = board.getPuzzleValue(pos);
                        if (val == 0) val = board.getPlayerValue(pos);
                        section.add(pos);

                        if (val < 1 || val > 9) valid = false;
                        if (val != 0) {
                            counts.put(val, counts.getOrDefault(val, 0) + 1);
                        }
                    }
                }

                for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                    if (e.getValue() > 1) {
                        valid = false;
                        for (Pos2i pos : section) {
                            int val = board.getPuzzleValue(pos);
                            if (val == 0) val = board.getPlayerValue(pos);
                            if (val == e.getKey()) duplicatePositions.add(pos);
                        }
                    }
                }

                if (valid && counts.size() == 9) correctCompletedPositions.addAll(section);
            }
        }

        Minecraft mc = Minecraft.getMinecraft();

        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                Pos2i pos = new Pos2i(cx, cz);
                int puzzleVal = board.getPuzzleValue(cx, cz);
                int playerVal = board.getPlayerValue(pos);
                int actualVal = puzzleVal != 0 ? puzzleVal : playerVal;

                if (actualVal != 0) {
                    int count = numberCounts.getOrDefault(actualVal, 0);
                    int color;

                    if (count > 9) {
                        color = 0xFFAAAA;
                    } else if (duplicatePositions.contains(pos)) {
                        color = 0xFFFF00;
                    } else if (correctCompletedPositions.contains(pos)) {
                        color = 0x00FFFF;
                    } else if (count == 9) {
                        color = 0x00FF00;
                    } else {
                        color = puzzleVal != 0 ? 0x808080 : 0xFFFFFF;
                    }

                    String text = Integer.toString(actualVal);

                    float stringWidth = mc.fontRenderer.getStringWidth(text);
                    float stringHeight = mc.fontRenderer.FONT_HEIGHT;

                    float scale = 0.08f;
                    float offsetX = -stringWidth * scale / 2f;
                    float offsetY = -stringHeight * scale / 2f;

                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    GL11.glTranslatef(cx + 0.525f, cz + 0.55f, -0.02f);
                    GL11.glScalef(scale, scale, scale);
                    GL11.glTranslatef(offsetX / scale, offsetY / scale, 0);
                    mc.fontRenderer.drawString(text, 0, 0, color, false);
                    GL11.glPopMatrix();

                    GL11.glPushMatrix();
                    GL11.glTranslatef(cx + 0.525f + 0.025f, cz + 0.55f + 0.025f, -0.01f);
                    GL11.glScalef(scale, scale, scale);
                    GL11.glTranslatef(offsetX / scale, offsetY / scale, 0);
                    int shadowColor = (color & 0xfcfcfc) >> 2;
                    mc.fontRenderer.drawString(text, 0, 0, shadowColor, false);
                    GL11.glPopMatrix();

                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                } else if (puzzleVal == 0) {
                    boolean[] cellNotes = board.getNotes(pos);
                    float noteScale = 0.027f;
                    float px = 1.0f / 16.0f;
                    float notesBoxStart = 1.25f * px;
                    float miniCellSize = 4.5f * px;
                    for (int d = 1; d <= 9; d++) {
                        if (!cellNotes[d - 1]) continue;

                        int noteCol = (d - 1) % 3;
                        int noteRow = (d - 1) / 3;
                        String text = Integer.toString(d);
                        float textWidth = mc.fontRenderer.getStringWidth(text) * noteScale;
                        float textHeight = mc.fontRenderer.FONT_HEIGHT * noteScale;
                        float miniCellX = cx + notesBoxStart + noteCol * miniCellSize;
                        float miniCellZ = cz + notesBoxStart + noteRow * miniCellSize;
                        float nx = miniCellX + (miniCellSize - textWidth) / 2.0f;
                        float nz = miniCellZ + (miniCellSize - textHeight) / 2.0f;

                        GL11.glPushMatrix();
                        GL11.glTranslatef(nx, nz, -0.02f);
                        GL11.glScalef(noteScale, noteScale, noteScale);
                        mc.fontRenderer.drawString(text, 0, 0, 0xAAAAAA, false);
                        GL11.glPopMatrix();
                    }
                }
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();

        SudokuOverlayHandler.addSupportedMaster(te.getBlockPos(), game);
    }

    private static int borderVariant(int cx, int cz, int size) {
        int v = 0;
        if ((cx + 1) % 3 == 0) v |= BORDER_RIGHT;
        if ((cz + 1) % 3 == 0) v |= BORDER_BOTTOM;
        if (cx == 0) v |= BORDER_LEFT;
        if (cz == 0) v |= BORDER_TOP;
        return v;
    }

    private static void ensureBoardTextures() {
        if (boardTextures != null) return;

        boardTextures = new int[16];
        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(BOARD);
            BufferedImage base = javax.imageio.ImageIO.read(res.getInputStream());
            int w = base.getWidth();
            int h = base.getHeight();
            int borderPx = Math.max(1, w / 16);

            for (int variant = 0; variant < 16; variant++) {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.drawImage(base, 0, 0, null);
                if (variant != 0) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                    g.setColor(new Color(0.85f, 0.85f, 0.85f));
                    if ((variant & BORDER_RIGHT) != 0) g.fillRect(w - borderPx, 0, borderPx, h);
                    if ((variant & BORDER_BOTTOM) != 0) g.fillRect(0, h - borderPx, w, borderPx);
                    if ((variant & BORDER_LEFT) != 0) g.fillRect(0, 0, borderPx, h);
                    if ((variant & BORDER_TOP) != 0) g.fillRect(0, 0, w, borderPx);
                }
                g.dispose();
                boardTextures[variant] = uploadTexture(img);
            }
        } catch (IOException e) {
            LootGames.LOGGER.error("Failed to load board texture variants", e);
        }
    }

    private static int uploadTexture(BufferedImage img) {
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
        for (int px : pixels) {
            buf.put((byte) ((px >> 16) & 0xFF)); // R
            buf.put((byte) ((px >> 8) & 0xFF)); // G
            buf.put((byte) (px & 0xFF)); // B
            buf.put((byte) ((px >> 24) & 0xFF)); // A
        }
        buf.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        return id;
    }
}
