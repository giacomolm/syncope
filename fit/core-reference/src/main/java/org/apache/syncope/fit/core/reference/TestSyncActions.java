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
package org.apache.syncope.fit.core.reference;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.sync.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.sync.DefaultSyncActions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Test synchronization action.
 */
public class TestSyncActions extends DefaultSyncActions {

    private int counter = 0;

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        AttrTO attrTO = null;
        for (int i = 0; i < subject.getPlainAttrs().size(); i++) {
            AttrTO _attrTO = CollectionUtils.get(subject.getPlainAttrs(), i);
            if ("fullname".equals(_attrTO.getSchema())) {
                attrTO = _attrTO;
            }
        }
        if (attrTO == null) {
            attrTO = new AttrTO();
            attrTO.setSchema("fullname");
            subject.getPlainAttrs().add(attrTO);
        }
        attrTO.getValues().clear();
        attrTO.getValues().add(String.valueOf(counter++));

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        if (subject instanceof UserTO && "test2".equals(UserTO.class.cast(subject).getUsername())) {
            throw new IgnoreProvisionException();
        }

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO, K extends AbstractSubjectMod> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final K subjectMod) throws JobExecutionException {

        subjectMod.getPlainAttrsToRemove().add("fullname");

        AttrMod fullnameMod = null;
        for (AttrMod attrMod : subjectMod.getPlainAttrsToUpdate()) {
            if ("fullname".equals(attrMod.getSchema())) {
                fullnameMod = attrMod;
            }
        }
        if (fullnameMod == null) {
            fullnameMod = new AttrMod();
            fullnameMod.setSchema("fullname");
            subjectMod.getPlainAttrsToUpdate().add(fullnameMod);
        }

        fullnameMod.getValuesToBeAdded().clear();
        fullnameMod.getValuesToBeAdded().add(String.valueOf(counter++));

        return delta;
    }
}