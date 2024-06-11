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

package com.xthesilent.aquaperms.nukkit.inject.permissible;

import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.permission.PermissionAttachmentInfo;
import cn.nukkit.plugin.Plugin;
import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.verbose.VerboseCheckTarget;
import com.xthesilent.aquaperms.common.verbose.VerboseHandler;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.aquasplashmc.api.util.Tristate;

import java.util.Map;

/**
 * A PermissibleBase extension which logs permission checks to the
 * plugin's {@link VerboseHandler} facility.
 *
 * Method calls are forwarded to the delegate permissible.
 */
public class MonitoredPermissibleBase extends PermissibleBase {
    final AquaPermsPlugin plugin;
    private final PermissibleBase delegate;
    private final VerboseCheckTarget verboseCheckTarget;

    // remains false until the object has been constructed
    // necessary to catch the superclass call to #recalculatePermissions on init
    @SuppressWarnings("UnusedAssignment")
    private boolean initialised = false;

    public MonitoredPermissibleBase(AquaPermsPlugin plugin, PermissibleBase delegate, String name) {
        super(null);
        DummyPermissibleBase.copyFields(delegate, this);

        this.plugin = plugin;
        this.delegate = delegate;
        this.verboseCheckTarget = VerboseCheckTarget.internal(name);
        this.initialised = true;
    }

    private void logCheck(CheckOrigin origin, String permission, boolean result) {
        this.plugin.getVerboseHandler().offerPermissionCheckEvent(origin, this.verboseCheckTarget, QueryOptionsImpl.DEFAULT_CONTEXTUAL, permission, TristateResult.forMonitoredResult(Tristate.of(result)));
        this.plugin.getPermissionRegistry().offer(permission);
    }

    PermissibleBase getDelegate() {
        return this.delegate;
    }

    @Override
    public boolean isPermissionSet(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.isPermissionSet(permission);
        logCheck(CheckOrigin.PLATFORM_API_HAS_PERMISSION_SET, permission, result);
        return result;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.isPermissionSet(permission);
        logCheck(CheckOrigin.PLATFORM_API_HAS_PERMISSION_SET, permission.getName(), result);
        return result;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.hasPermission(permission);
        logCheck(CheckOrigin.PLATFORM_API_HAS_PERMISSION, permission, result);
        return result;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.hasPermission(permission);
        logCheck(CheckOrigin.PLATFORM_API_HAS_PERMISSION, permission.getName(), result);
        return result;
    }

    @Override
    public void recalculatePermissions() {
        if (!this.initialised) {
            return;
        }

        this.delegate.recalculatePermissions();
    }

    // just forward calls to the delegate permissible
    @Override public boolean isOp() { return this.delegate.isOp(); }
    @Override public void setOp(boolean value) { this.delegate.setOp(value); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, Boolean value) { return this.delegate.addAttachment(plugin, name, value); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name) { return this.delegate.addAttachment(plugin, name); }
    @Override public PermissionAttachment addAttachment(Plugin plugin) { return this.delegate.addAttachment(plugin); }
    @Override public void removeAttachment(PermissionAttachment attachment) { this.delegate.removeAttachment(attachment); }
    @Override public void clearPermissions() { this.delegate.clearPermissions(); }
    @Override public Map<String, PermissionAttachmentInfo> getEffectivePermissions() { return this.delegate.getEffectivePermissions(); }

}
