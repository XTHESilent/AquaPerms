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

package com.xthesilent.aquaperms.common.commands.misc;

import com.xthesilent.aquaperms.common.cacheddata.type.PermissionCache;
import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.http.UnsuccessfulRequestException;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.treeview.TreeView;
import com.xthesilent.aquaperms.common.util.Predicates;
import com.xthesilent.aquaperms.common.util.Uuids;

import java.io.IOException;
import java.util.UUID;

public class TreeCommand extends SingleCommand {
    public TreeCommand() {
        super(CommandSpec.TREE, "Tree", CommandPermission.TREE, Predicates.alwaysFalse());
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        String selection = ".";
        String player = null;

        if (!args.isEmpty()) {
            selection = args.get(0);
        }
        if (args.size() > 1) {
            player = args.get(1);
        }

        User user;
        if (player != null) {
            UUID u = Uuids.parse(player);
            if (u != null) {
                user = plugin.getUserManager().getIfLoaded(u);
            } else {
                user = plugin.getUserManager().getByUsername(player);
            }

            if (user == null) {
                Message.USER_NOT_ONLINE.send(sender, player);
                return;
            }
        } else {
            user = null;
        }

        TreeView view = new TreeView(plugin.getPermissionRegistry(), selection);
        if (!view.hasData()) {
            Message.TREE_EMPTY.send(sender);
            return;
        }

        Message.TREE_UPLOAD_START.send(sender);
        PermissionCache permissionData = user == null ? null : user.getCachedData().getPermissionData(user.getQueryOptions());

        String id;
        try {
            id = view.uploadPasteData(plugin.getBytebin(), sender, user, permissionData);
        } catch (UnsuccessfulRequestException e) {
            Message.GENERIC_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
            return;
        } catch (IOException e) {
            plugin.getLogger().warn("Error uploading data to bytebin", e);
            Message.GENERIC_HTTP_UNKNOWN_FAILURE.send(sender);
            return;
        }

        String url = plugin.getConfiguration().get(ConfigKeys.TREE_VIEWER_URL_PATTERN) + id;
        Message.TREE_URL.send(sender, url);
    }
}
