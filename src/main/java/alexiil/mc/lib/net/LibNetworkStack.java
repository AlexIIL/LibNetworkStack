/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;

public class LibNetworkStack implements ModInitializer {

    public static final String MODID = "libnetworkstack";
    public static final boolean DEBUG;
    public static final Logger LOGGER = LogManager.getLogger("LibNetworkStack");

    public static final String CONFIG_FILE_LOCATION;
    public static final boolean CONFIG_RECORD_TYPES;

    static {
        boolean debug = Boolean.getBoolean("libnetworkstack.debug");

        FabricLoader fabric = FabricLoader.getInstance();
        final File cfgDir;
        if (fabric.getGameDirectory() == null) {
            // Can happen during a JUnit test
            cfgDir = new File("config");
        } else {
            cfgDir = fabric.getConfigDirectory();
        }
        if (!cfgDir.isDirectory()) {
            cfgDir.mkdirs();
        }
        File cfgFile = new File(cfgDir, MODID + ".txt");
        String fileLoc;
        try {
            fileLoc = cfgFile.getCanonicalPath();
        } catch (IOException io) {
            fileLoc = cfgFile.getAbsolutePath();
            LOGGER.warn("[config] Failed to get the canonical location of " + cfgFile, io);
        }
        CONFIG_FILE_LOCATION = fileLoc;
        Properties props = new Properties();
        boolean loading = cfgFile.exists();
        if (loading) {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(cfgFile), StandardCharsets.UTF_8)) {
                props.load(isr);
            } catch (IOException e) {
                LOGGER.error("[config] Failed to read the config file!", e);
            }
        }

        boolean hasAll = true;

        hasAll &= props.containsKey("debug");
        debug |= "true".equalsIgnoreCase(props.getProperty("debug", "false"));

        DEBUG = debug;

        hasAll &= props.containsKey("debug.record_types");
        CONFIG_RECORD_TYPES = debug | "true".equalsIgnoreCase(props.getProperty("debug.record_types", "false"));

        if (!hasAll) {
            try (Writer fw = new OutputStreamWriter(new FileOutputStream(cfgFile, true), StandardCharsets.UTF_8)) {
                fw.append("# LibBlockAttributes options file (fluids module)\n");
                fw.append("# Removing an option will reset it back to the default value\n");
                fw.append("# Removing or altering comments doesn't replace them.\n\n");

                if (!props.containsKey("debug")) {
                    fw.append("# True to enable all debugging, or false to use the\n");
                    fw.append("# other options for more fine-grained control.\n");
                    fw.append("debug=false\n\n");
                }

                if (!props.containsKey("debug.record_types")) {
                    fw.append("# True to enable recording type information when writing packets\n");
                    fw.append("# (which is used when an exception is thrown while reading packets)\n");
                    fw.append("debug.record_types=false\n\n");
                }

            } catch (IOException e) {
                LOGGER.warn("[config] Failed to write the config file!", e);
            }
        }
    }

    @Override
    public void onInitialize() {
        CoreMinecraftNetUtil.load();
    }
}
