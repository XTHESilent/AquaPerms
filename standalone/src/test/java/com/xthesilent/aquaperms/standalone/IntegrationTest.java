/*
 * This file is part of AquaPerms, licensed under the MIT License.
 *
 *  Copyright (c) AquasplashMC (XTHESilent) <xthesilent@aquasplashmc.com>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.xthesilent.aquaperms.standalone;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.node.types.Permission;
import com.xthesilent.aquaperms.standalone.app.integration.CommandExecutor;
import com.xthesilent.aquaperms.standalone.utils.TestPluginProvider;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.NodeEqualityPredicate;
import com.aquasplashmc.api.platform.Health;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A set of 'integration tests' for the standalone AquaPerms app.
 */
public class IntegrationTest {

    @Test
    public void testLoadEnableDisable(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            Health health = plugin.runHealthCheck();
            assertNotNull(health);
            assertTrue(health.isHealthy());
        });
    }

    @Test
    public void testRunCommand(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor commandExecutor = app.getCommandExecutor();
            commandExecutor.execute("group default permission set test").join();

            Group group = bootstrap.getPlugin().getStorage().loadGroup("default").join().orElse(null);
            assertNotNull(group);
            assertTrue(group.hasNode(DataType.NORMAL, Permission.builder().permission("test").build(), NodeEqualityPredicate.EXACT).asBoolean());
        });
    }

    @Test
    public void testReloadConfig(@TempDir Path tempDir) throws IOException {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            String server = plugin.getConfiguration().get(ConfigKeys.SERVER);
            assertEquals("global", server);

            Integer syncTime = plugin.getConfiguration().get(ConfigKeys.SYNC_TIME);
            assertEquals(-1, syncTime);

            Path config = tempDir.resolve("config.yml");
            assertTrue(Files.exists(config));

            String configString = Files.readString(config)
                    .replace("server: global", "server: test")
                    .replace("sync-minutes: -1", "sync-minutes: 10");
            Files.writeString(config, configString);

            plugin.getConfiguration().reload();

            server = plugin.getConfiguration().get(ConfigKeys.SERVER);
            assertEquals("test", server); // changed

            syncTime = plugin.getConfiguration().get(ConfigKeys.SYNC_TIME);
            assertEquals(-1, syncTime); // unchanged
        });
    }

}
