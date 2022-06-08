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

import net.fabricmc.loader.api.FabricLoader;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;

public class LibNetworkStack implements ModInitializer {

    public static final String MODID = "libnetworkstack";
    public static final boolean DEBUG;
    public static final Logger LOGGER = LogManager.getLogger("LibNetworkStack");

    public static final String CONFIG_FILE_LOCATION;
    public static final boolean CONFIG_RECORD_TYPES;
    public static final boolean CONFIG_RECORD_STACKTRACES;

    static {
        boolean debug = Boolean.getBoolean("libnetworkstack.debug");

        FabricLoader fabric = FabricLoader.getInstance();
        final File cfgDir;
        if (fabric.getGameDir() == null) {
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
        boolean didFileExist = cfgFile.exists();
        if (didFileExist) {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(cfgFile), StandardCharsets.UTF_8)) {
                props.load(isr);
            } catch (IOException e) {
                LOGGER.error("[config] Failed to read the config file!", e);
            }
        }

        boolean hasAll = true;

        boolean forceEnabled = FabricLoader.getInstance().isModLoaded("network-drain-cleaner");

        hasAll &= props.containsKey("debug.log");
        debug |= "true".equalsIgnoreCase(props.getProperty("debug.log", "false"));

        DEBUG = debug;

        hasAll &= props.containsKey("debug.record_types");
        CONFIG_RECORD_TYPES = forceEnabled || "true".equalsIgnoreCase(props.getProperty("debug.record_types", "false"));

        hasAll &= props.containsKey("debug.record_stacktraces");
        CONFIG_RECORD_STACKTRACES
            = forceEnabled || "true".equalsIgnoreCase(props.getProperty("debug.record_stacktraces", "false"));

        if (!hasAll) {
            try (Writer fw = new OutputStreamWriter(new FileOutputStream(cfgFile, true), StandardCharsets.UTF_8)) {
                if (!didFileExist) {
                    fw.append("# LibNetworkStack configuration file.\n");
                    fw.append("# Removing an option will reset it back to the default value.\n");
                    fw.append("# Removing or altering comments doesn't replace them.\n\n");
                }

                if (!props.containsKey("debug.log")) {
                    fw.append("# True to enable all debug logging.\n");
                    fw.append("debug.log=false\n\n");
                }

                if (!props.containsKey("debug.record_types")) {
                    fw.append("# True to enable recording type information when writing packets\n");
                    fw.append("# (which is used when an exception is thrown while reading packets)\n");
                    fw.append("debug.record_types=false\n\n");
                }

                if (!props.containsKey("debug.record_stacktraces")) {
                    fw.append("# True to enable recording stacktraces when writing packets\n");
                    fw.append("# (which is used when an exception is thrown while reading packets)\n");
                    fw.append("# Unlike 'debug.record_types' this adds quite a lot of overhead,\n");
                    fw.append("# so should only be used when absolutely necessary.\n");
                    fw.append("debug.record_stacktraces=false\n\n");
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
