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

package com.xthesilent.aquaperms.common.webeditor.store;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.xthesilent.aquaperms.common.config.ConfigKeys;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.webeditor.socket.CryptographyUtils;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

/**
 * Contains a store of known web editor sessions and provides a lookup function for
 * trusted editor public keys.
 */
@SuppressWarnings("Guava")
public class WebEditorStore {
    private final WebEditorSessionMap sessions;
    private final WebEditorSocketMap sockets;
    private final WebEditorKeystore keystore;
    private final Supplier<CompletableFuture<KeyPair>> keyPair;

    public WebEditorStore(AquaPermsPlugin plugin) {
        this.sessions = new WebEditorSessionMap();
        this.sockets = new WebEditorSocketMap();
        this.keystore = new WebEditorKeystore(plugin.getBootstrap().getConfigDirectory().resolve("editor-keystore.json"));

        Supplier<CompletableFuture<KeyPair>> keyPair = () -> CompletableFuture.supplyAsync(
                CryptographyUtils::generateKeyPair,
                plugin.getBootstrap().getScheduler().async()
        );

        if (plugin.getConfiguration().get(ConfigKeys.EDITOR_LAZILY_GENERATE_KEY)) {
            this.keyPair = Suppliers.memoize(keyPair);
        } else {
            CompletableFuture<KeyPair> future = keyPair.get();
            this.keyPair = () -> future;
        }
    }

    public WebEditorSessionMap sessions() {
        return this.sessions;
    }

    public WebEditorSocketMap sockets() {
        return this.sockets;
    }

    public WebEditorKeystore keystore() {
        return this.keystore;
    }

    public KeyPair keyPair() {
        return this.keyPair.get().join();
    }

}
