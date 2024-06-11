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

package com.xthesilent.aquaperms.sponge.service;

import com.xthesilent.aquaperms.sponge.service.model.LPPermissionDescription;
import com.xthesilent.aquaperms.sponge.service.model.LPPermissionService;
import com.xthesilent.aquaperms.sponge.service.model.LPProxiedSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubject;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectCollection;
import com.xthesilent.aquaperms.sponge.service.model.LPSubjectData;
import com.xthesilent.aquaperms.sponge.service.proxy.api8.PermissionDescriptionProxy;
import com.xthesilent.aquaperms.sponge.service.proxy.api8.PermissionServiceProxy;
import com.xthesilent.aquaperms.sponge.service.proxy.api8.SubjectCollectionProxy;
import com.xthesilent.aquaperms.sponge.service.proxy.api8.SubjectDataProxy;
import com.xthesilent.aquaperms.sponge.service.proxy.api8.SubjectProxy;
import com.aquasplashmc.api.model.data.DataType;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;

/**
 * Provides proxy instances which implement the SpongeAPI using the AquaPerms model.
 */
public final class ProxyFactory {
    private ProxyFactory() {}

    public static PermissionAndContextService toSponge(LPPermissionService luckPerms) {
        return new PermissionServiceProxy(luckPerms);
    }

    public static SubjectCollection toSponge(LPSubjectCollection luckPerms) {
        return new SubjectCollectionProxy(luckPerms);
    }

    public static LPProxiedSubject toSponge(LPSubject luckPerms) {
        return new SubjectProxy(luckPerms.getService(), luckPerms.toReference());
    }

    public static SubjectData toSponge(LPSubjectData luckPerms) {
        LPSubject parentSubject = luckPerms.getParentSubject();
        return new SubjectDataProxy(parentSubject.getService(), parentSubject.toReference(), luckPerms.getType() == DataType.NORMAL);
    }

    public static PermissionDescription toSponge(LPPermissionDescription luckPerms) {
        return new PermissionDescriptionProxy(luckPerms.getService(), luckPerms);
    }

}
