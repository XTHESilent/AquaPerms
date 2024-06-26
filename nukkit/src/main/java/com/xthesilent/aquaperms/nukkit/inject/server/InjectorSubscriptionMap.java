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

package com.xthesilent.aquaperms.nukkit.inject.server;

import cn.nukkit.Server;
import cn.nukkit.permission.Permissible;
import cn.nukkit.plugin.PluginManager;
import com.xthesilent.aquaperms.nukkit.LPNukkitPlugin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Injects a {@link AquaPermsSubscriptionMap} into the {@link PluginManager}.
 */
public class InjectorSubscriptionMap implements Runnable {
    private static final Field PERM_SUBS_FIELD;

    static {
        Field permSubsField = null;
        try {
            permSubsField = PluginManager.class.getDeclaredField("permSubs");
            permSubsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        PERM_SUBS_FIELD = permSubsField;
    }

    private final LPNukkitPlugin plugin;

    public InjectorSubscriptionMap(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            AquaPermsSubscriptionMap subscriptionMap = inject();
            if (subscriptionMap != null) {
                this.plugin.setSubscriptionMap(subscriptionMap);
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst injecting AquaPerms Permission Subscription map.", e);
        }
    }

    private AquaPermsSubscriptionMap inject() throws Exception {
        Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");
        PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();

        Object map = PERM_SUBS_FIELD.get(pluginManager);
        if (map instanceof AquaPermsSubscriptionMap) {
            if (((AquaPermsSubscriptionMap) map).plugin == this.plugin) {
                return null;
            }

            map = ((AquaPermsSubscriptionMap) map).detach();
        }

        //noinspection unchecked
        Map<String, Set<Permissible>> castedMap = (Map<String, Set<Permissible>>) map;

        // make a new subscription map & inject it
        AquaPermsSubscriptionMap newMap = new AquaPermsSubscriptionMap(this.plugin, castedMap);
        PERM_SUBS_FIELD.set(pluginManager, newMap);
        return newMap;
    }

    public static void uninject() {
        try {
            Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");
            PluginManager pluginManager = Server.getInstance().getPluginManager();

            Object map = PERM_SUBS_FIELD.get(pluginManager);
            if (map instanceof AquaPermsSubscriptionMap) {
                AquaPermsSubscriptionMap lpMap = (AquaPermsSubscriptionMap) map;
                PERM_SUBS_FIELD.set(pluginManager, lpMap.detach());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
