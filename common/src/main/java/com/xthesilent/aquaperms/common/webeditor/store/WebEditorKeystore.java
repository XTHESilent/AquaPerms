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

import com.xthesilent.aquaperms.common.context.ImmutableContextSetImpl;
import com.xthesilent.aquaperms.common.model.User;
import com.xthesilent.aquaperms.common.node.types.Meta;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.gson.GsonProvider;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.aquasplashmc.api.model.data.DataType;
import com.aquasplashmc.api.node.NodeType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class WebEditorKeystore {
    private static final String META_KEY = "lp-editor-key";

    private final Path consoleKeysPath;
    private final Set<String> trustedConsoleKeys;

    public WebEditorKeystore(Path consoleKeysPath) {
        this.consoleKeysPath = consoleKeysPath;
        this.trustedConsoleKeys = new CopyOnWriteArraySet<>();

        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the given public key has been trusted by the sender.
     *
     * @param sender    the sender
     * @param publicKey the public key
     * @return true if trusted
     */
    public boolean isTrusted(Sender sender, byte[] publicKey) {
        return isTrusted(sender, hash(publicKey));
    }

    /**
     * Checks if the given public key hash has been trusted by the sender.
     *
     * @param sender the sender
     * @param hash   the public key hash
     * @return true if trusted
     */
    public boolean isTrusted(Sender sender, String hash) {
        if (sender.isConsole()) {
            return isTrustedConsole(hash);
        } else {
            User user = sender.getPlugin().getUserManager().getIfLoaded(sender.getUniqueId());
            return user != null && isTrusted(user, hash);
        }
    }

    /**
     * Trusts the given public key for the sender.
     *
     * @param sender    the sender
     * @param publicKey the public key
     */
    public void trust(Sender sender, byte[] publicKey) {
        trust(sender, hash(publicKey));
    }

    /**
     * Trusts the given public key hash for the sender.
     *
     * @param sender the sender
     * @param hash   the public key hash
     */
    public void trust(Sender sender, String hash) {
        if (sender.isConsole()) {
            trustConsole(hash);
        } else {
            User user = sender.getPlugin().getUserManager().getIfLoaded(sender.getUniqueId());
            if (user != null) {
                trust(user, hash);
            }
        }
    }

    // console

    private boolean isTrustedConsole(String hash) {
        return this.trustedConsoleKeys.contains(hash);
    }

    private void trustConsole(String hash) {
        this.trustedConsoleKeys.add(hash);

        try {
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() throws Exception {
        if (Files.exists(this.consoleKeysPath)) {
            try (BufferedReader reader = Files.newBufferedReader(this.consoleKeysPath, StandardCharsets.UTF_8)) {
                KeystoreFile file = GsonProvider.normal().fromJson(reader, KeystoreFile.class);
                if (file != null && file.consoleKeys != null) {
                    this.trustedConsoleKeys.addAll(file.consoleKeys);
                }
            }
        }
    }

    private void save() throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(this.consoleKeysPath, StandardCharsets.UTF_8)) {
            KeystoreFile file = new KeystoreFile();
            file.consoleKeys = new ArrayList<>(this.trustedConsoleKeys);
            GsonProvider.prettyPrinting().toJson(file, writer);
        }
    }

    // users

    private boolean isTrusted(User user, String hash) {
        String key = user.getCachedData().getMetaData(QueryOptionsImpl.DEFAULT_CONTEXTUAL)
                .getMetaValue(META_KEY, CheckOrigin.INTERNAL).result();

        if (key == null || key.isEmpty()) {
            return false;
        }

        return hash.equals(key);
    }

    private void trust(User user, String hash) {
        user.removeIf(DataType.NORMAL, ImmutableContextSetImpl.EMPTY, NodeType.META.predicate(mn -> mn.getMetaKey().equals(META_KEY)), false);
        user.setNode(DataType.NORMAL, Meta.builder(META_KEY, hash).build(), false);

        user.getPlugin().getStorage().saveUser(user).join();
    }

    private static String hash(byte[] buf) {
        byte[] digest = createDigest().digest(buf);
        return Base64.getEncoder().encodeToString(digest);
    }

    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"FieldMayBeFinal", "unused"})
    private static class KeystoreFile {
        private String _comment = "This file stores a list of trusted editor public keys";
        private List<String> consoleKeys = null;
    }
}
