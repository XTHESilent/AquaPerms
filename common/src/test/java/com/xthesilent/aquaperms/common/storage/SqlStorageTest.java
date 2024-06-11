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

package com.xthesilent.aquaperms.common.storage;

import com.google.common.collect.ImmutableSet;
import com.xthesilent.aquaperms.common.actionlog.Log;
import com.xthesilent.aquaperms.common.actionlog.LoggedAction;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.config.AquaPermsConfiguration;
import com.xthesilent.aquaperms.common.event.EventDispatcher;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PrimaryGroupHolder;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.model.manager.group.GroupManager;
import com.xthesilent.aquaperms.common.model.manager.group.StandardGroupManager;
import com.xthesilent.aquaperms.common.model.manager.user.StandardUserManager;
import com.xthesilent.aquaperms.common.model.manager.user.UserManager;
import com.xthesilent.aquaperms.common.node.types.Inheritance;
import com.xthesilent.aquaperms.common.node.types.Permission;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.plugin.bootstrap.AquaPermsBootstrap;
import com.xthesilent.aquaperms.common.plugin.scheduler.SchedulerAdapter;
import com.xthesilent.aquaperms.common.storage.implementation.sql.SqlStorage;
import com.xthesilent.aquaperms.common.storage.implementation.sql.connection.ConnectionFactory;
import com.xthesilent.aquaperms.common.storage.implementation.sql.connection.file.NonClosableConnection;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.model.PlayerSaveResult;
import com.aquasplashmc.api.model.PlayerSaveResult.Outcome;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.types.InheritanceNode;
import com.aquasplashmc.api.node.types.PermissionNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SqlStorageTest {

    @Mock private AquaPermsPlugin plugin;
    @Mock private AquaPermsBootstrap bootstrap;
    @Mock private AquaPermsConfiguration configuration;

    private SqlStorage storage;

    @BeforeEach
    public void setupMocksAndDatabase() throws Exception {
        lenient().when(this.plugin.getBootstrap()).thenReturn(this.bootstrap);
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.bootstrap.getScheduler()).thenReturn(mock(SchedulerAdapter.class));
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(PrimaryGroupHolder.AllParentsByWeight::new);
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD)).thenReturn("parents-by-weight");
        lenient().when(this.bootstrap.getResourceStream(anyString()))
                .then(answer((String path) -> SqlStorageTest.class.getClassLoader().getResourceAsStream(path)));
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));

        this.storage = new SqlStorage(this.plugin, new TestH2ConnectionFactory(), "aquaperms_");
        this.storage.init();
    }

    @AfterEach
    public void shutdownDatabase() {
        this.storage.shutdown();
    }

    @Test
    public void testActionLog() throws Exception {
        LoggedAction action = LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("hello 123 hello 123")
                .build();

        this.storage.logAction(action);

        Log log = this.storage.getLog();
        assertEquals(1, log.getContent().size());
        assertEquals(action, log.getContent().first());
    }

    @Test
    public void testSavePlayerData() throws Exception {
        UUID uniqueId = UUID.randomUUID();

        // clean insert
        PlayerSaveResult r1 = this.storage.savePlayerData(uniqueId, "Player1");
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT), r1.getOutcomes());
        assertNull(r1.getOtherUniqueIds());
        assertNull(r1.getPreviousUsername());

        // no change expected
        PlayerSaveResult r2 = this.storage.savePlayerData(uniqueId, "Player1");
        assertEquals(ImmutableSet.of(Outcome.NO_CHANGE), r2.getOutcomes());
        assertNull(r2.getOtherUniqueIds());
        assertNull(r2.getPreviousUsername());

        // changed username
        PlayerSaveResult r3 = this.storage.savePlayerData(uniqueId, "Player2");
        assertEquals(ImmutableSet.of(Outcome.USERNAME_UPDATED), r3.getOutcomes());
        assertNull(r3.getOtherUniqueIds());
        assertTrue("Player1".equalsIgnoreCase(r3.getPreviousUsername()));

        // changed uuid
        UUID newUniqueId = UUID.randomUUID();
        PlayerSaveResult r4 = this.storage.savePlayerData(newUniqueId, "Player2");
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT, Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME), r4.getOutcomes());
        assertNotNull(r4.getOtherUniqueIds());
        assertEquals(ImmutableSet.of(uniqueId), r4.getOtherUniqueIds());
        assertNull(r2.getPreviousUsername());
    }

    @Test
    public void testGetPlayerUniqueIdAndName() throws Exception {
        UUID uniqueId = UUID.randomUUID();
        String username = "Player1";

        this.storage.savePlayerData(uniqueId, username);

        assertEquals(uniqueId, this.storage.getPlayerUniqueId("Player1"));
        assertTrue(username.equalsIgnoreCase(this.storage.getPlayerName(uniqueId)));
    }

    @Test
    public void testGetPlayerUniqueIdAndNameNull() throws Exception {
        assertNull(this.storage.getPlayerUniqueId("Player1"));
        assertNull(this.storage.getPlayerName(UUID.randomUUID()));
    }

    @Test
    public void testSaveAndLoadGroup() throws Exception {
        StandardGroupManager groupManager = new StandardGroupManager(this.plugin);

        //noinspection unchecked,rawtypes
        lenient().when(this.plugin.getGroupManager()).thenReturn((GroupManager) groupManager);

        Group group = this.storage.createAndLoadGroup("test");

        group.normalData().add(Permission.builder()
                .permission("test.1")
                .withContext("server", "test")
                .build()
        );
        group.normalData().add(Permission.builder()
                .permission("test.2")
                .withContext("world", "test")
                .build()
        );
        group.normalData().add(Permission.builder()
                .permission("test.3")
                .expiry(1, TimeUnit.HOURS)
                .withContext("server", "test")
                .withContext("world", "test")
                .withContext("hello", "test")
                .build()
        );

        Set<Node> nodes = group.normalData().asSet();
        assertEquals(3, nodes.size());

        this.storage.saveGroup(group);
        groupManager.unload("test");

        Group loaded = this.storage.loadGroup("test").orElse(null);
        assertNotNull(loaded);
        assertNotSame(group, loaded);
        assertEquals(nodes, loaded.normalData().asSet());
    }

    @Test
    public void testSaveAndDeleteUser() throws SQLException {
        StandardUserManager userManager = new StandardUserManager(this.plugin);

        //noinspection unchecked,rawtypes
        when(this.plugin.getUserManager()).thenReturn((UserManager) userManager);

        UUID exampleUniqueId = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        String exampleUsername = "Notch";
        PermissionNode examplePermission = Permission.builder()
                .permission("test.1")
                .withContext("server", "test")
                .build();
        InheritanceNode defaultGroupNode = Inheritance.builder(GroupManager.DEFAULT_GROUP_NAME).build();

        // create a default user, assert that is doesn't appear in unique users list
        this.storage.savePlayerData(exampleUniqueId, exampleUsername);
        assertFalse(this.storage.getUniqueUsers().contains(exampleUniqueId));

        // give the user a node, assert that it does appear in unique users list
        User user = this.storage.loadUser(exampleUniqueId, exampleUsername);
        user.setNode(DataType.NORMAL, examplePermission, true);
        this.storage.saveUser(user);
        assertTrue(this.storage.getUniqueUsers().contains(exampleUniqueId));

        // clear all nodes (reset to default) and assert that it does not appear in unique users list
        user.clearNodes(DataType.NORMAL, null, true);
        this.storage.saveUser(user);
        assertFalse(this.storage.getUniqueUsers().contains(exampleUniqueId));
        assertEquals(ImmutableSet.of(defaultGroupNode), user.normalData().asSet());

        // give it a node again, assert that it shows as a unique user
        user.setNode(DataType.NORMAL, examplePermission, true);
        this.storage.saveUser(user);
        assertTrue(this.storage.getUniqueUsers().contains(exampleUniqueId));
        assertEquals(ImmutableSet.of(defaultGroupNode, examplePermission), user.normalData().asSet());

        // reload user data from the db and assert that it is unchanged
        user = this.storage.loadUser(exampleUniqueId, exampleUsername);
        assertEquals(ImmutableSet.of(defaultGroupNode, examplePermission), user.normalData().asSet());
    }

    private static class TestH2ConnectionFactory implements ConnectionFactory {
        private final NonClosableConnection connection;

        TestH2ConnectionFactory() throws SQLException {
            this.connection = new NonClosableConnection(
                    DriverManager.getConnection("jdbc:h2:mem:test")
            );
        }

        @Override
        public Connection getConnection() {
            return this.connection;
        }

        @Override
        public String getImplementationName() {
            return "H2";
        }

        @Override
        public StorageMetadata getMeta() {
            return new StorageMetadata();
        }

        @Override
        public void init(AquaPermsPlugin plugin) {

        }

        @Override
        public Function<String, String> getStatementProcessor() {
            return s -> s.replace('\'', '`')
                    .replace("LIKE", "ILIKE")
                    .replace("value", "`value`")
                    .replace("``value``", "`value`");
        }

        @Override
        public void shutdown() throws Exception {
            this.connection.shutdown();
        }
    }

}
