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

import com.xthesilent.aquaperms.common.command.abstraction.ChildCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.sponge.service.model.LPPermissionService;
import com.xthesilent.aquaperms.sponge.service.model.LPSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectCollection;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectData;
import com.aquasplashmc.api.context.ImmutableContextSet;

public class ParentRemove extends ChildCommand<LPSubjectData> {
    public ParentRemove() {
        super(CommandSpec.SPONGE_PARENT_REMOVE, "remove", CommandPermission.SPONGE_PARENT_REMOVE, Predicates.inRange(0, 1));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, LPSubjectData subjectData, ArgumentList args, String label) {
        String collection = args.get(0);
        String name = args.get(1);
        ImmutableContextSet contextSet = args.getContextOrEmpty(2);

        LPPermissionService service = subjectData.getParentSubject().getService();
        if (service.getLoadedCollections().keySet().stream().map(String::toLowerCase).noneMatch(s -> s.equalsIgnoreCase(collection))) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: SubjectCollection '&4" + collection + "&c' doesn't exist.");
        }

        LPSubjectCollection c = service.getCollection(collection);
        if (!c.hasRegistered(name).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: Subject '&4" + name + "&c' doesn't exist.");
        }

        LPSubject subject = c.loadSubject(name).join();

        if (subjectData.removeParent(contextSet, subject.toReference()).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "&aRemoved parent &b" + subject.getParentCollection().getIdentifier() +
                        "&a/&b" + subject.getIdentifier().getName() + "&a in context " + SpongeCommandUtils.contextToString(contextSet));
        } else {
            SpongeCommandUtils.sendPrefixed(sender, "Unable to remove parent. Are you sure the Subject has it added?");
        }
    }
}
