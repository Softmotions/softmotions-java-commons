package com.softmotions.kotlin;

import org.apache.commons.configuration2.AbstractConfiguration;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class AbstractConfigurationKtAdapter {

    private AbstractConfigurationKtAdapter() {
    }

    public static boolean getBoolean(AbstractConfiguration c, String name, boolean def) {
        return c.getBoolean(name, def);
    }

    public static int getInt(AbstractConfiguration c, String name, int def) {
        return c.getInt(name, def);
    }

    public static long getLong(AbstractConfiguration c, String name, long def) {
        return c.getLong(name, def);
    }

    public static short getShort(AbstractConfiguration c, String name, short def) {
        return c.getShort(name, def);
    }

    public static byte getByte(AbstractConfiguration c, String name, byte def) {
        return c.getByte(name, def);
    }

    public static float getFloat(AbstractConfiguration c, String name, float def) {
        return c.getFloat(name, def);
    }

    public static double getDouble(AbstractConfiguration c, String name, double def) {
        return c.getDouble(name, def);
    }
}
