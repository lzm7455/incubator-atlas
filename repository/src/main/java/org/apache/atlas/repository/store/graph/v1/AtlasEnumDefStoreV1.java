/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.store.graph.v1;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumDefs;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.AtlasEnumDefStore;
import org.apache.atlas.repository.util.FilterUtil;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * EnumDef store in v1 format.
 */
public class AtlasEnumDefStoreV1 implements AtlasEnumDefStore {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasEnumDefStoreV1.class);

    private final AtlasTypeDefGraphStoreV1 typeDefStore;

    public AtlasEnumDefStoreV1(AtlasTypeDefGraphStoreV1 typeDefStore) {
        super();

        this.typeDefStore = typeDefStore;
    }

    @Override
    public AtlasEnumDef create(AtlasEnumDef enumDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.create({})", enumDef);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByName(enumDef.getName());

        if (vertex != null) {
            throw new AtlasBaseException(enumDef.getName() + ": type already exists");
        }

        vertex = typeDefStore.createTypeVertex(enumDef);

        toVertex(enumDef, vertex);

        AtlasEnumDef ret = toEnumDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.create({}): {}", enumDef, ret);
        }

        return ret;
    }

    @Override
    public List<AtlasEnumDef> create(List<AtlasEnumDef> atlasEnumDefs) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.create({})", atlasEnumDefs);
        }
        List<AtlasEnumDef> enumDefList = new LinkedList<>();
        for (AtlasEnumDef enumDef : atlasEnumDefs) {
            try {
                AtlasEnumDef atlasEnumDef = create(enumDef);
                enumDefList.add(atlasEnumDef);
            } catch (AtlasBaseException baseException) {
                LOG.error("Failed to create {}", enumDef);
                LOG.error("Exception: {}", baseException);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.create({}, {})", atlasEnumDefs, enumDefList);
        }
        return enumDefList;
    }

    @Override
    public List<AtlasEnumDef> getAll() throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.getAll()");
        }

        List<AtlasEnumDef> enumDefs = new LinkedList<>();
        Iterator<AtlasVertex> verticesByCategory = typeDefStore.findTypeVerticesByCategory(TypeCategory.ENUM);
        while (verticesByCategory.hasNext()) {
            AtlasEnumDef enumDef = toEnumDef(verticesByCategory.next());
            enumDefs.add(enumDef);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.getAll()");
        }
        return enumDefs;
    }

    @Override
    public AtlasEnumDef getByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.getByName({})", name);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with name " + name);
        }

        vertex.getProperty(Constants.TYPE_CATEGORY_PROPERTY_KEY, TypeCategory.class);

        AtlasEnumDef ret = toEnumDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.getByName({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public AtlasEnumDef getByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.getByGuid({})", guid);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with guid " + guid);
        }

        AtlasEnumDef ret = toEnumDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.getByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public AtlasEnumDef updateByName(String name, AtlasEnumDef enumDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.updateByName({}, {})", name, enumDef);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with name " + name);
        }

        toVertex(enumDef, vertex);

        AtlasEnumDef ret = toEnumDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.updateByName({}, {}): {}", name, enumDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasEnumDef updateByGuid(String guid, AtlasEnumDef enumDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.updateByGuid({})", guid);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with guid " + guid);
        }

        toVertex(enumDef, vertex);

        AtlasEnumDef ret = toEnumDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.updateByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public List<AtlasEnumDef> update(List<AtlasEnumDef> enumDefs) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.update({})", enumDefs);
        }

        List<AtlasEnumDef> updatedEnumDefs = new ArrayList<>();

        for (AtlasEnumDef enumDef : enumDefs) {
            try {
                AtlasEnumDef updatedDef = updateByName(enumDef.getName(), enumDef);
                updatedEnumDefs.add(updatedDef);
            } catch (AtlasBaseException ex) {
                LOG.error("Failed to update {}", enumDef);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.update({}): {}", enumDefs, updatedEnumDefs);
        }

        return updatedEnumDefs;
    }

    @Override
    public void deleteByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.deleteByName({})", name);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with name " + name);
        }

        typeDefStore.deleteTypeVertex(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.deleteByName({})", name);
        }
    }

    @Override
    public void deleteByNames(List<String> names) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.deleteByNames({})", names);
        }

        for (String name : names) {
            try {
                deleteByName(name);
            } catch (AtlasBaseException ex) {
                LOG.error("Failed to delete {}", name);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.deleteByName({})", names);
        }
    }

    @Override
    public void deleteByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.deleteByGuid({})", guid);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.ENUM);

        if (vertex == null) {
            throw new AtlasBaseException("no enumdef exists with guid " + guid);
        }

        typeDefStore.deleteTypeVertex(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.deleteByGuid({})", guid);
        }
    }

    @Override
    public void deleteByGuids(List<String> guids) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.deleteByGuids({})", guids);
        }

        for (String guid : guids) {
            try {
                deleteByGuid(guid);
            } catch (AtlasBaseException ex) {
                LOG.error("Failed to delete {}", guid);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.deleteByGuids({})", guids);
        }
    }

    @Override
    public AtlasEnumDefs search(SearchFilter filter) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasEnumDefStoreV1.search({})", filter);
        }

        List<AtlasEnumDef> enumDefs = new ArrayList<AtlasEnumDef>();

        Iterator<AtlasVertex> vertices = typeDefStore.findTypeVerticesByCategory(TypeCategory.ENUM);

        while(vertices.hasNext()) {
            AtlasVertex       vertex  = vertices.next();
            AtlasEnumDef enumDef = toEnumDef(vertex);

            if (enumDef != null) {
                enumDefs.add(enumDef);
            }
        }

        if (CollectionUtils.isNotEmpty(enumDefs)) {
            CollectionUtils.filter(enumDefs, FilterUtil.getPredicateFromSearchFilter(filter));
        }

        AtlasEnumDefs ret = new AtlasEnumDefs(enumDefs);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasEnumDefStoreV1.search({}): {}", filter, ret);
        }

        return ret;
    }

    private void toVertex(AtlasEnumDef enumDef, AtlasVertex vertex) {
        List<String> values = new ArrayList<>(enumDef.getElementDefs().size());

        for (AtlasEnumElementDef element : enumDef.getElementDefs()) {
            String elemKey = AtlasGraphUtilsV1.getPropertyKey(enumDef, element.getValue());

            AtlasGraphUtilsV1.setProperty(vertex, elemKey, element.getOrdinal());

            if (StringUtils.isNoneBlank(element.getDescription())) {
                String descKey = AtlasGraphUtilsV1.getPropertyKey(elemKey, "description");

                AtlasGraphUtilsV1.setProperty(vertex, descKey, element.getDescription());
            }

            values.add(element.getValue());
        }
        AtlasGraphUtilsV1.setProperty(vertex, AtlasGraphUtilsV1.getPropertyKey(enumDef), values);
    }

    private AtlasEnumDef toEnumDef(AtlasVertex vertex) {
        AtlasEnumDef ret = null;

        if (vertex != null && typeDefStore.isTypeVertex(vertex, TypeCategory.ENUM)) {
            ret = toEnumDef(vertex, new AtlasEnumDef(), typeDefStore);
        }

        return ret;
    }

    private static AtlasEnumDef toEnumDef(AtlasVertex vertex, AtlasEnumDef enumDef, AtlasTypeDefGraphStoreV1 typeDefStore) {
        AtlasEnumDef ret = enumDef != null ? enumDef : new AtlasEnumDef();

        typeDefStore.vertexToTypeDef(vertex, ret);

        List<AtlasEnumElementDef> elements = new ArrayList<>();
        List<String> elemValues = vertex.getProperty(AtlasGraphUtilsV1.getPropertyKey(ret), List.class);
        for (String elemValue : elemValues) {
            String elemKey = AtlasGraphUtilsV1.getPropertyKey(ret, elemValue);
            String descKey = AtlasGraphUtilsV1.getPropertyKey(elemKey, "description");

            Integer ordinal = AtlasGraphUtilsV1.getProperty(vertex, elemKey, Integer.class);
            String  desc    = AtlasGraphUtilsV1.getProperty(vertex, descKey, String.class);

            elements.add(new AtlasEnumElementDef(elemValue, desc, ordinal));
        }
        ret.setElementDefs(elements);

        return ret;
    }
}