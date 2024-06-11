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

package com.xthesilent.aquaperms.sponge.service.model.persisted;

import com.xthesilent.aquaperms.sponge.service.AquaPermsService;
import com.xthesilent.aquaperms.sponge.service.model.calculated.MonitoredSubjectData;
import com.aquasplashmc.api.model.data.DataType;

/**
 * Extension of CalculatedSubjectData which persists data when modified
 */
public class PersistedSubjectData extends MonitoredSubjectData {
    private final PersistedSubject subject;
    private boolean save = true;

    public PersistedSubjectData(PersistedSubject subject, DataType type, AquaPermsService service) {
        super(subject, type, service);
        this.subject = subject;
    }

    @Override
    protected void onUpdate(boolean success) {
        if (!this.save) {
            return;
        }

        this.subject.save();
    }

    public void setSave(boolean save) {
        this.save = save;
    }
}
