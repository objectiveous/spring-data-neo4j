/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class OneToNRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {
	
	public OneToNRelationshipFieldAccessorFactory(Neo4jTemplate template) {
		super(template);
	}

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        if (!property.isRelationship()) return false;
        final RelationshipInfo info = property.getRelationshipInfo();
        return info.isMultiple() && info.targetsNodes() && !info.isReadonly();
    }

    @Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
        final Class<?> targetType = relationshipInfo.getTargetType().getType();
        return new OneToNRelationshipFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), targetType, template,property);
	}

	public static class OneToNRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor {

	    public OneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> elementClass, final Neo4jTemplate template, Neo4jPersistentProperty property) {
	        super(elementClass, template, direction, type,property);
	    }

	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
	        final Node node = checkUnderlyingState(entity);
	        if (newVal == null) {
/* null should not remove existing relationships but leave them alone
	            removeMissingRelationships(node, Collections.<Node>emptySet());
*/
	            return null;
	        }
	        final Set<Node> targetNodes = createSetOfTargetNodes(newVal);
	        removeMissingRelationships(node, targetNodes);
	        createAddedRelationships(node, targetNodes);
	        return createManagedSet(entity, (Set<?>) newVal, updateMappingPolicy(mappingPolicy));
	    }

        @Override
	    public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
	        checkUnderlyingState(entity);
            final MappingPolicy currentPolicy = updateMappingPolicy(mappingPolicy);
            final Set<?> result = createEntitySetFromRelationshipEndNodes(entity, currentPolicy);
	        return doReturn(createManagedSet(entity, result, currentPolicy));
	    }

        @Override
        public Object getDefaultValue() {
            return new HashSet();
        }
    }
}
