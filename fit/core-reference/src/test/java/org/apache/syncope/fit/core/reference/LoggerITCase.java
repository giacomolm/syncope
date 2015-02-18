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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.List;
import org.apache.syncope.common.lib.to.EventCategoryTO;
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.logic.RoleLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class LoggerITCase extends AbstractITCase {

    @Test
    public void listLogs() {
        List<LoggerTO> loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertFalse(loggers.isEmpty());
        for (LoggerTO logger : loggers) {
            assertNotNull(logger);
        }
    }

    @Test
    public void listAudits() throws ParseException {
        List<LoggerTO> audits = loggerService.list(LoggerType.AUDIT);

        assertNotNull(audits);
        assertFalse(audits.isEmpty());
        for (LoggerTO audit : audits) {
            assertNotNull(AuditLoggerName.fromLoggerName(audit.getKey()));
        }
    }

    @Test
    public void setLevel() {
        List<LoggerTO> loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        int startSize = loggers.size();

        LoggerTO logger = new LoggerTO();
        logger.setKey("TEST");
        logger.setLevel(LoggerLevel.INFO);
        loggerService.update(LoggerType.LOG, logger.getKey(), logger);
        logger = loggerService.read(LoggerType.LOG, logger.getKey());
        assertNotNull(logger);
        assertEquals(LoggerLevel.INFO, logger.getLevel());

        loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertEquals(startSize + 1, loggers.size());

        // TEST Delete
        loggerService.delete(LoggerType.LOG, "TEST");
        loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertEquals(startSize, loggers.size());
    }

    @Test
    public void enableDisableAudit() {
        AuditLoggerName auditLoggerName = new AuditLoggerName(
                EventCategoryType.REST,
                ReportLogic.class.getSimpleName(),
                null,
                "deleteExecution",
                AuditElements.Result.FAILURE);

        List<AuditLoggerName> audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));

        LoggerTO loggerTO = new LoggerTO();
        String name = auditLoggerName.toLoggerName();
        loggerTO.setKey(name);
        loggerTO.setLevel(LoggerLevel.DEBUG);
        loggerService.update(LoggerType.AUDIT, name, loggerTO);

        audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertTrue(audits.contains(auditLoggerName));

        loggerService.delete(LoggerType.AUDIT, auditLoggerName.toLoggerName());

        audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));
    }

    @Test
    public void listAuditEvents() {
        final List<EventCategoryTO> events = loggerService.events();

        boolean found = false;

        for (EventCategoryTO eventCategoryTO : events) {
            if (UserLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.REST, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("list"));
                assertFalse(eventCategoryTO.getEvents().contains("doCreate"));
                assertFalse(eventCategoryTO.getEvents().contains("setStatusOnWfAdapter"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (RoleLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.REST, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("list"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (ResourceLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.REST, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("read"));
                assertTrue(eventCategoryTO.getEvents().contains("delete"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (AttributableType.USER.name().toLowerCase().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_LDAP.equals(eventCategoryTO.getSubcategory())
                        && EventCategoryType.SYNCHRONIZATION == eventCategoryTO.getType()) {
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.CREATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.UPDATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (AttributableType.USER.name().toLowerCase().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_CSV.equals(eventCategoryTO.getSubcategory())
                        && EventCategoryType.PROPAGATION == eventCategoryTO.getType()) {
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.CREATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.UPDATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (EventCategoryType.TASK == eventCategoryTO.getType()
                    && "SampleJob".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (EventCategoryType.TASK == eventCategoryTO.getType()
                    && "SyncJob".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);
    }
}