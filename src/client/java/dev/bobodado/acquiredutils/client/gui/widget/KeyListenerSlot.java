package dev.bobodado.acquiredutils.client.gui.widget;

/**
 * Shared "who's currently armed to receive the next key press" slot.
 * <p>
 * On this screen, keyPressed() is only ever routed to the active ModSection
 * (see AcquiredUtilsConfigScreen), never to individual widgets. So any
 * widget that needs to capture a key press (LockedKeybindWidget,
 * CustomKeybindRowWidget) arms itself into one of these on click, and the
 * owning section's own keyPressed(KeyEvent) override forwards the key to
 * whichever widget last armed itself.
 * <p>
 * One section = one slot, shared by every keybind-capturing widget it hosts.
 */
public class KeyListenerSlot {

	public interface Listener {
		void applyKeyCode(int keyCode);
	}

	public Listener current;

	public boolean isListening(Listener candidate) {
		return current == candidate;
	}

	public void clear() {
		current = null;
	}
}