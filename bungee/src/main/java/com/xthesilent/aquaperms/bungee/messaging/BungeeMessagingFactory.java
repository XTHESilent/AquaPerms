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

package com.xthesilent.aquaperms.bungee.messaging;

import com.xthesilent.aquaperms.bungee.LPBungeePlugin;
import com.xthesilent.aquaperms.common.messaging.InternalMessagingService;
import com.xthesilent.aquaperms.common.messaging.AquaPermsMessagingService;
import com.xthesilent.aquaperms.common.messaging.MessagingFactory;
import com.aquasplashmc.api.messenger.IncomingMessageConsumer;
import com.aquasplashmc.api.messenger.Messenger;
import com.aquasplashmc.api.messenger.MessengerProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BungeeMessagingFactory extends MessagingFactory<LPBungeePlugin> {
    public BungeeMessagingFactory(LPBungeePlugin plugin) {
        super(plugin);
    }

    @Override
    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("pluginmsg") || messagingType.equals("bungee")) {
            try {
                return new AquaPermsMessagingService(getPlugin(), new PluginMessageMessengerProvider());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (messagingType.equals("redisbungee")) {
            if (getPlugin().getBootstrap().getProxy().getPluginManager().getPlugin("RedisBungee") == null) {
                getPlugin().getLogger().warn("RedisBungee plugin not present.");
            } else {
                try {
                    return new AquaPermsMessagingService(getPlugin(), new RedisBungeeMessengerProvider());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return super.getServiceFor(messagingType);
    }

    private class PluginMessageMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "PluginMessage";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            PluginMessageMessenger messenger = new PluginMessageMessenger(getPlugin(), incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }

    private class RedisBungeeMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "RedisBungee";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            RedisBungeeMessenger messenger = new RedisBungeeMessenger(getPlugin(), incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }
}
