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

package com.xthesilent.aquaperms.common.calculator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xthesilent.aquaperms.common.cacheddata.CacheMetadata;
import com.xthesilent.aquaperms.common.cacheddata.result.TristateResult;
import com.xthesilent.aquaperms.common.calculator.processor.AbstractOverrideWildcardProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.DirectProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.PermissionProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.RegexProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.SpongeWildcardProcessor;
import com.xthesilent.aquaperms.common.calculator.processor.WildcardProcessor;
import com.xthesilent.aquaperms.common.model.HolderType;
import com.xthesilent.aquaperms.common.node.factory.NodeBuilders;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.query.QueryOptionsImpl;
import com.xthesilent.aquaperms.common.treeview.PermissionRegistry;
import com.xthesilent.aquaperms.common.verbose.VerboseCheckTarget;
import com.xthesilent.aquaperms.common.verbose.VerboseHandler;
import com.xthesilent.aquaperms.common.verbose.event.CheckOrigin;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.util.Tristate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class PermissionCalculatorTest {

    private static final CacheMetadata MOCK_METADATA = new CacheMetadata(
            HolderType.GROUP,
            VerboseCheckTarget.of(VerboseCheckTarget.GROUP_TYPE, "test"),
            QueryOptionsImpl.DEFAULT_CONTEXTUAL
    );

    private static final Map<String, Node> EXAMPLE_PERMISSIONS = ImmutableMap.<String, Boolean>builder()
            // direct
            .put("test.node1", true)
            .put("test.node2", false)

            // wildcard
            .put("one.two.three.four", true)
            .put("one.two.three.*", false)
            .put("one.two.three", true)
            .put("one.two.*", false)
            .put("one.two", true)
            .put("one.*", false)
            .put("one", true)
            .put("*", false)

            // regex
            .put("r=hello\\d+", true)
            .put("R=rege(x(es)?|xps?)[1-5]", false)

            // override
            .put("overridetest.*", true)

            .build().entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> NodeBuilders.determineMostApplicable(e.getKey()).value(e.getValue()).build()
            ));

    @Mock private AquaPermsPlugin plugin;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.plugin.getVerboseHandler()).thenReturn(mock(VerboseHandler.class));
        lenient().when(this.plugin.getPermissionRegistry()).thenReturn(mock(PermissionRegistry.class));
    }

    private PermissionCalculator createCalculator(PermissionProcessor... processors) {
        return new PermissionCalculator(this.plugin, MOCK_METADATA, ImmutableList.copyOf(processors));
    }

    @ParameterizedTest
    @CsvSource({
            "test, UNDEFINED",
            "test.node1, TRUE",
            "test.node2, FALSE"
    })
    public void testDirect(String node, Tristate expected) {
        PermissionCalculator calculator = createCalculator(new DirectProcessor());
        calculator.setSourcePermissions(EXAMPLE_PERMISSIONS);

        TristateResult result = calculator.checkPermission(node, CheckOrigin.INTERNAL);
        assertEquals(expected, result.result());
        assertNull(result.overriddenResult());

        if (expected != Tristate.UNDEFINED) {
            assertNotNull(result.node());
            assertSame(DirectProcessor.class, result.processorClass());
        } else {
            assertNull(result.node());
            assertNull(result.processorClass());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "one.two.three.four, true, direct",
            "one.two.three.test, false, wildcard",
            "one.two.three.*, false, direct",
            "one.two.three, true, direct",
            "one.two.test, false, wildcard",
            "one.two.*, false, direct",
            "one.two, true, direct",
            "one.test, false, wildcard",
            "one.*, false, direct",
            "one, true, direct",
            "test, false, wildcard",
            "*, false, direct",
    })
    public void testWildcard(String node, boolean expected, String type) {
        PermissionCalculator calculator = createCalculator(new DirectProcessor(), new WildcardProcessor());
        calculator.setSourcePermissions(EXAMPLE_PERMISSIONS);

        TristateResult result = calculator.checkPermission(node, CheckOrigin.INTERNAL);
        assertEquals(Tristate.of(expected), result.result());
        assertNull(result.overriddenResult());
        assertNotNull(result.node());

        if (type.equals("direct")) {
            assertSame(DirectProcessor.class, result.processorClass());
        } else if (type.equals("wildcard")) {
            assertSame(WildcardProcessor.class, result.processorClass());
        } else {
            throw new AssertionError();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "one, true, direct",
            "one.test, true, wildcard",
            "one.two, true, direct",
            "one.two.test, true, wildcard",
    })
    public void testSpongeWildcard(String node, boolean expected, String type) {
        PermissionCalculator calculator = createCalculator(new DirectProcessor(), new SpongeWildcardProcessor());
        calculator.setSourcePermissions(EXAMPLE_PERMISSIONS);

        TristateResult result = calculator.checkPermission(node, CheckOrigin.INTERNAL);
        assertEquals(Tristate.of(expected), result.result());
        assertNull(result.overriddenResult());
        assertNotNull(result.node());

        if (type.equals("direct")) {
            assertSame(DirectProcessor.class, result.processorClass());
        } else if (type.equals("wildcard")) {
            assertSame(SpongeWildcardProcessor.class, result.processorClass());
        } else {
            throw new AssertionError();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "hello, UNDEFINED",
            "hello1, TRUE",
            "hello123, TRUE",
            "helloo, UNDEFINED",
            "regex1, FALSE",
            "regexes2, FALSE",
            "regexp3, FALSE",
            "regexps4, FALSE",
    })
    public void testRegex(String node, Tristate expected) {
        PermissionCalculator calculator = createCalculator(new DirectProcessor(), new RegexProcessor());
        calculator.setSourcePermissions(EXAMPLE_PERMISSIONS);

        TristateResult result = calculator.checkPermission(node, CheckOrigin.INTERNAL);
        assertEquals(expected, result.result());
        assertNull(result.overriddenResult());

        if (expected != Tristate.UNDEFINED) {
            assertNotNull(result.node());
            assertSame(RegexProcessor.class, result.processorClass());
        } else {
            assertNull(result.node());
            assertNull(result.processorClass());
        }
    }

    @Test
    public void testOverrideWildcard() {
        AbstractOverrideWildcardProcessor overrideProcessor = new AbstractOverrideWildcardProcessor(true) {
            @Override
            protected TristateResult hasPermission(String permission) {
                if (permission.equals("overridetest.test")) {
                    return new TristateResult.Factory(AbstractOverrideWildcardProcessor.class)
                            .result(Tristate.FALSE);
                }
                return TristateResult.UNDEFINED;
            }
        };

        PermissionCalculator calculator = createCalculator(new DirectProcessor(), new WildcardProcessor(), overrideProcessor);
        calculator.setSourcePermissions(EXAMPLE_PERMISSIONS);

        TristateResult result = calculator.checkPermission("overridetest.test", CheckOrigin.INTERNAL);
        assertEquals(Tristate.FALSE, result.result());
        assertSame(AbstractOverrideWildcardProcessor.class, result.processorClass());

        TristateResult overriddenResult = result.overriddenResult();
        assertNotNull(overriddenResult);
        assertSame(WildcardProcessor.class, overriddenResult.processorClass());
    }

}
