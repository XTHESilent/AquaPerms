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

package com.xthesilent.aquaperms.sponge.loader;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.xthesilent.aquaperms.common.loader.JarInJarClassLoader;
import com.xthesilent.aquaperms.common.loader.LoaderBootstrap;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.function.Supplier;

@Plugin("aquaperms")
public class SpongeLoaderPlugin implements Supplier<Injector> {
    private static final String JAR_NAME = "aquaperms-sponge.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.xthesilent.aquaperms.sponge.LPSpongeBootstrap";

    private final LoaderBootstrap plugin;
    private final Injector injector;

    @Inject
    public SpongeLoaderPlugin(Injector injector) {
        this.injector = injector;

        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, Supplier.class, this);
    }

    @Override
    public Injector get() {
        return this.injector;
    }

    @Listener(order = Order.FIRST)
    public void onEnable(ConstructPluginEvent event) {
        this.plugin.onLoad();
        this.plugin.onEnable();
    }

    @Listener
    public void onDisable(StoppingEngineEvent<Server> event) {
        this.plugin.onDisable();
    }

}
