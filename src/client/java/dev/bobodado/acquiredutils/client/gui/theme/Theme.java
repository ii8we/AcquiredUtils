package dev.bobodado.acquiredutils.client.gui.theme;

public enum Theme {
    PURPLE(
        0xFF2A1738, 0xFF0E0914,
        0xFF6A4A72, 0xFF8E6AA3, 0xFFD9A441,
        0xFF24142F, 0xFF120A1B,
        0xFF24122E, 0xFF110918,
        0xFF22122A, 0xFF100914,
        0xFFD4A64D, 0xB9000000, 0x5A3A214D,
        0xFFF2EAF7, 0xFFC47BEE, 0xFFE2B85A, 0xFFD2C7DA,
        0xFF2C1A3A, 0xFF16101F
    );

    public final int panelTop, panelBottom;
    public final int frameOuter, frameMid, frameAccent;
    public final int headerTop, headerBottom;
    public final int sidebarTop, sidebarBottom;
    public final int footerTop, footerBottom;
    public final int divider, shadow, tabActiveBg;
    public final int text, accent, accentBright, credit;
    public final int sliderTrack, buttonBottom;

    Theme(int panelTop, int panelBottom,
          int frameOuter, int frameMid, int frameAccent,
          int headerTop, int headerBottom,
          int sidebarTop, int sidebarBottom,
          int footerTop, int footerBottom,
          int divider, int shadow, int tabActiveBg,
          int text, int accent, int accentBright, int credit,
          int sliderTrack, int buttonBottom) {
        this.panelTop = panelTop;
        this.panelBottom = panelBottom;
        this.frameOuter = frameOuter;
        this.frameMid = frameMid;
        this.frameAccent = frameAccent;
        this.headerTop = headerTop;
        this.headerBottom = headerBottom;
        this.sidebarTop = sidebarTop;
        this.sidebarBottom = sidebarBottom;
        this.footerTop = footerTop;
        this.footerBottom = footerBottom;
        this.divider = divider;
        this.shadow = shadow;
        this.tabActiveBg = tabActiveBg;
        this.text = text;
        this.accent = accent;
        this.accentBright = accentBright;
        this.credit = credit;
        this.sliderTrack = sliderTrack;
        this.buttonBottom = buttonBottom;
    }

    public static Theme current() {
        return PURPLE;
    }
}
