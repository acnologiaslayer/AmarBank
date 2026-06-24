package com.amarbank.gui;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.function.Consumer;

/**
 * An {@link EventQueue} that funnels any uncaught exception thrown while
 * dispatching AWT/Swing events to a single handler.
 *
 * Swing paints and event handling run on the Event Dispatch Thread (EDT).
 * If an exception escapes there (for example a buggy Look&Feel throwing a
 * NullPointerException during repaint), the default behaviour is to print
 * a stack trace and leave the UI in a broken, repeatedly-failing state.
 *
 * Installing this queue lets the application observe those errors, show a
 * proper message, and recover instead of crashing.
 */
public class SafeEventQueue extends EventQueue {

    private final Consumer<Throwable> handler;

    private SafeEventQueue(Consumer<Throwable> handler) {
        this.handler = handler;
    }

    /** Installs the guard on the system event queue (call on the EDT). */
    public static void install(Consumer<Throwable> handler) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new SafeEventQueue(handler));
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            super.dispatchEvent(event);
        } catch (Throwable t) {
            handler.accept(t);
        }
    }
}
