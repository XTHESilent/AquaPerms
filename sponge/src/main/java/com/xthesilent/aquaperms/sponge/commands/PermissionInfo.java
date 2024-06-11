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

package com.xthesilent.aquaperms.sponge.commands;

import com.google.common.collect.ImmutableMap;
import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectData;
import com.aquasplashmc.api.context.ImmutableContextSet;

import java.util.Map;

public class PermissionInfo extends ChildCommand<LPSubjectData> {
    public PermissionInfo() {
        super(CommandSpec.SPONGE_PERMISSION_INFO, "info", CommandPermission.SPONGE_PERMISSION_INFO, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, LPSubjectData subjectData, ArgumentList args, String label) {
        ImmutableContextSet contextSet = args.getContextOrEmpty(0);
        if (contextSet.isEmpty()) {
            SpongeCommandUtils.sendPrefixed(sender, "&aShowing permissions matching contexts &bANY&a.");
            Map<ImmutableContextSet, ImmutableMap<String, Boolean>> permissions = subjectData.getAllPermissions();
            if (permissions.isEmpty()) {
                SpongeCommandUtils.sendPrefixed(sender, "That subject does not have any permissions defined.");
                return;
            }

            for (Map.Entry<ImmutableContextSet, ImmutableMap<String, Boolean>> e : permissions.entrySet()) {
                SpongeCommandUtils.sendPrefixed(sender, "&3>> &bContext: " + SpongeCommandUtils.contextToString(e.getKey()) + "\n" + SpongeCommandUtils.nodesToString(e.getValue()));
            }

        } else {
            Map<String, Boolean> permissions = subjectData.getPermissions(contextSet);
            if (permissions.isEmpty()) {
                SpongeCommandUtils.sendPrefixed(sender, "That subject does not have any permissions defined in those contexts.");
                return;
            }

            SpongeCommandUtils.sendPrefixed(sender, "&aShowing permissions matching contexts &b" +
                        SpongeCommandUtils.contextToString(contextSet) + "&a.\n" + SpongeCommandUtils.nodesToString(permissions));
        }
    }
}
