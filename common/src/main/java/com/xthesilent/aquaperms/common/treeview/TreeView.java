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

package com.xthesilent.aquaperms.common.treeview;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import com.xthesilent.aquaperms.common.cacheddata.type.PermissionCache;
import com.xthesilent.aquaperms.common.http.AbstractHttpClient;
import com.xthesilent.aquaperms.common.http.BytebinClient;
import com.xthesilent.aquaperms.common.http.UnsuccessfulRequestException;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.gson.GsonProvider;
import com.xthesilent.aquaperms.common.util.gson.JObject;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * A readable view of a branch of {@link TreeNode}s.
 */
public class TreeView {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    // the root of the tree
    private final String rootPosition;

    // the actual tree object
    private final ImmutableTreeNode view;

    public TreeView(PermissionRegistry source, String rootPosition) {
        if (rootPosition.isEmpty() || rootPosition.equals("*")) {
            rootPosition = ".";
        } else if (!rootPosition.equals(".") && rootPosition.endsWith(".")) {
            rootPosition = rootPosition.substring(0, rootPosition.length() - 1);
        }
        this.rootPosition = rootPosition;

        Optional<TreeNode> root = findRoot(rootPosition, source);
        this.view = root.map(TreeNode::makeImmutableCopy).orElse(null);
    }

    /**
     * Gets if this TreeView has any content.
     *
     * @return true if the treeview has data
     */
    public boolean hasData() {
        return this.view != null;
    }

    /**
     * Finds the root of the tree node at the given position
     *
     * @param source the node source
     * @return the root, if it exists
     */
    private static Optional<TreeNode> findRoot(String rootPosition, PermissionRegistry source) {
        // get the root of the permission vault
        TreeNode root = source.getRootNode();

        // just return the root
        if (rootPosition.equals(".")) {
            return Optional.of(root);
        }

        // get the parts of the node
        List<String> parts = Splitter.on('.').omitEmptyStrings().splitToList(rootPosition);

        // for each part
        for (String part : parts) {

            // check the current root has some children
            if (!root.getChildren().isPresent()) {
                return Optional.empty();
            }

            // get the current roots children
            Map<String, TreeNode> branch = root.getChildren().get();

            // get the new root
            root = branch.get(part);
            if (root == null) {
                return Optional.empty();
            }
        }

        return Optional.of(root);
    }

    /**
     * Uploads the data contained in this TreeView and returns the id.
     *
     * @param bytebin the bytebin instance to upload to
     * @param sender the sender
     * @param user the reference user, or null
     * @param checker the permission data instance to check against, or null
     * @return the id, or null
     */
    public String uploadPasteData(BytebinClient bytebin, Sender sender, User user, PermissionCache checker) throws IOException, UnsuccessfulRequestException {
        // only paste if there is actually data here
        if (!hasData()) {
            throw new IllegalStateException();
        }

        // work out the prefix to apply
        // since the view is relative, we need to prepend this to all permissions
        String prefix = this.rootPosition.equals(".") ? "" : this.rootPosition + ".";
        JsonObject jsonTree = this.view.toJson(prefix);

        JObject metadata = new JObject()
                .add("time", DATE_FORMAT.format(new Date(System.currentTimeMillis())))
                .add("root", this.rootPosition)
                .add("uploader", new JObject()
                        .add("name", sender.getNameWithLocation())
                        .add("uuid", sender.getUniqueId().toString())
                );

        JObject checks;
        if (user != null && checker != null) {
            metadata.add("referenceUser", new JObject()
                    .add("name", user.getPlainDisplayName())
                    .add("uuid", user.getUniqueId().toString())
            );

            checks = new JObject();
            for (Map.Entry<Integer, String> node : this.view.getNodeEndings()) {
                String permission = prefix + node.getValue();
                checks.add(permission, checker.checkPermission(permission, CheckOrigin.INTERNAL).result().name().toLowerCase(Locale.ROOT));
            }
        } else {
            checks = null;
        }

        JsonObject payload = new JObject()
                .add("metadata", metadata)
                .add("data", new JObject()
                        .add("tree", jsonTree)
                        .consume(obj -> {
                            if (checks != null) {
                                obj.add("checkResults", checks);
                            }
                        })
                )
                .toJson();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytesOut), StandardCharsets.UTF_8)) {
            GsonProvider.normal().toJson(payload, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytebin.postContent(bytesOut.toByteArray(), AbstractHttpClient.JSON_TYPE).key();
    }

}
