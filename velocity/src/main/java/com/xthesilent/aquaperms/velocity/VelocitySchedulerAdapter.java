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

package com.xthesilent.aquaperms.velocity;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.xthesilent.aquaperms.common.plugin.scheduler.SchedulerAdapter;
import com.xthesilent.aquaperms.common.plugin.scheduler.SchedulerTask;
import com.xthesilent.aquaperms.common.util.Iterators;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class VelocitySchedulerAdapter implements SchedulerAdapter {
    private final LPVelocityBootstrap bootstrap;

    private final Executor executor;
    private final Set<ScheduledTask> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    public VelocitySchedulerAdapter(LPVelocityBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.executor = r -> bootstrap.getProxy().getScheduler().buildTask(bootstrap, r).schedule();
    }

    @Override
    public Executor async() {
        return this.executor;
    }

    @Override
    public Executor sync() {
        return this.executor;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        ScheduledTask t = this.bootstrap.getProxy().getScheduler().buildTask(this.bootstrap, task)
                .delay((int) delay, unit)
                .schedule();

        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        ScheduledTask t = this.bootstrap.getProxy().getScheduler().buildTask(this.bootstrap, task)
                .delay((int) interval, unit)
                .repeat((int) interval, unit)
                .schedule();

        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public void shutdownScheduler() {
        Iterators.tryIterate(this.tasks, ScheduledTask::cancel);
    }

    @Override
    public void shutdownExecutor() {
        // do nothing
    }
}
