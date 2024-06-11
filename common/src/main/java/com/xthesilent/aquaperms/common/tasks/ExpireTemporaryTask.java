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

package com.xthesilent.aquaperms.common.tasks;

import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;

public class ExpireTemporaryTask implements Runnable {
    private final AquaPermsPlugin plugin;

    public ExpireTemporaryTask(AquaPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean groupChanges = false;
        for (Group group : this.plugin.getGroupManager().getAll().values()) {
            if (group.auditTemporaryNodes()) {
                this.plugin.getStorage().saveGroup(group);
                groupChanges = true;
            }
        }

        for (User user : this.plugin.getUserManager().getAll().values()) {
            if (user.auditTemporaryNodes()) {
                this.plugin.getStorage().saveUser(user);
            }
        }

        if (groupChanges) {
            this.plugin.getGroupManager().invalidateAllGroupCaches();
            this.plugin.getUserManager().invalidateAllUserCaches();
        }
    }

}