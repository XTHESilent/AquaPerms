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

package com.xthesilent.aquaperms.common.verbose;

import com.xthesilent.aquaperms.common.cacheddata.result.StringResult;
import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.xthesilent.aquaperms.common.verbose.event.MetaCheckEvent;
import com.xthesilent.aquaperms.common.verbose.event.PermissionCheckEvent;
import com.aquasplashmc.api.util.Tristate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VerboseFilterTest {

    @Test
    public void testAcceptAll() {
        VerboseFilter filter = VerboseFilter.acceptAll();
        assertTrue(filter.isBlank());
    }

    @ParameterizedTest
    @CsvSource({
            "aquaperms, true",
            "aquaperms.user, true",
            "aquaperms.group, false",
            "Player1, true",
            "Player2, false",
            "aquaperms & Player1, true",
            "permission & aquaperms & Player1, true",
            "permission & aquaperms & Player1 & true, true",
            "aquaperms & Player2, false",
            "aquaperms | test, true"
    })
    public void testPermissionEvent(String expression, boolean expected) throws InvalidFilterException {
        VerboseFilter filter = VerboseFilter.compile(expression);
        assertFalse(filter.isBlank());

        PermissionCheckEvent relevantEvent = new PermissionCheckEvent(
                CheckOrigin.INTERNAL,
                VerboseCheckTarget.of(VerboseCheckTarget.USER_TYPE, "Player1"),
                QueryOptionsImpl.DEFAULT_CONTEXTUAL,
                System.currentTimeMillis(),
                new Throwable(),
                "test",
                "aquaperms.user.parent.info",
                TristateResult.forMonitoredResult(Tristate.TRUE)
        );

        PermissionCheckEvent nonRelevantEvent = new PermissionCheckEvent(
                CheckOrigin.INTERNAL,
                VerboseCheckTarget.of(VerboseCheckTarget.USER_TYPE, "aaaaaaa"),
                QueryOptionsImpl.DEFAULT_CONTEXTUAL,
                System.currentTimeMillis(),
                new Throwable(),
                "test",
                "aaaaaaaaa",
                TristateResult.forMonitoredResult(Tristate.FALSE)
        );

        assertEquals(expected, filter.evaluate(relevantEvent));
        assertFalse(filter.evaluate(nonRelevantEvent));
    }

    @ParameterizedTest
    @CsvSource({
            "nametags, true",
            "nametags.nametag, true",
            "nametags.other, false",
            "Player1, true",
            "Player2, false",
            "nametags & Player1, true",
            "meta & nametags & Player1, true",
            "meta & nametags & Player1 & admin, true",
            "nametags & Player2, false",
            "nametags | test, true"
    })
    public void testMetaEvent(String expression, boolean expected) throws InvalidFilterException {
        VerboseFilter filter = VerboseFilter.compile(expression);
        assertFalse(filter.isBlank());

        MetaCheckEvent relevantEvent = new MetaCheckEvent(
                CheckOrigin.INTERNAL,
                VerboseCheckTarget.of(VerboseCheckTarget.USER_TYPE, "Player1"),
                QueryOptionsImpl.DEFAULT_CONTEXTUAL,
                System.currentTimeMillis(),
                new Throwable(),
                "test",
                "nametags.nametag",
                StringResult.of("ADMIN")
        );

        MetaCheckEvent nonRelevantEvent = new MetaCheckEvent(
                CheckOrigin.INTERNAL,
                VerboseCheckTarget.of(VerboseCheckTarget.USER_TYPE, "aaaaaaa"),
                QueryOptionsImpl.DEFAULT_CONTEXTUAL,
                System.currentTimeMillis(),
                new Throwable(),
                "test",
                "aaaaaaaaa",
                StringResult.of("aaaaaa")
        );

        assertEquals(expected, filter.evaluate(relevantEvent));
        assertFalse(filter.evaluate(nonRelevantEvent));
    }

}
