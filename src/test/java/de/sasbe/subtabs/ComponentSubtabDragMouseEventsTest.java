package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ComponentSubtabDragMouseEventsTest {
    @Test
    void keepsEventWhenAlreadyTargeted() {
        JPanel target = new JPanel();
        MouseEvent event = new MouseEvent(
                target,
                MouseEvent.MOUSE_DRAGGED,
                0L,
                0,
                12,
                18,
                1,
                false,
                MouseEvent.BUTTON1
        );

        assertSame(event, ComponentSubtabDragMouseEvents.retarget(event, target));
    }

    @Test
    void convertsCoordinatesToEditorComponent() {
        JPanel parent = new JPanel(null);
        parent.setBounds(0, 0, 400, 400);

        JPanel glass = new JPanel();
        glass.setBounds(0, 0, 400, 400);

        JPanel editor = new JPanel();
        editor.setBounds(50, 80, 200, 100);

        parent.add(editor);
        parent.add(glass);

        MouseEvent event = new MouseEvent(
                glass,
                MouseEvent.MOUSE_DRAGGED,
                0L,
                0,
                70,
                90,
                1,
                false,
                MouseEvent.BUTTON1
        );

        MouseEvent retargeted = ComponentSubtabDragMouseEvents.retarget(event, editor);

        assertSame(editor, retargeted.getComponent());
        assertEquals(MouseEvent.MOUSE_DRAGGED, retargeted.getID());
        assertEquals(20, retargeted.getX());
        assertEquals(10, retargeted.getY());
        assertEquals(MouseEvent.BUTTON1, retargeted.getButton());
    }

    @Test
    void detachedComponentsAreNotUsableForDevicePoint() {
        JPanel detached = new JPanel();

        assertFalse(ComponentSubtabDragMouseEvents.isUsableForDevicePoint(null));
        assertFalse(ComponentSubtabDragMouseEvents.isUsableForDevicePoint(detached));
    }

    @Test
    void dockSessionKeepsOriginalEventWhenNothingIsShowing() {
        JPanel detached = new JPanel();
        MouseEvent event = new MouseEvent(
                detached,
                MouseEvent.MOUSE_RELEASED,
                0L,
                0,
                4,
                6,
                1,
                false,
                MouseEvent.BUTTON1
        );

        MouseEvent dockEvent = ComponentSubtabDragMouseEvents.forDockSession(event, new JPanel());

        assertSame(event, dockEvent);
    }

    @Test
    void firstUsableFallsBackToPrimaryWhenNothingIsDisplayable() {
        JPanel primary = new JPanel();
        JPanel fallback = new JPanel();

        assertSame(primary, ComponentSubtabDragMouseEvents.firstUsableForDevicePoint(primary, fallback));
    }

    @Test
    void repaintDragSurfacesDoesNotThrowForDetachedComponents() {
        ComponentSubtabDragMouseEvents.repaintDragSurfaces(null, new JPanel());
    }
}
