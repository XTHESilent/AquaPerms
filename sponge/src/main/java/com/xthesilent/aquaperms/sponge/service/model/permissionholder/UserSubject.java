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

package com.xthesilent.aquaperms.sponge.service.model.permissionholder;

import com.xthesilent.aquaperms.common.model.PermissionHolderIdentifier;
import com.xthesilent.aquaperms.sponge.LPSpongePlugin;
import com.xthesilent.aquaperms.sponge.model.SpongeUser;
import com.xthesilent.aquaperms.sponge.service.model.LPSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectCollection;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectUser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;

/**
 * Implements {@link LPSubject} for a {@link SpongeUser}.
 */
public final class UserSubject extends PermissionHolderSubject<SpongeUser> implements LPSubject, LPSubjectUser {
    public UserSubject(LPSpongePlugin plugin, SpongeUser parent) {
        super(plugin, parent);
    }

    @Override
    public PermissionHolderIdentifier getIdentifier() {
        return this.parent.getIdentifier();
    }

    @Override
    public Optional<String> getFriendlyIdentifier() {
        return this.parent.getUsername();
    }

    @Override
    public Optional<ServerPlayer> resolvePlayer() {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }

        return Sponge.server().player(this.parent.getUniqueId());
    }

    @Override
    public LPSubjectCollection getParentCollection() {
        return this.plugin.getService().getUserSubjects();
    }
}