package dev.bobodado.acquiredutils.client.gui;

import dev.bobodado.acquiredutils.AcquiredUtils;
import dev.bobodado.acquiredutils.client.gui.section.GuiRow;
import dev.bobodado.acquiredutils.client.gui.section.ModSection;
import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import dev.bobodado.acquiredutils.client.gui.widget.DropdownWidget;
import dev.bobodado.acquiredutils.client.gui.widget.ThemedButtonWidget;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class AcquiredUtilsConfigScreen extends Screen {

    private static final Identifier ICON_GEAR =
        Identifier.fromNamespaceAndPath("acquiredutils", "textures/gui/icon_gear.png");
    private static final Identifier ICON_KEYBOARD =
        Identifier.fromNamespaceAndPath("acquiredutils", "textures/gui/icon_keyboard.png");
    private static final Identifier ICON_MOD =
        Identifier.fromNamespaceAndPath("acquiredutils", "textures/gui/icon_header.png");
    private static final Identifier ICON_MOD_SMALL =
        Identifier.fromNamespaceAndPath("acquiredutils", "textures/gui/icon_mod.png");
    private static final Identifier BACKGROUND_TEXTURE =
        Identifier.fromNamespaceAndPath("acquiredutils", "textures/gui/menu_backdrop.png");

    public static final int BASE_PANEL_WIDTH = 560;
    public static final int BASE_PANEL_HEIGHT = 310;
    public static final int BASE_HEADER_HEIGHT = 56;
    public static final int BASE_FOOTER_HEIGHT = 28;
    public static final int BASE_SIDEBAR_WIDTH = 126;
    public static final int BASE_PADDING = 10;
    public static final int BASE_TAB_HEIGHT = 22;

    private final Screen parent;

    private float menuScale;
    private int panelWidth, panelHeight, headerHeight, footerHeight;
    private int sidebarWidth, padding, tabHeight;
    private int panelX, panelY;

    private final Map<String, ModSection> sections = new LinkedHashMap<>();
    private String activeSectionId;
    private final List<AbstractWidget> sectionWidgets = new ArrayList<>();

    private final List<TabPos> tabPositions = new ArrayList<>();
    private record TabPos(int x, int y, int w, int h, String id) {}

    private final List<StoredText> storedTexts = new ArrayList<>();
    private record StoredText(
        String translationKey,
        int x,
        int y,
        boolean isLabel,
        int maxWidth
    ) {}

    private final List<RowCard> rowCards = new ArrayList<>();
    private record RowCard(int x, int y, int w, int h) {}

    private boolean needsRebuild = false;
    private EditBox searchBox;
    private String searchQuery = "";
    private static final int SEARCH_WIDTH_BASE = 150;
    private static final int SEARCH_HEIGHT_BASE = 18;

    private int contentScroll = 0;
    private int maxContentScroll = 0;
    private int sidebarScroll = 0;
    private int maxSidebarScroll = 0;

    private static final int ROW_HEIGHT_BASE = 62;
    private static final int ROW_GAP_BASE = 8;
    private static final int CONTENT_TOP_BASE = 36;

    public AcquiredUtilsConfigScreen(Screen parent) {
        super(Component.translatable("acquiredutils.gui.title"));
        this.parent = parent;
    }

    public void registerSection(ModSection section) {
        sections.put(section.getId(), section);

        if (activeSectionId == null) {
            activeSectionId = section.getId();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public net.minecraft.client.Minecraft getMinecraft() {
        return this.minecraft;
    }

    public float getMenuScale() {
        return menuScale;
    }

    public int s(int base) {
        return (int) (base * menuScale);
    }

    public void addSectionWidget(AbstractWidget widget) {
        sectionWidgets.add(widget);
        addRenderableWidget(widget);
    }

    public void scheduleRebuild() {
        needsRebuild = true;
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            boolean searchFocused = searchBox != null && searchBox.isFocused();
            init();
            if (searchFocused && searchBox != null) {
                searchBox.setFocused(true);
                searchBox.setCursorPosition(searchBox.getValue().length());
            }
        }
    }

    private void computeLayout() {
        this.menuScale = AcquiredUtilsConfig.get().menuScale;

        float maxScaleX = (float) this.width / BASE_PANEL_WIDTH;
        float maxScaleY = (float) this.height / BASE_PANEL_HEIGHT;

        this.menuScale = Math.max(
            0.5f,
            Math.min(
                this.menuScale,
                Math.min(maxScaleX, maxScaleY)
            )
        );

        this.panelWidth = s(BASE_PANEL_WIDTH);
        this.panelHeight = s(BASE_PANEL_HEIGHT);
        this.headerHeight = s(BASE_HEADER_HEIGHT);
        this.footerHeight = s(BASE_FOOTER_HEIGHT);
        this.sidebarWidth = s(BASE_SIDEBAR_WIDTH);
        this.padding = s(BASE_PADDING);
        this.tabHeight = s(BASE_TAB_HEIGHT);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;
    }

    @Override
    protected void init() {
        computeLayout();
        clearWidgets();
        sectionWidgets.clear();
        tabPositions.clear();
        storedTexts.clear();
        rowCards.clear();

        buildHeader();
        buildSearchBox();
        buildSidebarTabs();
        buildContent();
        buildFooterButton();
    }

    private void buildHeader() {
        // Intentionally empty: the header is visual only. The close button
        // lives in the footer to match the layout specification.
    }

    private void buildSearchBox() {
        int contentX = panelX + sidebarWidth + padding;
        int contentY = panelY + headerHeight + padding;
        int contentW = panelWidth - sidebarWidth - padding * 2;

        int width = s(SEARCH_WIDTH_BASE);
        int height = s(SEARCH_HEIGHT_BASE);
        int x = contentX + contentW - width - s(4);
        int y = contentY + s(6);

        searchBox = new EditBox(
            this.font,
            x,
            y,
            width,
            height,
            Component.translatable("acquiredutils.gui.search")
        );
        searchBox.setMaxLength(80);
        searchBox.setBordered(true);
        searchBox.setTextColor(0xFFF2EAF7);
        searchBox.setSuggestion(searchQuery.isEmpty() ? "Search settings..." : null);
        searchBox.setHint(Component.translatable("acquiredutils.gui.search"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals(searchQuery)) {
                searchQuery = normalized;
                searchBox.setSuggestion(normalized.isEmpty() ? "Search settings..." : null);
                contentScroll = 0;
                sidebarScroll = 0;
                needsRebuild = true;
            }
        });

        addRenderableWidget(searchBox);
    }

    private void buildFooterButton() {
        int closeWidth = s(24);
        int closeHeight = s(18);
        addRenderableWidget(new ThemedButtonWidget(
            panelX + panelWidth - closeWidth - padding,
            panelY + panelHeight - footerHeight + (footerHeight - closeHeight) / 2,
            closeWidth,
            closeHeight,
            Component.literal("×"),
            this::onClose
        ));
    }

    private void buildSidebarTabs() {
        int tabX = panelX + padding;
        int tabWidth = sidebarWidth - padding * 2;
        int baseTabY = panelY + headerHeight + padding;
        int gap = s(4);

        List<ModSection> matchingSections = getMatchingSections();

        if (searchQuery.isEmpty() == false
            && !matchingSections.isEmpty()
            && !matchingSections.stream().anyMatch(section -> section.getId().equals(activeSectionId))) {
            activeSectionId = matchingSections.get(0).getId();
        }

        int totalHeight = matchingSections.size() * tabHeight
            + Math.max(0, matchingSections.size() - 1) * gap;
        int viewportHeight = panelHeight - headerHeight - footerHeight - padding * 2;
        maxSidebarScroll = Math.max(0, totalHeight - viewportHeight);
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, maxSidebarScroll));

        int i = 0;
        for (ModSection section : matchingSections) {
            final String id = section.getId();
            int y = baseTabY + i * (tabHeight + gap) - sidebarScroll;

            tabPositions.add(new TabPos(tabX, y, tabWidth, tabHeight, id));

            ThemedButtonWidget tab = new ThemedButtonWidget(
                tabX, y, tabWidth, tabHeight,
                section.getDisplayName(),
                () -> switchTab(id),
                true
            );

            tab.visible = y + tabHeight > panelY + headerHeight
                && y < panelY + panelHeight - footerHeight;

            addRenderableWidget(tab);
            i++;
        }
    }

    private List<ModSection> getMatchingSections() {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(sections.values());
        }

        List<ModSection> result = new ArrayList<>();
        for (ModSection section : sections.values()) {
            if (matchesSection(section)) {
                result.add(section);
            }
        }
        return result;
    }

    private boolean matchesSection(ModSection section) {
        String name = section.getDisplayName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(searchQuery)) {
            return true;
        }

        for (GuiRow row : section.getRows()) {
            String label = row.labelKey() == null
                ? ""
                : Component.translatable(row.labelKey()).getString().toLowerCase(Locale.ROOT);
            String desc = row.descKey() == null
                ? ""
                : Component.translatable(row.descKey()).getString().toLowerCase(Locale.ROOT);

            if (label.contains(searchQuery) || desc.contains(searchQuery)) {
                return true;
            }
        }
        return false;
    }

    private List<GuiRow> getFilteredRows(ModSection section) {
        List<GuiRow> rows = section.getRows();
        if (searchQuery.isEmpty()) {
            return rows;
        }

        List<GuiRow> result = new ArrayList<>();
        for (GuiRow row : rows) {
            String label = row.labelKey() == null
                ? ""
                : Component.translatable(row.labelKey()).getString().toLowerCase(Locale.ROOT);
            String desc = row.descKey() == null
                ? ""
                : Component.translatable(row.descKey()).getString().toLowerCase(Locale.ROOT);

            if (label.contains(searchQuery) || desc.contains(searchQuery)) {
                result.add(row);
            }
        }
        return result;
    }

    private void buildContent() {
        ModSection active = sections.get(activeSectionId);
        if (active == null) return;

        int contentX = panelX + sidebarWidth + padding;
        int contentY = panelY + headerHeight + padding;
        int contentW = panelWidth - sidebarWidth - padding * 2;
        int contentH = panelHeight - headerHeight - footerHeight - padding * 2;

        List<GuiRow> rows = getFilteredRows(active);
        int rowHeight = s(ROW_HEIGHT_BASE);
        int rowGap = s(ROW_GAP_BASE);
        int contentTop = contentY + s(CONTENT_TOP_BASE);
        int viewportHeight = contentH - s(CONTENT_TOP_BASE);

        int totalHeight = rows.isEmpty()
            ? 0
            : rows.size() * rowHeight + (rows.size() - 1) * rowGap;

        maxContentScroll = Math.max(0, totalHeight - viewportHeight);
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));

        // Reserve a clean scrollbar gutter so the bar never sits on top of
        // setting cards or controls.
        int scrollbarGutter = s(12);
        int bodyW = Math.max(s(160), contentW - scrollbarGutter);

        int rowY = contentTop - contentScroll;

        if (rows.isEmpty() && !searchQuery.isEmpty()) {
            rowCards.add(new RowCard(
                contentX,
                contentTop,
                bodyW,
                rowHeight
            ));
        }

        for (GuiRow row : rows) {
            int controlW = row.controlWidth() < 0
                ? bodyW
                : Math.min(s(row.controlWidth()), bodyW);

            int controlH = s(row.controlHeight());
            int controlShift = row.controlWidth() < 0 ? 0 : s(12);
            int controlX = row.controlWidth() < 0
                ? contentX
                : contentX + bodyW - controlW - controlShift;

            int controlY = rowY + s(8);

            AbstractWidget widget = row.factory().create(
                controlX,
                controlY,
                controlW,
                controlH
            );

            int viewportTop = contentY + s(22);
            int viewportBottom = contentY + contentH;
            widget.visible = rowY + rowHeight > viewportTop
                && rowY < viewportBottom;

            rowCards.add(new RowCard(
                contentX,
                rowY,
                bodyW,
                rowHeight
            ));

            addSectionWidget(widget);

            int textX = contentX + s(16);
            int textWidth = row.controlWidth() < 0
                ? bodyW - s(24)
                : Math.max(s(90), bodyW - controlW - s(40));

            if (row.labelKey() != null) {
                storedTexts.add(new StoredText(
                    row.labelKey(),
                    textX,
                    rowY + s(6),
                    true,
                    textWidth
                ));
            }

            if (row.descKey() != null) {
                storedTexts.add(new StoredText(
                    row.descKey(),
                    textX,
                    rowY + s(28),
                    false,
                    textWidth
                ));
            }

            rowY += rowHeight + rowGap;
        }
    }

    private void switchTab(String id) {
        ModSection old = sections.get(activeSectionId);

        if (old != null) {
            old.onClose();
        }

        activeSectionId = id;
        init();
    }

    @Override
    public void render(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        Theme theme = Theme.current();

        graphics.fill(0, 0, this.width, this.height, 0x4C05030A);

        drawPanelChrome(graphics, theme);

        graphics.enableScissor(panelX, panelY, panelX + panelWidth, panelY + panelHeight);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND_TEXTURE,
            panelX, panelY,
            0.0f, 0.0f,
            panelWidth, panelHeight,
            1600, 900
        );
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x7A07030F);
        graphics.disableScissor();

        int logoSize = Math.min(s(24), headerHeight - s(6));
        int logoX = panelX + padding + s(3);
        int logoY = panelY + (headerHeight - logoSize) / 2;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ICON_MOD,
            logoX,
            logoY,
            0.0f, 0.0f,
            logoSize, logoSize,
            32, 32
        );

        int titleX = logoX + logoSize + s(5);
        int titleY = panelY + (headerHeight - this.font.lineHeight) / 2;

        Component titlePrefix = Component.literal("AcquiredUtils")
            .copy()
            .withStyle(Style.EMPTY.withBold(true));
        Component titleBy = Component.literal(" by ");
        Component titleAuthor = Component.literal("ii8we")
            .copy()
            .withStyle(Style.EMPTY.withBold(true));

        graphics.drawString(this.font, titlePrefix, titleX, titleY, theme.accentBright, false);
        int afterPrefix = titleX + this.font.width(titlePrefix);
        graphics.drawString(this.font, titleBy, afterPrefix, titleY, theme.text, false);
        int afterBy = afterPrefix + this.font.width(titleBy);
        graphics.drawString(this.font, titleAuthor, afterBy, titleY, theme.text, false);

        String version = "v1.0.0";
        graphics.drawString(
            this.font,
            Component.literal(version),
            panelX + panelWidth - padding - this.font.width(version),
            titleY,
            theme.credit,
            false
        );

        int underlineY = panelY + headerHeight - 1;

        graphics.fill(
            panelX,
            underlineY,
            panelX + panelWidth,
            underlineY + 1,
            theme.divider
        );
        graphics.fill(
            panelX + s(2),
            underlineY - s(1),
            panelX + s(34),
            underlineY + s(1),
            theme.accentBright
        );

        graphics.fill(
            panelX + sidebarWidth,
            panelY + headerHeight,
            panelX + sidebarWidth + 1,
            panelY + panelHeight - footerHeight,
            theme.divider
        );

        graphics.drawString(
            this.font,
            Component.literal("Settings are saved automatically"),
            panelX + padding,
            panelY + panelHeight - footerHeight + s(9),
            theme.credit,
            false
        );

        int contentX = panelX + sidebarWidth + padding;
        int contentY = panelY + headerHeight + padding;
        int contentW = panelWidth - sidebarWidth - padding * 2;
        int contentH = panelHeight - headerHeight - footerHeight - padding * 2;

        ModSection active = sections.get(activeSectionId);

        graphics.fill(
            contentX,
            contentY,
            contentX + contentW,
            contentY + contentH,
            0xA8120C1A
        );

        graphics.fill(
            contentX + s(4),
            contentY + s(7),
            contentX + contentW - s(4),
            contentY + s(8),
            theme.divider
        );

        graphics.enableScissor(
            contentX,
            contentY,
            contentX + contentW,
            contentY + contentH
        );

        for (RowCard card : rowCards) {
            drawRowCard(graphics, card, theme);
        }

        if (active != null) {
            active.render(
                graphics,
                mouseX,
                mouseY,
                partialTick,
                contentX,
                contentY,
                Math.max(s(160), contentW - s(12)),
                contentH
            );
        }

        graphics.disableScissor();

        drawScrollbars(graphics, theme, contentX, contentY, contentW, contentH);

        // Content widgets are rendered separately under the viewport scissor.
        // This prevents buttons/sliders/dropdowns from leaking outside the
        // scrollable area when the user scrolls up or down.
        List<Boolean> contentVisibility = new ArrayList<>(sectionWidgets.size());
        for (AbstractWidget widget : sectionWidgets) {
            contentVisibility.add(widget.visible);
            widget.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < sectionWidgets.size(); i++) {
            sectionWidgets.get(i).visible = contentVisibility.get(i);
        }

        graphics.enableScissor(
            contentX,
            contentY,
            contentX + contentW,
            contentY + contentH
        );

        for (AbstractWidget widget : sectionWidgets) {
            if (widget.visible) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        for (StoredText st : storedTexts) {
            if (st.isLabel()) {
                drawLabel(
                    graphics,
                    Component.translatable(st.translationKey()),
                    st.x(),
                    st.y()
                );
            } else {
                drawDescription(
                    graphics,
                    st.translationKey(),
                    st.x(),
                    st.y(),
                    st.maxWidth()
                );
            }
        }

        graphics.disableScissor();

        if (active != null && getFilteredRows(active).isEmpty() && !searchQuery.isEmpty()) {
            graphics.drawString(
                this.font,
                Component.translatable("acquiredutils.gui.search.no_results"),
                contentX + s(16),
                contentY + s(46),
                theme.credit,
                false
            );
        }

        for (TabPos tab : tabPositions) {
            Identifier icon = switch (tab.id()) {
                case "general" -> ICON_GEAR;
                case "keybinds" -> ICON_KEYBOARD;
                default -> ICON_MOD_SMALL;
            };

            int iconSize = Math.min(s(12), tab.h() - s(6));
            int iconX = tab.x() + s(6);
            int iconY = tab.y() + (tab.h() - iconSize) / 2;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                iconX, iconY,
                0.0f, 0.0f,
                iconSize, iconSize,
                16,
                16
            );

            if (tab.id().equals(activeSectionId)) {
                int edgeW = Math.max(1, s(3));
                graphics.fill(
                    tab.x(), tab.y(), tab.x() + edgeW, tab.y() + tab.h(),
                    theme.accentBright
                );
            }
        }

        for (AbstractWidget w : sectionWidgets) {
            if (w instanceof DropdownWidget d && d.isOpen()) {
                d.renderOverlay(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
                );
            }
        }
    }

    private void drawScrollbars(
        GuiGraphics graphics,
        Theme theme,
        int contentX,
        int contentY,
        int contentW,
        int contentH
    ) {
        int barWidth = Math.max(2, s(3));

        if (maxContentScroll > 0) {
            int gutter = s(12);
            int trackX = contentX + contentW - gutter / 2 - barWidth / 2;
            int trackY = contentY + s(28);
            int trackH = contentH - s(34);

            graphics.fill(
                trackX,
                trackY,
                trackX + barWidth,
                trackY + trackH,
                theme.sliderTrack
            );

            int thumbH = Math.max(
                s(22),
                (int) ((trackH * (double) trackH) / (trackH + maxContentScroll))
            );
            int travel = Math.max(0, trackH - thumbH);
            int thumbY = trackY + (int) (travel *
                (contentScroll / (double) maxContentScroll));

            graphics.fill(
                trackX - 1,
                thumbY,
                trackX + barWidth + 1,
                thumbY + thumbH,
                theme.accent
            );
        }

        if (maxSidebarScroll > 0) {
            int trackX = panelX + sidebarWidth - s(4);
            int trackY = panelY + headerHeight + padding;
            int trackH = panelHeight - headerHeight - footerHeight - padding * 2;

            graphics.fill(
                trackX,
                trackY,
                trackX + barWidth,
                trackY + trackH,
                theme.sliderTrack
            );

            int thumbH = Math.max(
                s(18),
                (int) ((trackH * (double) trackH) / (trackH + maxSidebarScroll))
            );
            int travel = Math.max(0, trackH - thumbH);
            int thumbY = trackY + (int) (travel *
                (sidebarScroll / (double) maxSidebarScroll));

            graphics.fill(
                trackX - 1,
                thumbY,
                trackX + barWidth + 1,
                thumbY + thumbH,
                theme.accent
            );
        }
    }

    private void drawRowCard(GuiGraphics graphics, RowCard card, Theme theme) {
        int inset = Math.max(1, s(1));

        graphics.fill(
            card.x(), card.y(),
            card.x() + card.w(), card.y() + card.h(),
            0xA0181220
        );

        graphics.renderOutline(
            card.x(), card.y(), card.w(), card.h(), theme.frameMid
        );

        graphics.fill(
            card.x() + inset, card.y() + inset,
            card.x() + s(3), card.y() + card.h() - inset,
            theme.accent
        );
    }

    private void drawPanelChrome(GuiGraphics graphics, Theme theme) {
        int ft = Math.max(1, s(4));
        int shadowOffset = Math.max(1, s(3));

        graphics.fill(
            panelX - ft + shadowOffset,
            panelY - ft + shadowOffset,
            panelX + panelWidth + ft + shadowOffset,
            panelY + panelHeight + ft + shadowOffset,
            theme.shadow
        );

        graphics.fillGradient(
            panelX,
            panelY,
            panelX + panelWidth,
            panelY + panelHeight,
            theme.panelTop,
            theme.panelBottom
        );

        graphics.renderOutline(
            panelX - ft,
            panelY - ft,
            panelWidth + ft * 2,
            panelHeight + ft * 2,
            theme.frameOuter
        );

        graphics.renderOutline(
            panelX - ft + 1,
            panelY - ft + 1,
            panelWidth + ft * 2 - 2,
            panelHeight + ft * 2 - 2,
            theme.frameMid
        );

        graphics.renderOutline(
            panelX - 1,
            panelY - 1,
            panelWidth + 2,
            panelHeight + 2,
            theme.frameAccent
        );

        drawCornerAccents(
            graphics,
            panelX - ft,
            panelY - ft,
            panelWidth + ft * 2,
            panelHeight + ft * 2,
            theme
        );

        graphics.fillGradient(
            panelX,
            panelY,
            panelX + panelWidth,
            panelY + headerHeight,
            theme.headerTop,
            theme.headerBottom
        );

        graphics.fillGradient(
            panelX,
            panelY + headerHeight,
            panelX + sidebarWidth,
            panelY + panelHeight - footerHeight,
            theme.sidebarTop,
            theme.sidebarBottom
        );

        graphics.fillGradient(
            panelX,
            panelY + panelHeight - footerHeight,
            panelX + panelWidth,
            panelY + panelHeight,
            theme.footerTop,
            theme.footerBottom
        );
    }

    private void drawCornerAccents(
        GuiGraphics graphics,
        int x,
        int y,
        int w,
        int h,
        Theme theme
    ) {
        int len = Math.max(2, s(9));
        int thick = Math.max(1, s(2));

        graphics.fill(x, y, x + len, y + thick, theme.accentBright);
        graphics.fill(x, y, x + thick, y + len, theme.accentBright);

        graphics.fill(
            x + w - len,
            y,
            x + w,
            y + thick,
            theme.accentBright
        );

        graphics.fill(
            x + w - thick,
            y,
            x + w,
            y + len,
            theme.accentBright
        );

        graphics.fill(
            x,
            y + h - thick,
            x + len,
            y + h,
            theme.accentBright
        );

        graphics.fill(
            x,
            y + h - len,
            x + thick,
            y + h,
            theme.accentBright
        );

        graphics.fill(
            x + w - len,
            y + h - thick,
            x + w,
            y + h,
            theme.accentBright
        );

        graphics.fill(
            x + w - thick,
            y + h - len,
            x + w,
            y + h,
            theme.accentBright
        );
    }

    public void drawDescription(
        GuiGraphics graphics,
        String translationKey,
        int x,
        int y,
        int maxWidth
    ) {
        Component desc = Component.translatable(translationKey)
            .copy();

        int safeWidth = Math.max(80, maxWidth);
        String value = desc.getString();
        Component display = desc;

        while (value.length() > 1 && this.font.width(display) > safeWidth) {
            value = value.substring(0, value.length() - 1).stripTrailing();
            display = Component.literal(value + "...")
                .copy();
        }

        graphics.drawString(
            this.font,
            display,
            x,
            y,
            0xFFD3C9DE,
            false
        );
    }

    private void drawLabel(
        GuiGraphics graphics,
        Component label,
        int x,
        int y
    ) {
        Component styled = label.copy().withStyle(
            Style.EMPTY.withBold(true)
        );

        graphics.drawString(
            this.font,
            styled,
            x,
            y,
            Theme.current().text,
            false
        );
    }

    @Override
    public boolean mouseClicked(
        MouseButtonEvent event,
        boolean doubleClick
    ) {
        double mouseX = event.x();
        double mouseY = event.y();

        for (AbstractWidget w : sectionWidgets) {
            if (w instanceof DropdownWidget d && d.isOpen()) {
                if (!d.isMouseOver(mouseX, mouseY)
                    && !d.isOverExpandedArea(mouseX, mouseY)) {

                    d.setOpen(false);
                }
            }
        }

        ModSection active = sections.get(activeSectionId);

        if (active != null
            && active.mouseClicked(mouseX, mouseY, event.button())) {

            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY
    ) {
        int contentX = panelX + sidebarWidth + padding;
        int contentY = panelY + headerHeight + padding;
        int contentW = panelWidth - sidebarWidth - padding * 2;
        int contentH = panelHeight - headerHeight - footerHeight - padding * 2;

        boolean insideContent = mouseX >= contentX
            && mouseX < contentX + contentW
            && mouseY >= contentY
            && mouseY < contentY + contentH;

        boolean insideSidebar = mouseX >= panelX + padding
            && mouseX < panelX + sidebarWidth - padding
            && mouseY >= panelY + headerHeight + padding
            && mouseY < panelY + panelHeight - footerHeight - padding;

        int steps = (int) Math.signum(scrollY);

        if (insideContent && maxContentScroll > 0 && steps != 0) {
            int delta = Math.max(s(20), Math.abs(steps) * s(30));
            contentScroll = Math.max(0, Math.min(maxContentScroll, contentScroll - (scrollY > 0 ? delta : -delta)));
            init();
            return true;
        }

        if (insideSidebar && maxSidebarScroll > 0 && steps != 0) {
            int delta = Math.max(s(20), Math.abs(steps) * s(30));
            sidebarScroll = Math.max(0, Math.min(maxSidebarScroll, sidebarScroll - (scrollY > 0 ? delta : -delta)));
            init();
            return true;
        }

        ModSection active = sections.get(activeSectionId);
        if (active != null && active.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        ModSection active = sections.get(activeSectionId);

        if (active != null
            && active.keyPressed(
                event.key(),
                event.scancode(),
                event.modifiers()
            )) {

            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        for (ModSection section : sections.values()) {
            section.onClose();
        }

        AcquiredUtilsConfig.saveIfDirty();

        AcquiredUtils.LOGGER.info(
            "[AcquiredUtils] Settings saved on menu close"
        );

        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}