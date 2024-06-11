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

package com.xthesilent.aquaperms.common.storage.implementation;

import com.xthesilent.aquaperms.common.actionlog.Log;
import com.xthesilent.aquaperms.common.bulkupdate.BulkUpdate;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.node.matcher.ConstraintNodeMatcher;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.storage.StorageMetadata;
import com.xthesilent.aquaperms.common.storage.misc.NodeEntry;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.model.PlayerSaveResult;
import com.aquasplashmc.api.node.Node;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StorageImplementation {
    AquaPermsPlugin getPlugin();

    String getImplementationName();

    void init() throws Exception;

    void shutdown();

    StorageMetadata getMeta();

    void logAction(Action entry) throws Exception;

    Log getLog() throws Exception;

    void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception;

    User loadUser(UUID uniqueId, String username) throws Exception;

    Map<UUID, User> loadUsers(Set<UUID> uniqueIds) throws Exception;

    void saveUser(User user) throws Exception;

    Set<UUID> getUniqueUsers() throws Exception;

    <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> constraint) throws Exception;

    Group createAndLoadGroup(String name) throws Exception;

    Optional<Group> loadGroup(String name) throws Exception;

    void loadAllGroups() throws Exception;

    void saveGroup(Group group) throws Exception;

    void deleteGroup(Group group) throws Exception;

    <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> constraint) throws Exception;

    Track createAndLoadTrack(String name) throws Exception;

    Optional<Track> loadTrack(String name) throws Exception;

    void loadAllTracks() throws Exception;

    void saveTrack(Track track) throws Exception;

    void deleteTrack(Track track) throws Exception;

    PlayerSaveResult savePlayerData(UUID uniqueId, String username) throws Exception;

    void deletePlayerData(UUID uniqueId) throws Exception;

    @Nullable UUID getPlayerUniqueId(String username) throws Exception;

    @Nullable String getPlayerName(UUID uniqueId) throws Exception;
}
