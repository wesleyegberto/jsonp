/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.json;

import org.glassfish.json.api.BufferPool;

import javax.json.JsonArrayBuilder;
import javax.json.*;
import javax.json.serialization.NullValueSerializationBehavior;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * JsonObjectBuilder implementation
 *
 * @author Jitendra Kotamraju
 * @author Kin-man Chung
 */
class JsonObjectBuilderImpl implements JsonObjectBuilder {
	private NullValueSerializationBehavior nullValueSerializationBehavior = NullValueSerializationBehavior.FORBIDDEN;
	
    private Map<String, JsonValue> valueMap;
    private final BufferPool bufferPool;
	
    JsonObjectBuilderImpl(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    JsonObjectBuilderImpl(JsonObject object, BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        valueMap = new LinkedHashMap<>();
        valueMap.putAll(object);
    }

    JsonObjectBuilderImpl(Map<String, Object> map, BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        valueMap = new LinkedHashMap<>();
        populate(map);
    }

    @Override
	public JsonObjectBuilder forbidNull() {
    	this.nullValueSerializationBehavior = NullValueSerializationBehavior.FORBIDDEN;
    	return this;
	}

    @Override
	public JsonObjectBuilder serializeNull() {
    	this.nullValueSerializationBehavior = NullValueSerializationBehavior.SERIALIZE;
    	return this;
	}

    @Override
	public JsonObjectBuilder ignoreNull() {
    	this.nullValueSerializationBehavior = NullValueSerializationBehavior.IGNORE;
    	return this;
	}

    @Override
    public JsonObjectBuilder add(String name, JsonValue value) {
        validateName(name);
    	if (shouldIgnore(value)) {
    		return this;
    	}
    	if (shouldAddNull(value)) {
    		return addNull(name);
    	}
        validateValue(value);
        putValueMap(name, value);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, String value) {
        validateName(name);
        // return handleAndAddValue(name, value, JsonStringImpl::new);
    	if (shouldIgnore(value)) {
    		return this;
    	}
    	if (shouldAddNull(value)) {
    		return addNull(name);
    	}
        validateValue(value);
        putValueMap(name, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, BigInteger value) {
        validateName(name);
    	if (shouldIgnore(value)) {
    		return this;
    	}
    	if (shouldAddNull(value)) {
    		return addNull(name);
    	}
        validateValue(value);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, BigDecimal value) {
        validateName(name);
    	if (shouldIgnore(value)) {
    		return this;
    	}
    	if (shouldAddNull(value)) {
    		return addNull(name);
    	}
        validateValue(value);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, int value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, long value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, double value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, boolean value) {
        validateName(name);
        putValueMap(name, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(String name) {
        validateName(name);
        putValueMap(name, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, JsonObjectBuilder builder) {
        validateName(name);
    	if (shouldIgnore(builder)) {
    		return this;
    	}
    	if (shouldAddNull(builder)) {
    		return addNull(name);
    	}
        if (isNullForbidden() && builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_OBJECT_BUILDER_NULL());
        }
        putValueMap(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, JsonArrayBuilder builder) {
    	validateName(name);
    	if (shouldIgnore(builder)) {
    		return this;
    	}
    	if (shouldAddNull(builder)) {
    		return addNull(name);
    	}
        if (isNullForbidden() && builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_ARRAY_BUILDER_NULL());
        }
        putValueMap(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder addAll(JsonObjectBuilder builder) {
        if (builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_OBJECT_BUILDER_NULL());
        }
        if (valueMap == null) {
            this.valueMap = new LinkedHashMap<>();
        }
        this.valueMap.putAll(builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder remove(String name) {
        validateName(name);
        this.valueMap.remove(name);
        return this;
    }

    @Override
    public JsonObject build() {
        Map<String, JsonValue> snapshot = (valueMap == null)
                ? Collections.<String, JsonValue>emptyMap()
                : Collections.unmodifiableMap(valueMap);
        valueMap = null;
        return new JsonObjectImpl(snapshot, bufferPool);
    }

    private void populate(Map<String, Object> map) {
        final Set<String> fields = map.keySet();
        for (String field : fields) {
            Object value = map.get(field);
            if (value != null && value instanceof Optional) {
                ((Optional<?>) value).ifPresent(v ->
                        this.valueMap.put(field, MapUtil.handle(v, bufferPool)));
            } else {
                this.valueMap.put(field, MapUtil.handle(value, bufferPool));
            }
        }
    }

    private void putValueMap(String name, JsonValue value) {
        if (valueMap == null) {
            this.valueMap = new LinkedHashMap<>();
        }
        valueMap.put(name, value);
    }

    private void validateName(String name) {
        if (name == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_NAME_NULL());
        }
    }

    private void validateValue(Object value) {
        if (isNullForbidden() && value == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_VALUE_NULL());
        }
    }
    
    private boolean shouldIgnore(Object value) {
    	return nullValueSerializationBehavior == NullValueSerializationBehavior.IGNORE && value == null;
    }
    
    private boolean shouldAddNull(Object value) {
    	return nullValueSerializationBehavior == NullValueSerializationBehavior.SERIALIZE && value == null;
    }

	private boolean isNullForbidden() {
		return nullValueSerializationBehavior == NullValueSerializationBehavior.FORBIDDEN;
	}
    
    // TODO: Verify if we can use Lambda (thus requiring JDK >= 8)
    private <T, R extends JsonValue> JsonObjectBuilder handleAndAddValue(String name, T value, Function<T, R> valueTransformer) {
    	JsonValue valueToAdd = null;
    	
    	if (value == null) {
    		switch (nullValueSerializationBehavior) {
    			case IGNORE:
    				return this;
    			case SERIALIZE:
					valueToAdd = JsonValue.NULL;
					break;
    			default:
    				throw new NullPointerException(JsonMessages.OBJBUILDER_VALUE_NULL());
    		}
    	} else {
			valueToAdd = valueTransformer.apply(value);
    	}
    	putValueMap(name, valueToAdd);
    	return this;
    }

    private static final class JsonObjectImpl extends AbstractMap<String, JsonValue> implements JsonObject {
        private final Map<String, JsonValue> valueMap;      // unmodifiable
        private final BufferPool bufferPool;

        JsonObjectImpl(Map<String, JsonValue> valueMap, BufferPool bufferPool) {
            this.valueMap = valueMap;
            this.bufferPool = bufferPool;
        }

        @Override
        public JsonArray getJsonArray(String name) {
            return (JsonArray)get(name);
        }

        @Override
        public JsonObject getJsonObject(String name) {
            return (JsonObject)get(name);
        }

        @Override
        public JsonNumber getJsonNumber(String name) {
            return (JsonNumber)get(name);
        }

        @Override
        public JsonString getJsonString(String name) {
            return (JsonString)get(name);
        }

        @Override
        public String getString(String name) {
            return getJsonString(name).getString();
        }

        @Override
        public String getString(String name, String defaultValue) {
            try {
                return getString(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public int getInt(String name) {
            return getJsonNumber(name).intValue();
        }

        @Override
        public int getInt(String name, int defaultValue) {
            try {
                return getInt(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public boolean getBoolean(String name) {
            JsonValue value = get(name);
            if (value == null) {
                throw new NullPointerException();
            } else if (value == JsonValue.TRUE) {
                return true;
            } else if (value == JsonValue.FALSE) {
                return false;
            } else {
                throw new ClassCastException();
            }
        }

        @Override
        public boolean getBoolean(String name, boolean defaultValue) {
            try {
                return getBoolean(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public boolean isNull(String name) {
            return get(name).equals(JsonValue.NULL);
        }

        @Override
        public ValueType getValueType() {
            return ValueType.OBJECT;
        }

        @Override
        public Set<Entry<String, JsonValue>> entrySet() {
            return valueMap.entrySet();
        }

        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            try (JsonWriter jw = new JsonWriterImpl(sw, bufferPool)) {
                jw.write(this);
            }
            return sw.toString();
        }

        @Override
        public JsonObject asJsonObject() {
            return this;
        }

        @Override
        public int size() {
            return valueMap.size();
        }

        @Override
        public JsonValue get(Object key) {
            return valueMap.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return valueMap.containsKey(key);
        }
    }

}
