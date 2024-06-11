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

package com.xthesilent.aquaperms.common.cacheddata.type;

import com.google.common.collect.Maps;
import com.xthesilent.aquaperms.common.cacheddata.CacheMetadata;
import com.xthesilent.aquaperms.common.cacheddata.UsageTracked;
import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.calculator.CalculatorFactory;
import com.xthesilent.aquaperms.common.calculator.PermissionCalculator;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.aquasplashmc.api.cacheddata.CachedPermissionData;
import com.aquasplashmc.api.cacheddata.Result;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.query.QueryOptions;
import com.aquasplashmc.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds cached permissions data for a given context
 */
public class PermissionCache extends UsageTracked implements CachedPermissionData {

    /**
     * The query options this container is holding data for
     */
    private final QueryOptions queryOptions;

    /**
     * The raw set of permission strings.
     */
    private final Map<String, Node> permissions;

    /**
     * A string->boolean view of {@link #permissions}
     */
    private final Map<String, Boolean> permissionsView;

    /**
     * The calculator instance responsible for resolving the raw permission strings in the permission map.
     * This calculator will attempt to resolve all regex/wildcard permissions, as well as account for
     * defaults & attachment permissions (if applicable.)
     */
    private final PermissionCalculator calculator;

    public PermissionCache(QueryOptions queryOptions, CacheMetadata metadata, CalculatorFactory calculatorFactory, ConcurrentHashMap<String, Node> sourcePermissions) {
        this.queryOptions = queryOptions;
        this.permissions = sourcePermissions;
        this.permissionsView = Collections.unmodifiableMap(Maps.transformValues(this.permissions, Node::getValue));

        this.calculator = calculatorFactory.build(queryOptions, metadata);
        this.calculator.setSourcePermissions(this.permissions);
    }

    @Override
    public void invalidateCache() {
        this.calculator.invalidateCache();
    }

    public PermissionCalculator getCalculator() {
        return this.calculator;
    }

    @Override
    public @NonNull Map<String, Boolean> getPermissionMap() {
        return this.permissionsView;
    }

    public TristateResult checkPermission(String permission, CheckOrigin origin) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        return this.calculator.checkPermission(permission, origin);
    }

    @Override
    public @NonNull Result<Tristate, Node> queryPermission(@NonNull String permission) {
        return checkPermission(permission, CheckOrigin.LUCKPERMS_API);
    }

    @Override
    public @NonNull Tristate checkPermission(@NonNull String permission) {
        return checkPermission(permission, CheckOrigin.LUCKPERMS_API).result();
    }

    @Override
    public @NonNull QueryOptions getQueryOptions() {
        return this.queryOptions;
    }

}
