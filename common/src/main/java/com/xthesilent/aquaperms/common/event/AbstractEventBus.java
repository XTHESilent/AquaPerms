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

package com.xthesilent.aquaperms.common.event;

import com.xthesilent.aquaperms.common.api.AquaPermsApiProvider;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import net.kyori.event.EventSubscriber;
import net.kyori.event.SimpleEventBus;
import com.aquasplashmc.api.event.EventBus;
import com.aquasplashmc.api.event.EventSubscription;
import com.aquasplashmc.api.event.AquaPermsEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractEventBus<P> implements EventBus, AutoCloseable {

    /**
     * The plugin instance
     */
    private final AquaPermsPlugin plugin;

    /**
     * The api provider instance
     */
    private final AquaPermsApiProvider apiProvider;

    /**
     * The delegate event bus
     */
    private final Bus bus = new Bus();

    protected AbstractEventBus(AquaPermsPlugin plugin, AquaPermsApiProvider apiProvider) {
        this.plugin = plugin;
        this.apiProvider = apiProvider;
    }

    public AquaPermsPlugin getPlugin() {
        return this.plugin;
    }

    public AquaPermsApiProvider getApiProvider() {
        return this.apiProvider;
    }

    /**
     * Checks that the given plugin object is a valid plugin instance for the platform
     *
     * @param plugin the object
     * @return a plugin
     * @throws IllegalArgumentException if the plugin is invalid
     */
    protected abstract P checkPlugin(Object plugin) throws IllegalArgumentException;

    public void post(AquaPermsEvent event) {
        this.bus.post(event);
    }

    public boolean shouldPost(Class<? extends AquaPermsEvent> eventClass) {
        return this.bus.hasSubscribers(eventClass);
    }

    public void subscribe(AquaPermsEventListener listener) {
        listener.bind(this);
    }

    @Override
    public <T extends AquaPermsEvent> @NonNull EventSubscription<T> subscribe(@NonNull Class<T> eventClass, @NonNull Consumer<? super T> handler) {
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        return registerSubscription(eventClass, handler, null);
    }

    @Override
    public <T extends AquaPermsEvent> @NonNull EventSubscription<T> subscribe(Object plugin, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> handler) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        return registerSubscription(eventClass, handler, checkPlugin(plugin));
    }

    private <T extends AquaPermsEvent> EventSubscription<T> registerSubscription(Class<T> eventClass, Consumer<? super T> handler, Object plugin) {
        if (!eventClass.isInterface()) {
            throw new IllegalArgumentException("class " + eventClass + " is not an interface");
        }
        if (!AquaPermsEvent.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("class " + eventClass.getName() + " does not implement AquaPermsEvent");
        }

        AquaPermsEventSubscription<T> eventHandler = new AquaPermsEventSubscription<>(this, eventClass, handler, plugin);
        this.bus.register(eventClass, eventHandler);

        return eventHandler;
    }

    @Override
    public <T extends AquaPermsEvent> @NonNull Set<EventSubscription<T>> getSubscriptions(@NonNull Class<T> eventClass) {
        return this.bus.getHandlers(eventClass);
    }

    /**
     * Removes a specific handler from the bus
     *
     * @param handler the handler to remove
     */
    public void unregisterHandler(AquaPermsEventSubscription<?> handler) {
        this.bus.unregister(handler);
    }

    /**
     * Removes all handlers for a specific plugin
     *
     * @param plugin the plugin
     */
    protected void unregisterHandlers(P plugin) {
        this.bus.unregister(sub -> ((AquaPermsEventSubscription<?>) sub).getPlugin() == plugin);
    }

    @Override
    public void close() {
        this.bus.unregisterAll();
    }

    private static final class Bus extends SimpleEventBus<AquaPermsEvent> {
        Bus() {
            super(AquaPermsEvent.class);
        }

        @Override
        protected boolean shouldPost(@NonNull AquaPermsEvent event, @NonNull EventSubscriber<?> subscriber) {
            return true;
        }

        public <T extends AquaPermsEvent> Set<EventSubscription<T>> getHandlers(Class<T> eventClass) {
            //noinspection unchecked
            return super.subscribers().values().stream()
                    .filter(s -> s instanceof EventSubscription && ((EventSubscription<?>) s).getEventClass().isAssignableFrom(eventClass))
                    .map(s -> (EventSubscription<T>) s)
                    .collect(Collectors.toSet());
        }
    }
}
