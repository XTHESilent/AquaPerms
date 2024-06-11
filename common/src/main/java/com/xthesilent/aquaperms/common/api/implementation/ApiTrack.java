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

package com.xthesilent.aquaperms.common.api.implementation;

import com.google.common.base.Preconditions;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.aquasplashmc.api.context.ContextSet;
import com.aquasplashmc.api.model.data.DataMutateResult;
import com.aquasplashmc.api.model.group.Group;
import com.aquasplashmc.api.model.user.User;
import com.aquasplashmc.api.track.DemotionResult;
import com.aquasplashmc.api.track.PromotionResult;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;

public class ApiTrack implements com.aquasplashmc.api.track.Track {
    public static Track cast(com.aquasplashmc.api.track.Track track) {
        Objects.requireNonNull(track, "track");
        Preconditions.checkState(track instanceof ApiTrack, "Illegal instance " + track.getClass() + " cannot be handled by this implementation.");
        return ((ApiTrack) track).getHandle();
    }

    private final Track handle;
    
    public ApiTrack(Track handle) {
        this.handle = handle;
    }

    Track getHandle() {
        return this.handle;
    }

    @Override
    public @NonNull String getName() {
        return this.handle.getName();
    }

    @Override
    public @NonNull List<String> getGroups() {
        return this.handle.getGroups();
    }

    @Override
    public String getNext(@NonNull Group current) {
        Objects.requireNonNull(current, "current");
        try {
            return this.handle.getNext(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getPrevious(@NonNull Group current) {
        Objects.requireNonNull(current, "current");
        try {
            return this.handle.getPrevious(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public @NonNull PromotionResult promote(@NonNull User user, @NonNull ContextSet contextSet) {
        return this.handle.promote(ApiUser.cast(user), contextSet, Predicates.alwaysTrue(), null, true);
    }

    @Override
    public @NonNull DemotionResult demote(@NonNull User user, @NonNull ContextSet contextSet) {
        return this.handle.demote(ApiUser.cast(user), contextSet, Predicates.alwaysTrue(), null, true);
    }

    @Override
    public @NonNull DataMutateResult appendGroup(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.appendGroup(ApiGroup.cast(group));
    }

    @Override
    public @NonNull DataMutateResult insertGroup(@NonNull Group group, int position) throws IndexOutOfBoundsException {
        Objects.requireNonNull(group, "group");
        return this.handle.insertGroup(ApiGroup.cast(group), position);
    }

    @Override
    public @NonNull DataMutateResult removeGroup(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.removeGroup(ApiGroup.cast(group));
    }

    @Override
    public @NonNull DataMutateResult removeGroup(@NonNull String group) {
        Objects.requireNonNull(group, "group");
        return this.handle.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.containsGroup(ApiGroup.cast(group));
    }

    @Override
    public boolean containsGroup(@NonNull String group) {
        Objects.requireNonNull(group, "group");
        return this.handle.containsGroup(group);
    }

    @Override
    public void clearGroups() {
        this.handle.clearGroups();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiTrack)) return false;
        ApiTrack that = (ApiTrack) o;
        return this.handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }
}
