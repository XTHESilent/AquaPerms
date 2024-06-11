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

import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.standalone.app.integration.CommandExecutor;
import com.xthesilent.aquaperms.standalone.app.integration.SingletonPlayer;
import com.xthesilent.aquaperms.standalone.utils.CommandTester;
import com.xthesilent.aquaperms.standalone.utils.TestPluginProvider;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.aquasplashmc.api.event.log.LogNotifyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandsIntegrationTest {
    
    private static final Map<String, String> CONFIG = ImmutableMap.<String, String>builder()
            .put("log-notify", "false")
            .build();

    @Test
    public void testGroupCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.creategroup")
                    .whenRunCommand("creategroup test")
                    .thenExpect("[AP] test was successfully created.")

                    .givenHasPermissions("aquaperms.creategroup")
                    .whenRunCommand("creategroup test2")
                    .thenExpect("[AP] test2 was successfully created.")

                    .givenHasPermissions("aquaperms.deletegroup")
                    .whenRunCommand("deletegroup test2")
                    .thenExpect("[AP] test2 was successfully deleted.")

                    .givenHasPermissions("aquaperms.listgroups")
                    .whenRunCommand("listgroups")
                    .thenExpect("""
                            [AP] Showing group entries:    (page 1 of 1 - 2 entries)
                            [AP] Groups: (name, weight, tracks)
                            [AP] -  default - 0
                            [AP] -  test - 0
                            """
                    )

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group test info")
                    .thenExpect("""
                            [AP] > Group Info: test
                            [AP] - Display Name: test
                            [AP] - Weight: None
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Meta: None
                            """
                    )

                    .givenHasAllPermissions()
                    .whenRunCommand("group test meta set hello world")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.group.setweight")
                    .whenRunCommand("group test setweight 10")
                    .thenExpect("[AP] Set weight to 10 for group test.")

                    .givenHasPermissions("aquaperms.group.setweight")
                    .whenRunCommand("group test setweight 100")
                    .thenExpect("[AP] Set weight to 100 for group test.")

                    .givenHasPermissions("aquaperms.group.setdisplayname")
                    .whenRunCommand("group test setdisplayname Test")
                    .thenExpect("[AP] Set display name to Test for group test in context global.")

                    .givenHasPermissions("aquaperms.group.setdisplayname")
                    .whenRunCommand("group test setdisplayname Dummy")
                    .thenExpect("[AP] Set display name to Dummy for group test in context global.")

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group Dummy info")
                    .thenExpect("""
                            [AP] > Group Info: test
                            [AP] - Display Name: Dummy
                            [AP] - Weight: 100
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("aquaperms.group.clone")
                    .whenRunCommand("group test clone testclone")
                    .thenExpect("[AP] test (Dummy) was successfully cloned onto testclone (Dummy).")

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group testclone info")
                    .thenExpect("""
                            [AP] > Group Info: testclone
                            [AP] - Display Name: Dummy
                            [AP] - Weight: 100
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("aquaperms.group.rename")
                    .whenRunCommand("group test rename test2")
                    .thenExpect("[AP] test (Dummy) was successfully renamed to test2 (Dummy).")

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group test2 info")
                    .thenExpect("""
                            [AP] > Group Info: test2
                            [AP] - Display Name: Dummy
                            [AP] - Weight: 100
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("aquaperms.listgroups")
                    .whenRunCommand("listgroups")
                    .thenExpect("""
                            [AP] Showing group entries:    (page 1 of 1 - 3 entries)
                            [AP] Groups: (name, weight, tracks)
                            [AP] -  test2 (Dummy) - 100
                            [AP] -  testclone (Dummy) - 100
                            [AP] -  default - 0
                            """
                    );
        });
    }

    @Test
    public void testGroupPermissionCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node true")
                    .thenExpect("[AP] Set test.node to true for test in context global.")

                    .givenHasPermissions("aquaperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node.other false server=test")
                    .thenExpect("[AP] Set test.node.other to false for test in context server=test.")

                    .givenHasPermissions("aquaperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node.other false server=test world=test2")
                    .thenExpect("[AP] Set test.node.other to false for test in context server=test, world=test2.")

                    .givenHasPermissions("aquaperms.group.permission.settemp")
                    .whenRunCommand("group test permission settemp abc true 1h")
                    .thenExpect("[AP] Set abc to true for test for a duration of 1 hour in context global.")

                    .givenHasPermissions("aquaperms.group.permission.settemp")
                    .whenRunCommand("group test permission settemp abc true 2h replace")
                    .thenExpect("[AP] Set abc to true for test for a duration of 2 hours in context global.")

                    .givenHasPermissions("aquaperms.group.permission.unsettemp")
                    .whenRunCommand("group test permission unsettemp abc")
                    .thenExpect("[AP] Unset temporary permission abc for test in context global.")

                    .givenHasPermissions("aquaperms.group.permission.info")
                    .whenRunCommand("group test permission info")
                    .thenExpect("""
                            [AP] test's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenHasPermissions("aquaperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node")
                    .thenExpect("[AP] Unset test.node for test in context global.")

                    .givenHasPermissions("aquaperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node.other")
                    .thenExpect("[AP] test does not have test.node.other set in context global.")

                    .givenHasPermissions("aquaperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node.other server=test")
                    .thenExpect("[AP] Unset test.node.other for test in context server=test.")

                    .givenHasPermissions("aquaperms.group.permission.check")
                    .whenRunCommand("group test permission check test.node.other")
                    .thenExpect("""
                            [AP] Permission information for test.node.other:
                            [AP] - test has test.node.other set to false in context server=test, world=test2.
                            [AP] - test does not inherit test.node.other.
                            [AP]
                            [AP] Permission check for test.node.other:
                            [AP]     Result: undefined
                            [AP]     Processor: None
                            [AP]     Cause: None
                            [AP]     Context: None
                            """
                    )

                    .givenHasPermissions("aquaperms.group.permission.clear")
                    .whenRunCommand("group test permission clear server=test world=test2")
                    .thenExpect("[AP] test's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenHasPermissions("aquaperms.group.permission.info")
                    .whenRunCommand("group test permission info")
                    .thenExpect("[AP] test does not have any permissions set.");
        });
    }

    @Test
    public void testGroupParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .whenRunCommand("creategroup test2")
                    .whenRunCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.group.parent.add")
                    .whenRunCommand("group test parent add default")
                    .thenExpect("[AP] test now inherits permissions from default in context global.")

                    .givenHasPermissions("aquaperms.group.parent.add")
                    .whenRunCommand("group test parent add test2 server=test")
                    .thenExpect("[AP] test now inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("aquaperms.group.parent.add")
                    .whenRunCommand("group test parent add test3 server=test")
                    .thenExpect("[AP] test now inherits permissions from test3 in context server=test.")

                    .givenHasPermissions("aquaperms.group.parent.addtemp")
                    .whenRunCommand("group test parent addtemp test2 1d server=hello")
                    .thenExpect("[AP] test now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.group.parent.removetemp")
                    .whenRunCommand("group test parent removetemp test2 server=hello")
                    .thenExpect("[AP] test no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenHasPermissions("aquaperms.group.parent.info")
                    .whenRunCommand("group test parent info")
                    .thenExpect("""
                            [AP] test's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenHasPermissions("aquaperms.group.parent.set")
                    .whenRunCommand("group test parent set test2 server=test")
                    .thenExpect("[AP] test had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenHasPermissions("aquaperms.group.parent.remove")
                    .whenRunCommand("group test parent remove test2 server=test")
                    .thenExpect("[AP] test no longer inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("aquaperms.group.parent.clear")
                    .whenRunCommand("group test parent clear")
                    .thenExpect("[AP] test's parents were cleared in context global. (1 node was removed.)")

                    .givenHasPermissions("aquaperms.group.parent.info")
                    .whenRunCommand("group test parent info")
                    .thenExpect("[AP] test does not have any parents defined.");
        });
    }

    @Test
    public void testGroupMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [AP] test has no prefixes.
                            [AP] test has no suffixes.
                            [AP] test has no meta.
                            """
                    )

                    .givenHasPermissions("aquaperms.group.meta.set")
                    .whenRunCommand("group test meta set hello world")
                    .thenExpect("[AP] Set meta key 'hello' to 'world' for test in context global.")


                    .givenHasPermissions("aquaperms.group.meta.set")
                    .whenRunCommand("group test meta set hello world2 server=test")
                    .thenExpect("[AP] Set meta key 'hello' to 'world2' for test in context server=test.")

                    .givenHasPermissions("aquaperms.group.meta.addprefix")
                    .whenRunCommand("group test meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[AP] test had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenHasPermissions("aquaperms.group.meta.addsuffix")
                    .whenRunCommand("group test meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[AP] test had suffix 'hi' set at a priority of 100 in context global.")

                    .givenHasPermissions("aquaperms.group.meta.addsuffix")
                    .whenRunCommand("group test meta addsuffix 1 \"&6no\"")
                    .thenExpect("[AP] test had suffix 'no' set at a priority of 1 in context global.")

                    .givenHasPermissions("aquaperms.group.meta.settemp")
                    .whenRunCommand("group test meta settemp abc xyz 1d server=hello")
                    .thenExpect("[AP] Set meta key 'abc' to 'xyz' for test for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.addtempprefix")
                    .whenRunCommand("group test meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[AP] test had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.addtempsuffix")
                    .whenRunCommand("group test meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[AP] test had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.unsettemp")
                    .whenRunCommand("group test meta unsettemp abc server=hello")
                    .thenExpect("[AP] Unset temporary meta key 'abc' for test in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.removetempprefix")
                    .whenRunCommand("group test meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[AP] test had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.removetempsuffix")
                    .whenRunCommand("group test meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[AP] test had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("aquaperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [AP] test's Prefixes
                            [AP] -> 10 - 'hello world' (inherited from self)
                            [AP] test's Suffixes
                            [AP] -> 100 - 'hi' (inherited from self)
                            [AP] -> 1 - 'no' (inherited from self)
                            [AP] test's Meta
                            [AP] -> hello = 'world2' (inherited from self) (server=test)
                            [AP] -> hello = 'world' (inherited from self)
                            """
                    )

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group test info")
                    .thenExpect("""
                            [AP] > Group Info: test
                            [AP] - Display Name: test
                            [AP] - Weight: None
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Prefix: "hello world"
                            [AP]     Suffix: "hi"
                            [AP]     Meta: (hello=world)
                            """
                    )

                    .givenHasPermissions("aquaperms.group.meta.unset")
                    .whenRunCommand("group test meta unset hello")
                    .thenExpect("[AP] Unset meta key 'hello' for test in context global.")

                    .givenHasPermissions("aquaperms.group.meta.unset")
                    .whenRunCommand("group test meta unset hello server=test")
                    .thenExpect("[AP] Unset meta key 'hello' for test in context server=test.")

                    .givenHasPermissions("aquaperms.group.meta.removeprefix")
                    .whenRunCommand("group test meta removeprefix 10")
                    .thenExpect("[AP] test had all prefixes at priority 10 removed in context global.")

                    .givenHasPermissions("aquaperms.group.meta.removesuffix")
                    .whenRunCommand("group test meta removesuffix 100")
                    .thenExpect("[AP] test had all suffixes at priority 100 removed in context global.")

                    .givenHasPermissions("aquaperms.group.meta.removesuffix")
                    .whenRunCommand("group test meta removesuffix 1")
                    .thenExpect("[AP] test had all suffixes at priority 1 removed in context global.")

                    .givenHasPermissions("aquaperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [AP] test has no prefixes.
                            [AP] test has no suffixes.
                            [AP] test has no meta.
                            """
                    );
        });
    }

    @Test
    public void testUserCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();
            plugin.getStorage().savePlayerData(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.user.info")
                    .whenRunCommand("user Luck info")
                    .thenExpect("""
                            [AP] > User Info: luck
                            [AP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [AP]     (type: official)
                            [AP] - Status: Offline
                            [AP] - Parent Groups:
                            [AP]     > default
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Contexts: None
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Primary Group: default
                            [AP]     Meta: (primarygroup=default)
                            """
                    )

                    .givenHasPermissions("aquaperms.user.info")
                    .whenRunCommand("user c1d60c50-70b5-4722-8057-87767557e50d info")
                    .thenExpect("""
                            [AP] > User Info: luck
                            [AP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [AP]     (type: official)
                            [AP] - Status: Offline
                            [AP] - Parent Groups:
                            [AP]     > default
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Contexts: None
                            [AP]     Prefix: None
                            [AP]     Suffix: None
                            [AP]     Primary Group: default
                            [AP]     Meta: (primarygroup=default)
                            """
                    )

                    .givenHasAllPermissions()
                    .whenRunCommand("creategroup admin")
                    .whenRunCommand("user Luck parent set admin")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.user.clone")
                    .whenRunCommand("user Luck clone Notch")
                    .thenExpect("[AP] luck was successfully cloned onto notch.")

                    .givenHasPermissions("aquaperms.user.parent.info")
                    .whenRunCommand("user Notch parent info")
                    .thenExpect("""
                            [AP] notch's Parents:  (page 1 of 1 - 1 entries)
                            > admin
                            """
                    );
        });
    }

    @Test
    public void testUserPermissionCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node true")
                    .thenExpect("[AP] Set test.node to true for luck in context global.")

                    .givenHasPermissions("aquaperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node.other false server=test")
                    .thenExpect("[AP] Set test.node.other to false for luck in context server=test.")

                    .givenHasPermissions("aquaperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node.other false server=test world=test2")
                    .thenExpect("[AP] Set test.node.other to false for luck in context server=test, world=test2.")

                    .givenHasPermissions("aquaperms.user.permission.settemp")
                    .whenRunCommand("user Luck permission settemp abc true 1h")
                    .thenExpect("[AP] Set abc to true for luck for a duration of 1 hour in context global.")

                    .givenHasPermissions("aquaperms.user.permission.settemp")
                    .whenRunCommand("user Luck permission settemp abc true 2h replace")
                    .thenExpect("[AP] Set abc to true for luck for a duration of 2 hours in context global.")

                    .givenHasPermissions("aquaperms.user.permission.unsettemp")
                    .whenRunCommand("user Luck permission unsettemp abc")
                    .thenExpect("[AP] Unset temporary permission abc for luck in context global.")

                    .givenHasPermissions("aquaperms.user.permission.info")
                    .whenRunCommand("user Luck permission info")
                    .thenExpect("""
                            [AP] luck's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenHasPermissions("aquaperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node")
                    .thenExpect("[AP] Unset test.node for luck in context global.")

                    .givenHasPermissions("aquaperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node.other")
                    .thenExpect("[AP] luck does not have test.node.other set in context global.")

                    .givenHasPermissions("aquaperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node.other server=test")
                    .thenExpect("[AP] Unset test.node.other for luck in context server=test.")

                    .givenHasPermissions("aquaperms.user.permission.check")
                    .whenRunCommand("user Luck permission check test.node.other")
                    .thenExpect("""
                            [AP] Permission information for test.node.other:
                            [AP] - luck has test.node.other set to false in context server=test, world=test2.
                            [AP] - luck does not inherit test.node.other.
                            [AP]
                            [AP] Permission check for test.node.other:
                            [AP]     Result: undefined
                            [AP]     Processor: None
                            [AP]     Cause: None
                            [AP]     Context: None
                            """
                    )

                    .givenHasPermissions("aquaperms.user.permission.clear")
                    .whenRunCommand("user Luck permission clear server=test world=test2")
                    .thenExpect("[AP] luck's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenHasPermissions("aquaperms.user.permission.info")
                    .whenRunCommand("user Luck permission info")
                    .thenExpect("[AP] luck does not have any permissions set.");
        });
    }

    @Test
    public void testUserParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test2")
                    .whenRunCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.user.parent.add")
                    .whenRunCommand("user Luck parent add default")
                    .thenExpect("[AP] luck already inherits from default in context global.")

                    .givenHasPermissions("aquaperms.user.parent.add")
                    .whenRunCommand("user Luck parent add test2 server=test")
                    .thenExpect("[AP] luck now inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("aquaperms.user.parent.add")
                    .whenRunCommand("user Luck parent add test3 server=test")
                    .thenExpect("[AP] luck now inherits permissions from test3 in context server=test.")

                    .givenHasPermissions("aquaperms.user.parent.addtemp")
                    .whenRunCommand("user Luck parent addtemp test2 1d server=hello")
                    .thenExpect("[AP] luck now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.user.parent.removetemp")
                    .whenRunCommand("user Luck parent removetemp test2 server=hello")
                    .thenExpect("[AP] luck no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenHasPermissions("aquaperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [AP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenHasPermissions("aquaperms.user.parent.set")
                    .whenRunCommand("user Luck parent set test2 server=test")
                    .thenExpect("[AP] luck had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenHasPermissions("aquaperms.user.parent.remove")
                    .whenRunCommand("user Luck parent remove test2 server=test")
                    .thenExpect("[AP] luck no longer inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("aquaperms.user.parent.clear")
                    .whenRunCommand("user Luck parent clear")
                    .thenExpect("[AP] luck's parents were cleared in context global. (0 nodes were removed.)")

                    .givenHasPermissions("aquaperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [AP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

    @Test
    public void testUserMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [AP] luck has no prefixes.
                            [AP] luck has no suffixes.
                            [AP] luck has no meta.
                            """
                    )

                    .givenHasPermissions("aquaperms.user.meta.set")
                    .whenRunCommand("user Luck meta set hello world")
                    .thenExpect("[AP] Set meta key 'hello' to 'world' for luck in context global.")

                    .givenHasPermissions("aquaperms.user.meta.set")
                    .whenRunCommand("user Luck meta set hello world2 server=test")
                    .thenExpect("[AP] Set meta key 'hello' to 'world2' for luck in context server=test.")

                    .givenHasPermissions("aquaperms.user.meta.addprefix")
                    .whenRunCommand("user Luck meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[AP] luck had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenHasPermissions("aquaperms.user.meta.addsuffix")
                    .whenRunCommand("user Luck meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[AP] luck had suffix 'hi' set at a priority of 100 in context global.")

                    .givenHasPermissions("aquaperms.user.meta.addsuffix")
                    .whenRunCommand("user Luck meta addsuffix 1 \"&6no\"")
                    .thenExpect("[AP] luck had suffix 'no' set at a priority of 1 in context global.")

                    .givenHasPermissions("aquaperms.user.meta.settemp")
                    .whenRunCommand("user Luck meta settemp abc xyz 1d server=hello")
                    .thenExpect("[AP] Set meta key 'abc' to 'xyz' for luck for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.addtempprefix")
                    .whenRunCommand("user Luck meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[AP] luck had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.addtempsuffix")
                    .whenRunCommand("user Luck meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[AP] luck had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.unsettemp")
                    .whenRunCommand("user Luck meta unsettemp abc server=hello")
                    .thenExpect("[AP] Unset temporary meta key 'abc' for luck in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.removetempprefix")
                    .whenRunCommand("user Luck meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[AP] luck had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.removetempsuffix")
                    .whenRunCommand("user Luck meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[AP] luck had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("aquaperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [AP] luck's Prefixes
                            [AP] -> 10 - 'hello world' (inherited from self)
                            [AP] luck's Suffixes
                            [AP] -> 100 - 'hi' (inherited from self)
                            [AP] -> 1 - 'no' (inherited from self)
                            [AP] luck's Meta
                            [AP] -> hello = 'world2' (inherited from self) (server=test)
                            [AP] -> hello = 'world' (inherited from self)
                            """
                    )

                    .givenHasPermissions("aquaperms.user.info")
                    .whenRunCommand("user Luck info")
                    .thenExpect("""
                            [AP] > User Info: luck
                            [AP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [AP]     (type: official)
                            [AP] - Status: Offline
                            [AP] - Parent Groups:
                            [AP]     > default
                            [AP] - Contextual Data: (mode: server)
                            [AP]     Contexts: None
                            [AP]     Prefix: "hello world"
                            [AP]     Suffix: "hi"
                            [AP]     Primary Group: default
                            [AP]     Meta: (hello=world) (primarygroup=default)
                            """
                    )

                    .givenHasPermissions("aquaperms.user.meta.unset")
                    .whenRunCommand("user Luck meta unset hello")
                    .thenExpect("[AP] Unset meta key 'hello' for luck in context global.")

                    .givenHasPermissions("aquaperms.user.meta.unset")
                    .whenRunCommand("user Luck meta unset hello server=test")
                    .thenExpect("[AP] Unset meta key 'hello' for luck in context server=test.")

                    .givenHasPermissions("aquaperms.user.meta.removeprefix")
                    .whenRunCommand("user Luck meta removeprefix 10")
                    .thenExpect("[AP] luck had all prefixes at priority 10 removed in context global.")

                    .givenHasPermissions("aquaperms.user.meta.removesuffix")
                    .whenRunCommand("user Luck meta removesuffix 100")
                    .thenExpect("[AP] luck had all suffixes at priority 100 removed in context global.")

                    .givenHasPermissions("aquaperms.user.meta.removesuffix")
                    .whenRunCommand("user Luck meta removesuffix 1")
                    .thenExpect("[AP] luck had all suffixes at priority 1 removed in context global.")

                    .givenHasPermissions("aquaperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [AP] luck has no prefixes.
                            [AP] luck has no suffixes.
                            [AP] luck has no meta.
                            """
                    );
        });
    }

    @Test
    public void testTrackCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.createtrack")
                    .whenRunCommand("createtrack test1")
                    .thenExpect("[AP] test1 was successfully created.")

                    .givenHasPermissions("aquaperms.createtrack")
                    .whenRunCommand("createtrack test2")
                    .thenExpect("[AP] test2 was successfully created.")

                    .givenHasPermissions("aquaperms.listtracks")
                    .whenRunCommand("listtracks")
                    .thenExpect("[AP] Tracks: test1, test2")

                    .givenHasPermissions("aquaperms.deletetrack")
                    .whenRunCommand("deletetrack test2")
                    .thenExpect("[AP] test2 was successfully deleted.")

                    .givenHasAllPermissions()
                    .whenRunCommand("creategroup aaa")
                    .whenRunCommand("creategroup bbb")
                    .whenRunCommand("creategroup ccc")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.track.append")
                    .whenRunCommand("track test1 append bbb")
                    .thenExpect("[AP] Group bbb was appended to track test1.")

                    .givenHasPermissions("aquaperms.track.insert")
                    .whenRunCommand("track test1 insert aaa 1")
                    .thenExpect("""
                            [AP] Group aaa was inserted into track test1 at position 1.
                            [AP] aaa ---> bbb
                            """
                    )

                    .givenHasPermissions("aquaperms.track.insert")
                    .whenRunCommand("track test1 insert ccc 3")
                    .thenExpect("""
                            [AP] Group ccc was inserted into track test1 at position 3.
                            [AP] aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("aquaperms.track.info")
                    .whenRunCommand("track test1 info")
                    .thenExpect("""
                            [AP] > Showing Track: test1
                            [AP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("aquaperms.track.clone")
                    .whenRunCommand("track test1 clone testclone")
                    .thenExpect("[AP] test1 was successfully cloned onto testclone.")

                    .givenHasPermissions("aquaperms.track.info")
                    .whenRunCommand("track testclone info")
                    .thenExpect("""
                            [AP] > Showing Track: testclone
                            [AP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("aquaperms.track.rename")
                    .whenRunCommand("track test1 rename test2")
                    .thenExpect("[AP] test1 was successfully renamed to test2.")

                    .givenHasPermissions("aquaperms.listtracks")
                    .whenRunCommand("listtracks")
                    .thenExpect("[AP] Tracks: test2, testclone")

                    .givenHasPermissions("aquaperms.track.info")
                    .whenRunCommand("track test2 info")
                    .thenExpect("""
                            [AP] > Showing Track: test2
                            [AP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("aquaperms.group.showtracks")
                    .whenRunCommand("group aaa showtracks")
                    .thenExpect("""
                            [AP] aaa's Tracks:
                            > test2:
                            (aaa ---> bbb ---> ccc)
                            > testclone:
                            (aaa ---> bbb ---> ccc)
                            """
                    )

                    .givenHasPermissions("aquaperms.track.remove")
                    .whenRunCommand("track test2 remove bbb")
                    .thenExpect("""
                            [AP] Group bbb was removed from track test2.
                            [AP] aaa ---> ccc
                            """
                    )

                    .givenHasPermissions("aquaperms.track.clear")
                    .whenRunCommand("track test2 clear")
                    .thenExpect("[AP] test2's groups track was cleared.");
        });
    }

    @Test
    public void testUserTrackCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("createtrack staff")
                    .whenRunCommand("createtrack premium")

                    .whenRunCommand("creategroup mod")
                    .whenRunCommand("creategroup admin")

                    .whenRunCommand("creategroup vip")
                    .whenRunCommand("creategroup vip+")
                    .whenRunCommand("creategroup mvp")
                    .whenRunCommand("creategroup mvp+")

                    .whenRunCommand("track staff append mod")
                    .whenRunCommand("track staff append admin")
                    .whenRunCommand("track premium append vip")
                    .whenRunCommand("track premium append vip+")
                    .whenRunCommand("track premium append mvp")
                    .whenRunCommand("track premium append mvp+")

                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("[AP] luck isn't in any groups on staff, so they were added to the first group, mod in context global.")

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("""
                            [AP] Promoting luck along track staff from mod to admin in context global.
                            [AP] mod ---> admin
                            """
                    )

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("[AP] The end of track staff was reached, unable to promote luck.")

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("""
                            [AP] Demoting luck along track staff from admin to mod in context global.
                            [AP] mod <--- admin
                            """
                    )

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("[AP] The end of track staff was reached, so luck was removed from mod.")

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("[AP] luck isn't already in any groups on staff.")

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test1")
                    .thenExpect("[AP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test1.")

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test2")
                    .thenExpect("[AP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test2.")

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test1")
                    .thenExpect("""
                            [AP] Promoting luck along track premium from vip to vip+ in context server=test1.
                            [AP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenHasPermissions("aquaperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test2")
                    .thenExpect("""
                            [AP] Promoting luck along track premium from vip to vip+ in context server=test2.
                            [AP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenHasPermissions("aquaperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [AP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > vip+ (server=test2)
                            > vip+ (server=test1)
                            > default
                            """
                    )

                    .givenHasPermissions("aquaperms.user.showtracks")
                    .whenRunCommand("user Luck showtracks")
                    .thenExpect("""
                            [AP] luck's Tracks:
                            > premium: (server=test2)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            > premium: (server=test1)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            """
                    )

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test1")
                    .thenExpect("""
                            [AP] Demoting luck along track premium from vip+ to vip in context server=test1.
                            [AP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test2")
                    .thenExpect("""
                            [AP] Demoting luck along track premium from vip+ to vip in context server=test2.
                            [AP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test1")
                    .thenExpect("[AP] The end of track premium was reached, so luck was removed from vip.")

                    .givenHasPermissions("aquaperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test2")
                    .thenExpect("[AP] The end of track premium was reached, so luck was removed from vip.")

                    .givenHasPermissions("aquaperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [AP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

    @Test
    public void testSearchCommand(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .whenRunCommand("user Luck permission set hello.world true server=survival")
                    .whenRunCommand("group test permission set hello.world true world=nether")
                    .whenRunCommand("user Luck parent add test")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.search")
                    .whenRunCommand("search hello.world")
                    .thenExpect("""
                            [AP] Searching for users and groups with permissions == hello.world...
                            [AP] Found 2 entries from 1 users and 1 groups.
                            [AP] Showing user entries:    (page 1 of 1 - 1 entries)
                            > luck - true (server=survival)
                            [AP] Showing group entries:    (page 1 of 1 - 1 entries)
                            > test - true (world=nether)
                            """
                    )

                    .givenHasPermissions("aquaperms.search")
                    .whenRunCommand("search ~~ group.%")
                    .thenExpect("""
                                [AP] Searching for users and groups with permissions ~~ group.%...
                                [AP] Found 2 entries from 2 users and 0 groups.
                                [AP] Showing user entries:    (page 1 of 1 - 2 entries)
                                > luck - (group.test) - true
                                > luck - (group.default) - true
                                """
                    );
        });
    }

    @Test
    public void testBulkUpdate(@TempDir Path tempDir) throws InterruptedException {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("skip-bulkupdate-confirmation", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();
            plugin.getStorage().savePlayerData(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch").join();

            CountDownLatch completed = new CountDownLatch(1);

            SingletonPlayer.INSTANCE.addMessageSink(component -> {
                String plain = PlainTextComponentSerializer.plainText().serialize(component);
                if (plain.contains("Bulk update completed successfully")) {
                    completed.countDown();
                }
            });

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup moderator")
                    .whenRunCommand("creategroup admin")
                    .whenRunCommand("user Luck parent add moderator server=survival")
                    .whenRunCommand("user Notch parent add moderator")
                    .whenRunCommand("group admin parent add moderator")
                    .whenRunCommand("group moderator rename mod")
                    .clearMessageBuffer()

                    .givenHasPermissions("aquaperms.bulkupdate")
                    .whenRunCommand("bulkupdate all update permission group.mod \"permission == group.moderator\"")
                    .thenExpectStartsWith("[AP] Running bulk update.");

            assertTrue(completed.await(15, TimeUnit.SECONDS), "operation did not complete in the allotted time");

            Group adminGroup = plugin.getGroupManager().getIfLoaded("admin");
            assertNotNull(adminGroup);
            assertEquals(ImmutableSet.of(Inheritance.builder("mod").build()), adminGroup.normalData().asSet());

            User luckUser = plugin.getStorage().loadUser(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), null).join();
            assertNotNull(luckUser);
            assertEquals(
                    ImmutableSet.of(
                            Inheritance.builder("default").build(),
                            Inheritance.builder("mod").withContext("server", "survival").build()
                    ),
                    luckUser.normalData().asSet()
            );

            User notchUser = plugin.getStorage().loadUser(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), null).join();
            assertNotNull(notchUser);
            assertEquals(
                    ImmutableSet.of(
                            Inheritance.builder("default").build(),
                            Inheritance.builder("mod").build()
                    ),
                    notchUser.normalData().asSet()
            );
        });
    }

    @Test
    public void testInvalidCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.user.info")
                    .whenRunCommand("user unknown info")
                    .thenExpect("[AP] A user for unknown could not be found.")

                    .givenHasPermissions("aquaperms.user.info")
                    .whenRunCommand("user unknown unknown")
                    .thenExpect("[AP] Command not recognised.")

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group unknown info")
                    .thenExpect("[AP] A group named unknown could not be found.")

                    .givenHasPermissions("aquaperms.group.info")
                    .whenRunCommand("group unknown unknown")
                    .thenExpect("[AP] Command not recognised.")

                    .givenHasPermissions("aquaperms.track.info")
                    .whenRunCommand("track unknown info")
                    .thenExpect("[AP] A track named unknown could not be found.")

                    .givenHasPermissions("aquaperms.track.info")
                    .whenRunCommand("track unknown unknown")
                    .thenExpect("[AP] Command not recognised.");
        });
    }

    @Test
    public void testNoPermissions(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            String version = "v" + bootstrap.getVersion();

            new CommandTester(plugin, executor)
                    .givenHasPermissions(/* empty */)

                    .whenRunCommand("")
                    .thenExpect("""
                                [AP] Running AquaPerms %s.
                                [AP] It seems that no permissions have been setup yet!
                                [AP] Before you can use any of the AquaPerms commands in-game, you need to use the console to give yourself access.
                                [AP] Open your console and run:
                                [AP]  > lp user StandaloneUser permission set aquaperms.* true
                                [AP] After you've done this, you can begin to define your permission assignments and groups.
                                [AP] Don't know where to start? Check here: https://luckperms.net/wiki/Usage
                                """.formatted(version)
                    )

                    .whenRunCommand("help")
                    .thenExpect("[AP] Running AquaPerms %s.".formatted(version))

                    .whenRunCommand("group default info")
                    .thenExpect("[AP] Running AquaPerms %s.".formatted(version));
        });
    }

    @Test
    public void testLogNotify(@TempDir Path tempDir) {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("log-notify", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            // by default, notifications are not sent to the user who initiated the event - override that
            app.getApi().getEventBus().subscribe(LogNotifyEvent.class, e -> e.setCancelled(false));

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.group.permission.set", "aquaperms.log.notify")
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("""
                                [AP] Set hello.world to true for default in context server=test.
                                [AP] LOG > (StandaloneUser) [G] (default)
                                [AP] LOG > permission set hello.world true server=test
                                """
                    );
        });
    }

    @Test
    public void testArgumentBasedCommandPermissions(@TempDir Path tempDir) {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("argument-based-command-permissions", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("aquaperms.group.permission.set")
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("[AP] You do not have permission to use this command!")

                    .givenHasPermissions(
                            "aquaperms.group.permission.set",
                            "aquaperms.group.permission.set.modify.default",
                            "aquaperms.group.permission.set.usecontext.global",
                            "aquaperms.group.permission.set.test.permission"
                    )
                    .whenRunCommand("group default permission set test.permission")
                    .thenExpect("[AP] Set test.permission to true for default in context global.")

                    .givenHasPermissions(
                            "aquaperms.group.permission.unset",
                            "aquaperms.group.permission.unset.modify.default",
                            "aquaperms.group.permission.unset.usecontext.global",
                            "aquaperms.group.permission.unset.test.permission"
                    )
                    .whenRunCommand("group default permission unset test.permission")
                    .thenExpect("[AP] Unset test.permission for default in context global.")

                    .givenHasPermissions(
                            "aquaperms.group.permission.set",
                            "aquaperms.group.permission.set.modify.default",
                            "aquaperms.group.permission.set.usecontext.server.test",
                            "aquaperms.group.permission.set.hello.world"
                    )
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("[AP] Set hello.world to true for default in context server=test.")

                    .givenHasPermissions("aquaperms.group.permission.info")
                    .whenRunCommand("group default permission info")
                    .thenExpect("[AP] You do not have permission to use this command!")

                    .givenHasPermissions(
                            "aquaperms.group.permission.info",
                            "aquaperms.group.permission.info.view.default"
                    )
                    .whenRunCommand("group default permission info")
                    .thenExpect("""
                                [AP] default's Permissions:  (page 1 of 1 - 1 entries)
                                > hello.world (server=test)
                                """
                    );
        });
    }

}
