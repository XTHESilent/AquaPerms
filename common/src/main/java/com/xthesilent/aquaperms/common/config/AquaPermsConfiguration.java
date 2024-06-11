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

package com.xthesilent.aquaperms.common.config;

import com.xthesilent.aquaperms.common.config.generic.KeyedConfiguration;
import com.xthesilent.aquaperms.common.config.generic.adapter.ConfigurationAdapter;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;

public class AquaPermsConfiguration extends KeyedConfiguration {
    private final AquaPermsPlugin plugin;
    private final ContextsFile contextsFile;

    public AquaPermsConfiguration(AquaPermsPlugin plugin, ConfigurationAdapter adapter) {
        super(adapter, ConfigKeys.getKeys());
        this.plugin = plugin;
        this.contextsFile = new ContextsFile(this);

        init();
    }

    @Override
    protected void load(boolean initial) {
        super.load(initial);
        this.contextsFile.load();
    }

    @Override
    public void reload() {
        super.reload();
        getPlugin().getEventDispatcher().dispatchConfigReload();
    }

    public ContextsFile getContextsFile() {
        return this.contextsFile;
    }

    public AquaPermsPlugin getPlugin() {
        return this.plugin;
    }
}
