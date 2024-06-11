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

package com.xthesilent.aquaperms.fabric.messaging;

import com.xthesilent.aquaperms.common.messaging.InternalMessagingService;
import com.xthesilent.aquaperms.common.messaging.AquaPermsMessagingService;
import com.xthesilent.aquaperms.common.messaging.MessagingFactory;
import com.xthesilent.aquaperms.fabric.LPFabricPlugin;
import com.aquasplashmc.api.messenger.IncomingMessageConsumer;
import com.aquasplashmc.api.messenger.Messenger;
import com.aquasplashmc.api.messenger.MessengerProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FabricMessagingFactory extends MessagingFactory<LPFabricPlugin> {
    public FabricMessagingFactory(LPFabricPlugin plugin) {
        super(plugin);
    }

    @Override
    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("pluginmsg") || messagingType.equals("bungee") || messagingType.equals("velocity")) {
            try {
                return new AquaPermsMessagingService(getPlugin(), new PluginMessageMessengerProvider());
            } catch (Exception e) {
                getPlugin().getLogger().severe("Exception occurred whilst enabling messaging", e);
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

}