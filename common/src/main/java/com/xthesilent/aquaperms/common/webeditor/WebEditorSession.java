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

package com.xthesilent.aquaperms.common.webeditor;

import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.http.AbstractHttpClient;
import com.xthesilent.aquaperms.common.http.UnsuccessfulRequestException;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.model.Group;
import com.xthesilent.aquaperms.common.model.PermissionHolder;
import com.xthesilent.aquaperms.common.model.PermissionHolderIdentifier;
import com.xthesilent.aquaperms.common.model.Track;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.webeditor.socket.WebEditorSocket;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Encapsulates a session with the web editor.
 *
 * <p>A session is tied to a specific user, and can comprise of multiple requests to and
 * responses from the web editor.</p>
 */
public class WebEditorSession {

    public static WebEditorSession create(List<PermissionHolder> holders, List<Track> tracks, Sender sender, String cmdLabel, AquaPermsPlugin plugin) {
        WebEditorRequest initialRequest = WebEditorRequest.generate(holders, tracks, sender, cmdLabel, plugin);
        return new WebEditorSession(initialRequest, plugin, sender, cmdLabel);
    }

    private WebEditorRequest initialRequest;

    private final AquaPermsPlugin plugin;
    private final Sender sender;
    private final String cmdLabel;

    private final Set<PermissionHolderIdentifier> holders;
    private final Set<String> tracks;

    private WebEditorSocket socket = null;

    public WebEditorSession(WebEditorRequest initialRequest, AquaPermsPlugin plugin, Sender sender, String cmdLabel) {
        this.initialRequest = initialRequest;
        this.plugin = plugin;
        this.sender = sender;
        this.cmdLabel = cmdLabel;

        this.holders = new LinkedHashSet<>(initialRequest.getHolders().keySet());
        this.tracks = new LinkedHashSet<>(initialRequest.getTracks().keySet());
    }

    public String open() {
        createSocket();
        return createInitialSession();
    }

    private void createSocket() {
        try {
            // create and connect to a socket
            WebEditorSocket socket = new WebEditorSocket(this.plugin, this.sender, this);
            socket.initialize(this.plugin.getBytesocks());
            socket.waitForConnect(5, TimeUnit.SECONDS);

            this.socket = socket;
            this.plugin.getWebEditorStore().sockets().putSocket(this.sender, this.socket);
        } catch (Exception e) {
            if (!ignoreSocketConnectError(e)) {
                this.plugin.getLogger().warn("Unable to establish socket connection", e);
            }
        }
    }

    private static boolean ignoreSocketConnectError(Exception e) {
        if (e instanceof UnsuccessfulRequestException) {
            UnsuccessfulRequestException req = (UnsuccessfulRequestException) e;
            int code = req.getResponse().code();

            // 502 - bad gateway / 503 - service unavailable
            // probably means the socket service is offline, that's ok, no need to send a warning
            return code == 502 || code == 503;
        }

        return false;
    }

    private String createInitialSession() {
        Objects.requireNonNull(this.initialRequest);

        WebEditorRequest request = this.initialRequest;
        this.initialRequest = null;

        if (this.socket != null) {
            this.socket.appendDetailToRequest(request);
        }

        String id = uploadRequestData(request);
        if (id == null) {
            return null;
        }

        // form a url for the editor
        String url = this.plugin.getConfiguration().get(ConfigKeys.WEB_EDITOR_URL_PATTERN) + id;
        Message.EDITOR_URL.send(this.sender, url);

        // schedule socket close
        if (this.socket != null) {
            this.socket.scheduleCleanupIfUnused();
        }

        return id;
    }

    public WebEditorSocket getSocket() {
        return this.socket;
    }

    public void includeCreatedGroup(Group group) {
        this.holders.add(group.getIdentifier());
    }

    public void includeCreatedTrack(Track track) {
        this.tracks.add(track.getName());
    }

    public void excludeDeletedUser(User user) {
        this.holders.remove(user.getIdentifier());
    }

    public void excludeDeletedGroup(Group group) {
        this.holders.remove(group.getIdentifier());
    }

    public void excludeDeletedTrack(Track track) {
        this.tracks.remove(track.getName());
    }

    public String createFollowUpSession() {
        List<PermissionHolder> holders = this.holders.stream()
                .map(id -> {
                    switch (id.getType()) {
                        case PermissionHolderIdentifier.USER_TYPE:
                            return this.plugin.getStorage().loadUser(UUID.fromString(id.getName()), null);
                        case PermissionHolderIdentifier.GROUP_TYPE:
                            return this.plugin.getStorage().loadGroup(id.getName()).thenApply(o -> o.orElse(null));
                        default:
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Track> tracks = this.tracks.stream()
                .map(id -> this.plugin.getStorage().loadTrack(id).thenApply(o -> o.orElse(null)))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return uploadRequestData(WebEditorRequest.generate(holders, tracks, this.sender, this.cmdLabel, this.plugin));
    }

    public String getCommandLabel() {
        return this.cmdLabel;
    }

    private String uploadRequestData(WebEditorRequest request) {
        byte[] requestBuf = request.encode();

        String pasteId;
        try {
            pasteId = this.plugin.getBytebin().postContent(requestBuf, AbstractHttpClient.JSON_TYPE, "editor").key();
        } catch (UnsuccessfulRequestException e) {
            Message.EDITOR_HTTP_REQUEST_FAILURE.send(this.sender, e.getResponse().code(), e.getResponse().message());
            return null;
        } catch (IOException e) {
            new RuntimeException("Error uploading data to bytebin", e).printStackTrace();
            Message.EDITOR_HTTP_UNKNOWN_FAILURE.send(this.sender);
            return null;
        }

        this.plugin.getWebEditorStore().sessions().addNewSession(pasteId, request);
        return pasteId;
    }


}
