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

package com.xthesilent.aquaperms.common.node.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xthesilent.aquaperms.common.context.serializer.ContextSetJsonSerializer;
import com.xthesilent.aquaperms.common.node.factory.NodeBuilders;
import com.aquasplashmc.api.node.Node;
import com.aquasplashmc.api.node.NodeBuilder;
import com.aquasplashmc.api.node.metadata.types.InheritanceOriginMetadata;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NodeJsonSerializer {

    private NodeJsonSerializer() {

    }

    public static JsonObject serializeNode(Node node, boolean includeInheritanceOrigin) {
        JsonObject attributes = new JsonObject();

        attributes.addProperty("type", node.getType().name().toLowerCase(Locale.ROOT));
        attributes.addProperty("key", node.getKey());
        attributes.addProperty("value", node.getValue());

        Instant expiry = node.getExpiry();
        if (expiry != null) {
            attributes.addProperty("expiry", expiry.getEpochSecond());
        }

        if (!node.getContexts().isEmpty()) {
            attributes.add("context", ContextSetJsonSerializer.serialize(node.getContexts()));
        }

        if (includeInheritanceOrigin) {
            InheritanceOriginMetadata origin = node.getMetadata(InheritanceOriginMetadata.KEY).orElse(null);
            if (origin != null) {
                JsonObject metadata = new JsonObject();
                metadata.add("inheritanceOrigin", serializeInheritanceOrigin(origin));
                attributes.add("metadata", metadata);
            }
        }

        return attributes;
    }

    private static JsonObject serializeInheritanceOrigin(InheritanceOriginMetadata origin) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", origin.getOrigin().getType());
        obj.addProperty("name", origin.getOrigin().getName());
        return obj;
    }

    public static JsonArray serializeNodes(Collection<Node> nodes) {
        JsonArray arr = new JsonArray();
        for (Node node : nodes) {
            arr.add(serializeNode(node, false));
        }
        return arr;
    }

    public static Node deserializeNode(JsonElement ent) {
        JsonObject attributes = ent.getAsJsonObject();

        String key = attributes.get("key").getAsString();

        if (key.isEmpty()) {
            return null; // skip
        }

        NodeBuilder<?, ?> builder = NodeBuilders.determineMostApplicable(key);

        boolean value = attributes.get("value").getAsBoolean();
        builder.value(value);

        if (attributes.has("expiry")) {
            builder.expiry(attributes.get("expiry").getAsLong());
        }

        if (attributes.has("context")) {
            builder.context(ContextSetJsonSerializer.deserialize(attributes.get("context")));
        }

        return builder.build();
    }

    public static Set<Node> deserializeNodes(JsonArray arr) {
        Set<Node> nodes = new HashSet<>();
        for (JsonElement ent : arr) {
            Node node = deserializeNode(ent);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }
}
