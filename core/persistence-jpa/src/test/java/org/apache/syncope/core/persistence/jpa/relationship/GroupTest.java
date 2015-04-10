/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.relationship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class GroupTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test(expected = InvalidEntityException.class)
    public void saveWithTwoOwners() {
        Group root = groupDAO.find("root", null);
        assertNotNull("did not find expected group", root);

        User user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);

        Group group = entityFactory.newEntity(Group.class);
        group.setName("error");
        group.setUserOwner(user);
        group.setGroupOwner(root);

        groupDAO.save(group);
    }

    @Test
    public void findByOwner() {
        Group group = groupDAO.find(6L);
        assertNotNull("did not find expected group", group);

        User user = userDAO.find(5L);
        assertNotNull("did not find expected user", user);

        assertEquals(user, group.getUserOwner());

        Group child1 = groupDAO.find(7L);
        assertNotNull(child1);
        assertEquals(group, child1.getParent());

        Group child2 = groupDAO.find(10L);
        assertNotNull(child2);
        assertEquals(group, child2.getParent());

        List<Group> ownedGroups = groupDAO.findOwnedByUser(user.getKey());
        assertFalse(ownedGroups.isEmpty());
        assertEquals(2, ownedGroups.size());
        assertTrue(ownedGroups.contains(group));
        assertTrue(ownedGroups.contains(child1));
        assertFalse(ownedGroups.contains(child2));
    }

    public void createWithPasswordPolicy() {
        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        Group group = entityFactory.newEntity(Group.class);
        group.setName("groupWithPasswordPolicy");
        group.setPasswordPolicy(policy);

        Group actual = groupDAO.save(group);
        assertNotNull(actual);

        actual = groupDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        groupDAO.delete(actual.getKey());
        assertNull(groupDAO.find(actual.getKey()));

        assertNotNull(policyDAO.find(4L));
    }

    @Test
    public void delete() {
        groupDAO.delete(2L);

        groupDAO.flush();

        assertNull(groupDAO.find(2L));
        assertEquals(1, groupDAO.findByEntitlement(entitlementDAO.find("base")).size());
        assertEquals(userDAO.find(2L).getGroups().size(), 2);
        assertNull(plainAttrDAO.find(700L, GPlainAttr.class));
        assertNull(plainAttrValueDAO.find(41L, GPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("icon", GPlainSchema.class));
    }
}