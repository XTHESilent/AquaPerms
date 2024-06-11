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

package com.xthesilent.aquaperms.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.verbose.VerboseCheckTarget;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.xthesilent.aquaperms.velocity.LPVelocityPlugin;
import com.xthesilent.aquaperms.velocity.service.CompatibilityUtil;
import com.aquasplashmc.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MonitoringPermissionCheckListener {
    private final LPVelocityPlugin plugin;

    public MonitoringPermissionCheckListener(LPVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onOtherPermissionSetup(PermissionsSetupEvent e) {
        // players are handled separately
        if (e.getSubject() instanceof Player) {
            return;
        }

        e.setProvider(new MonitoredPermissionProvider(e.getProvider()));
    }

    private final class MonitoredPermissionProvider implements PermissionProvider {
        private final PermissionProvider delegate;

        MonitoredPermissionProvider(PermissionProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NonNull PermissionFunction createFunction(@NonNull PermissionSubject subject) {
            PermissionFunction function = this.delegate.createFunction(subject);
            return new MonitoredPermissionFunction(subject, function);
        }
    }

    private final class MonitoredPermissionFunction implements PermissionFunction {
        private final VerboseCheckTarget verboseCheckTarget;
        private final PermissionFunction delegate;

        MonitoredPermissionFunction(PermissionSubject subject, PermissionFunction delegate) {
            this.delegate = delegate;
            this.verboseCheckTarget = VerboseCheckTarget.internal(determineName(subject));
        }

        @Override
        public com.velocitypowered.api.permission.@NonNull Tristate getPermissionValue(@NonNull String permission) {
            com.velocitypowered.api.permission.Tristate setting = this.delegate.getPermissionValue(permission);

            // report result
            Tristate result = CompatibilityUtil.convertTristate(setting);

            MonitoringPermissionCheckListener.this.plugin.getVerboseHandler().offerPermissionCheckEvent(CheckOrigin.PLATFORM_API_HAS_PERMISSION_SET, this.verboseCheckTarget, QueryOptionsImpl.DEFAULT_CONTEXTUAL, permission, TristateResult.forMonitoredResult(result));
            MonitoringPermissionCheckListener.this.plugin.getPermissionRegistry().offer(permission);

            return setting;
        }
    }

    private String determineName(PermissionSubject subject) {
        if (subject == this.plugin.getBootstrap().getProxy().getConsoleCommandSource()) {
            return "console";
        }
        return subject.getClass().getSimpleName();
    }
}
