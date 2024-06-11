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

package com.xthesilent.aquaperms.common.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xthesilent.aquaperms.common.api.implementation.ApiPermissionHolder;
import com.xthesilent.aquaperms.common.cacheddata.GroupCachedDataManager;
import com.xthesilent.aquaperms.common.cacheddata.UserCachedDataManager;
import com.xthesilent.aquaperms.common.event.gen.GeneratedEventClass;
import com.xthesilent.aquaperms.common.event.model.EntitySourceImpl;
import com.xthesilent.aquaperms.common.event.model.SenderPlatformEntity;
import com.xthesilent.aquaperms.common.event.model.UnknownSource;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Difference;
import com.aquasplashmc.api.actionlog.Action;
import com.aquasplashmc.api.event.AquaPermsEvent;
import com.aquasplashmc.api.event.cause.CreationCause;
import com.aquasplashmc.api.event.cause.DeletionCause;
import com.aquasplashmc.api.event.context.ContextUpdateEvent;
import com.aquasplashmc.api.event.extension.ExtensionLoadEvent;
import com.aquasplashmc.api.event.group.GroupCacheLoadEvent;
import com.aquasplashmc.api.event.group.GroupCreateEvent;
import com.aquasplashmc.api.event.group.GroupDataRecalculateEvent;
import com.aquasplashmc.api.event.group.GroupDeleteEvent;
import com.aquasplashmc.api.event.group.GroupLoadAllEvent;
import com.aquasplashmc.api.event.group.GroupLoadEvent;
import com.aquasplashmc.api.event.log.LogBroadcastEvent;
import com.aquasplashmc.api.event.log.LogNetworkPublishEvent;
import com.aquasplashmc.api.event.log.LogNotifyEvent;
import com.aquasplashmc.api.event.log.LogPublishEvent;
import com.aquasplashmc.api.event.log.LogReceiveEvent;
import com.aquasplashmc.api.event.messaging.CustomMessageReceiveEvent;
import com.aquasplashmc.api.event.node.NodeAddEvent;
import com.aquasplashmc.api.event.node.NodeClearEvent;
import com.aquasplashmc.api.event.node.NodeMutateEvent;
import com.aquasplashmc.api.event.node.NodeRemoveEvent;
import com.aquasplashmc.api.event.player.PlayerDataSaveEvent;
import com.aquasplashmc.api.event.player.PlayerLoginProcessEvent;
import com.aquasplashmc.api.event.player.lookup.UniqueIdDetermineTypeEvent;
import com.aquasplashmc.api.event.player.lookup.UniqueIdLookupEvent;
import com.aquasplashmc.api.event.player.lookup.UsernameLookupEvent;
import com.aquasplashmc.api.event.player.lookup.UsernameValidityCheckEvent;
import com.aquasplashmc.api.event.source.Source;
import com.aquasplashmc.api.event.sync.ConfigReloadEvent;
import com.aquasplashmc.api.event.sync.PostNetworkSyncEvent;
import com.aquasplashmc.api.event.sync.PostSyncEvent;
import com.aquasplashmc.api.event.sync.PreNetworkSyncEvent;
import com.aquasplashmc.api.event.sync.PreSyncEvent;
import com.aquasplashmc.api.event.sync.SyncType;
import com.aquasplashmc.api.event.track.TrackCreateEvent;
import com.aquasplashmc.api.event.track.TrackDeleteEvent;
import com.aquasplashmc.api.event.track.TrackLoadAllEvent;
import com.aquasplashmc.api.event.track.TrackLoadEvent;
import com.aquasplashmc.api.event.track.mutate.TrackAddGroupEvent;
import com.aquasplashmc.api.event.track.mutate.TrackClearEvent;
import com.aquasplashmc.api.event.track.mutate.TrackRemoveGroupEvent;
import com.aquasplashmc.api.event.type.Cancellable;
import com.aquasplashmc.api.event.type.ResultEvent;
import com.aquasplashmc.api.event.user.UserCacheLoadEvent;
import com.aquasplashmc.api.event.user.UserDataRecalculateEvent;
import com.aquasplashmc.api.event.user.UserFirstLoginEvent;
import com.aquasplashmc.api.event.user.UserLoadEvent;
import com.aquasplashmc.api.event.user.UserUnloadEvent;
import com.aquasplashmc.api.event.user.track.UserDemoteEvent;
import com.aquasplashmc.api.event.user.track.UserPromoteEvent;
import com.aquasplashmc.api.extension.Extension;
import com.aquasplashmc.api.model.PlayerSaveResult;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.Node;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class EventDispatcher {
    private final AbstractEventBus<?> eventBus;

    public EventDispatcher(AbstractEventBus<?> eventBus) {
        this.eventBus = eventBus;
    }

    public AbstractEventBus<?> getEventBus() {
        return this.eventBus;
    }

    private AquaPermsEvent generate(Class<? extends AquaPermsEvent> eventClass, Object... params) {
        try {
            return GeneratedEventClass.generate(eventClass).newInstance(this.eventBus.getApiProvider(), params);
        } catch (Throwable e) {
            throw new RuntimeException("Exception occurred whilst generating event instance", e);
        }
    }

    private void post(Class<? extends AquaPermsEvent> eventClass, Object... params) {
        AquaPermsEvent event = generate(eventClass, params);
        this.eventBus.post(event);
    }

    private void postAsync(Class<? extends AquaPermsEvent> eventClass, Object... params) {
        // check against common mistakes - events with any sort of result shouldn't be posted async
        if (Cancellable.class.isAssignableFrom(eventClass) || ResultEvent.class.isAssignableFrom(eventClass)) {
            throw new RuntimeException("Event cannot be posted async (" + eventClass.getName() + ")");
        }

        // if there aren't any handlers registered for the event, don't bother trying to post it
        if (!this.eventBus.shouldPost(eventClass)) {
            return;
        }

        // async: generate an event class and post it
        this.eventBus.getPlugin().getBootstrap().getScheduler().executeAsync(() -> post(eventClass, params));
    }

    private void postSync(Class<? extends AquaPermsEvent> eventClass, Object... params) {
        // if there aren't any handlers registered for our event, don't bother trying to post it
        if (!this.eventBus.shouldPost(eventClass)) {
            return;
        }

        // generate an event class and post it
        post(eventClass, params);
    }

    private boolean postCancellable(Class<? extends AquaPermsEvent> eventClass, Object... params) {
        if (!Cancellable.class.isAssignableFrom(eventClass)) {
            throw new RuntimeException("Event is not cancellable: " + eventClass.getName());
        }

        // extract the initial state from the first parameter
        boolean initialState = (boolean) params[0];

        // if there aren't any handlers registered for the event, just return the initial state
        if (!this.eventBus.shouldPost(eventClass)) {
            return initialState;
        }

        // otherwise:
        // - initialise an AtomicBoolean for the result with the initial state
        // - replace the boolean with the AtomicBoolean in the params array
        // - generate an event class and post it
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        params[0] = cancel;
        post(eventClass, params);

        // return the final status
        return cancel.get();
    }

    public void dispatchContextUpdate(Object subject) {
        postSync(ContextUpdateEvent.class, subject);
    }

    public void dispatchExtensionLoad(Extension extension) {
        postAsync(ExtensionLoadEvent.class, extension);
    }

    public void dispatchGroupCacheLoad(Group group, GroupCachedDataManager data) {
        postAsync(GroupCacheLoadEvent.class, group.getApiProxy(), data);
    }

    public void dispatchGroupCreate(Group group, CreationCause cause) {
        postAsync(GroupCreateEvent.class, group.getApiProxy(), cause);
    }

    public void dispatchGroupDelete(Group group, DeletionCause cause) {
        postAsync(GroupDeleteEvent.class, group.getName(), ImmutableSet.copyOf(group.normalData().asSet()), cause);
    }

    public void dispatchGroupLoadAll() {
        postAsync(GroupLoadAllEvent.class);
    }

    public void dispatchGroupLoad(Group group) {
        postAsync(GroupLoadEvent.class, group.getApiProxy());
    }

    public boolean dispatchLogBroadcast(boolean initialState, Action entry, LogBroadcastEvent.Origin origin) {
        return postCancellable(LogBroadcastEvent.class, initialState, entry, origin);
    }

    public boolean dispatchLogPublish(boolean initialState, Action entry) {
        return postCancellable(LogPublishEvent.class, initialState, entry);
    }

    public boolean dispatchLogNetworkPublish(boolean initialState, UUID id, Action entry) {
        return postCancellable(LogNetworkPublishEvent.class, initialState, id, entry);
    }

    public boolean dispatchLogNotify(boolean initialState, Action entry, LogNotifyEvent.Origin origin, Sender sender) {
        return postCancellable(LogNotifyEvent.class, initialState, entry, origin, new SenderPlatformEntity(sender));
    }

    public void dispatchLogReceive(UUID id, Action entry) {
        postAsync(LogReceiveEvent.class, id, entry);
    }

    public void dispatchCustomMessageReceive(String channelId, String payload) {
        postAsync(CustomMessageReceiveEvent.class, channelId, payload);
    }

    public void dispatchNodeChanges(PermissionHolder target, DataType dataType, Difference<Node> changes) {
        if (!this.eventBus.shouldPost(NodeAddEvent.class) && !this.eventBus.shouldPost(NodeRemoveEvent.class)) {
            return;
        }

        if (changes.isEmpty()) {
            return;
        }

        ApiPermissionHolder proxy = proxy(target);
        ImmutableSet<Node> state = target.getData(dataType).asImmutableSet();

        // call an event for each recorded change
        for (Difference.Change<Node> change : changes.getChanges()) {
            Class<? extends NodeMutateEvent> type = change.type() == Difference.ChangeType.ADD ?
                    NodeAddEvent.class : NodeRemoveEvent.class;

            postAsync(type, proxy, dataType, state, change.value());
        }
    }

    public void dispatchNodeClear(PermissionHolder target, DataType dataType, Difference<Node> changes) {
        if (!this.eventBus.shouldPost(NodeClearEvent.class)) {
            return;
        }

        if (changes.isEmpty()) {
            return;
        }

        ApiPermissionHolder proxy = proxy(target);
        ImmutableSet<Node> state = target.getData(dataType).asImmutableSet();

        // call clear event
        ImmutableSet<Node> nodes = ImmutableSet.copyOf(changes.getRemoved());
        postAsync(NodeClearEvent.class, proxy, dataType, state, nodes);

        // call add event if needed for any nodes that were added
        for (Node added : changes.getAdded()) {
            postAsync(NodeAddEvent.class, proxy, dataType, state, added);
        }
    }

    public void dispatchConfigReload() {
        postAsync(ConfigReloadEvent.class);
    }

    public void dispatchNetworkPostSync(UUID id, SyncType type, boolean didOccur, UUID specificUserUniqueId) {
        postAsync(PostNetworkSyncEvent.class, id, type, didOccur, specificUserUniqueId);
    }

    public void dispatchPostSync() {
        postAsync(PostSyncEvent.class);
    }

    public boolean dispatchNetworkPreSync(boolean initialState, UUID id, SyncType type, UUID specificUserUniqueId) {
        return postCancellable(PreNetworkSyncEvent.class, initialState, id, type, specificUserUniqueId);
    }

    public boolean dispatchPreSync(boolean initialState) {
        return postCancellable(PreSyncEvent.class, initialState);
    }

    public void dispatchTrackCreate(Track track, CreationCause cause) {
        postAsync(TrackCreateEvent.class, track.getApiProxy(), cause);
    }

    public void dispatchTrackDelete(Track track, DeletionCause cause) {
        postAsync(TrackDeleteEvent.class, track.getName(), ImmutableList.copyOf(track.getGroups()), cause);
    }

    public void dispatchTrackLoadAll() {
        postAsync(TrackLoadAllEvent.class);
    }

    public void dispatchTrackLoad(Track track) {
        postAsync(TrackLoadEvent.class, track.getApiProxy());
    }

    public void dispatchTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        postAsync(TrackAddGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group);
    }

    public void dispatchTrackClear(Track track, List<String> before) {
        postAsync(TrackClearEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.of());
    }

    public void dispatchTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        postAsync(TrackRemoveGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group);
    }

    public void dispatchUserCacheLoad(User user, UserCachedDataManager data) {
        postAsync(UserCacheLoadEvent.class, user.getApiProxy(), data);
    }

    public void dispatchDataRecalculate(PermissionHolder holder) {
        if (holder.getType() == HolderType.USER) {
            User user = (User) holder;
            postAsync(UserDataRecalculateEvent.class, user.getApiProxy(), user.getCachedData());
        } else {
            Group group = (Group) holder;
            postAsync(GroupDataRecalculateEvent.class, group.getApiProxy(), group.getCachedData());
        }
    }

    public void dispatchUserFirstLogin(UUID uniqueId, String username) {
        postAsync(UserFirstLoginEvent.class, uniqueId, username);
    }

    public void dispatchPlayerLoginProcess(UUID uniqueId, String username, @Nullable User user) {
        postSync(PlayerLoginProcessEvent.class, uniqueId, username, user == null ? null : user.getApiProxy());
    }

    public void dispatchPlayerDataSave(UUID uniqueId, String username, PlayerSaveResult result) {
        postAsync(PlayerDataSaveEvent.class, uniqueId, username, result);
    }

    public String dispatchUniqueIdDetermineType(UUID uniqueId, String initialType) {
        AtomicReference<String> result = new AtomicReference<>(initialType);
        postSync(UniqueIdDetermineTypeEvent.class, result, uniqueId);
        return result.get();
    }

    public UUID dispatchUniqueIdLookup(String username, UUID initial) {
        AtomicReference<UUID> result = new AtomicReference<>(initial);
        postSync(UniqueIdLookupEvent.class, result, username);
        return result.get();
    }

    public String dispatchUsernameLookup(UUID uniqueId, String initial) {
        AtomicReference<String> result = new AtomicReference<>(initial);
        postSync(UsernameLookupEvent.class, result, uniqueId);
        return result.get();
    }

    public boolean dispatchUsernameValidityCheck(String username, boolean initialState) {
        AtomicBoolean result = new AtomicBoolean(initialState);
        postSync(UsernameValidityCheckEvent.class, username, result);
        return result.get();
    }

    public void dispatchUserLoad(User user) {
        postAsync(UserLoadEvent.class, user.getApiProxy());
    }

    public boolean dispatchUserUnload(User user) {
        return postCancellable(UserUnloadEvent.class, false, user.getApiProxy());
    }

    public void dispatchUserDemote(User user, Track track, String from, String to, @Nullable Sender sender) {
        Source source = sender == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(sender));
        postAsync(UserDemoteEvent.class, source, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
    }

    public void dispatchUserPromote(User user, Track track, String from, String to, @Nullable Sender sender) {
        Source source = sender == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(sender));
        postAsync(UserPromoteEvent.class, source, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
    }

    private static ApiPermissionHolder proxy(PermissionHolder holder) {
        if (holder instanceof Group) {
            return ((Group) holder).getApiProxy();
        } else if (holder instanceof User) {
            return ((User) holder).getApiProxy();
        } else {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends AquaPermsEvent>[] getKnownEventTypes() {
        return new Class[]{
                ContextUpdateEvent.class,
                ExtensionLoadEvent.class,
                GroupCacheLoadEvent.class,
                GroupCreateEvent.class,
                GroupDataRecalculateEvent.class,
                GroupDeleteEvent.class,
                GroupLoadAllEvent.class,
                GroupLoadEvent.class,
                LogBroadcastEvent.class,
                LogNetworkPublishEvent.class,
                LogNotifyEvent.class,
                LogPublishEvent.class,
                LogReceiveEvent.class,
                CustomMessageReceiveEvent.class,
                NodeAddEvent.class,
                NodeClearEvent.class,
                NodeRemoveEvent.class,
                PlayerDataSaveEvent.class,
                PlayerLoginProcessEvent.class,
                UniqueIdDetermineTypeEvent.class,
                UniqueIdLookupEvent.class,
                UsernameLookupEvent.class,
                UsernameValidityCheckEvent.class,
                ConfigReloadEvent.class,
                PostNetworkSyncEvent.class,
                PostSyncEvent.class,
                PreNetworkSyncEvent.class,
                PreSyncEvent.class,
                TrackCreateEvent.class,
                TrackDeleteEvent.class,
                TrackLoadAllEvent.class,
                TrackLoadEvent.class,
                TrackAddGroupEvent.class,
                TrackClearEvent.class,
                TrackRemoveGroupEvent.class,
                UserCacheLoadEvent.class,
                UserDataRecalculateEvent.class,
                UserFirstLoginEvent.class,
                UserLoadEvent.class,
                UserUnloadEvent.class,
                UserDemoteEvent.class,
                UserPromoteEvent.class
        };
    }

}
