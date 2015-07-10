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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.Mode;
import org.apache.syncope.console.pages.panels.RolePanel;
import org.apache.syncope.console.wicket.markup.html.tree.TreeRolePanel.TreeNodeUpdate;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1732493223434085205L;

    protected final PageReference pageRef;

    protected final ModalWindow window;

    protected final Mode mode;

    protected final boolean createFlag;

    protected final RolePanel rolePanel;

    protected RoleTO originalRoleTO;

    public RoleModalPage(final PageReference pageRef, final ModalWindow window, final RoleTO roleTO) {
        this(pageRef, window, roleTO, Mode.ADMIN);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RoleModalPage(final PageReference pageRef, final ModalWindow window, final RoleTO roleTO, final Mode mode) {
        super();

        this.pageRef = pageRef;
        this.window = window;
        this.mode = mode;

        this.createFlag = roleTO.getId() == 0;
        if (!createFlag) {
            originalRoleTO = SerializationUtils.clone(roleTO);
        }

        final Form<RoleTO> form = new Form<RoleTO>("roleForm");
        form.setMultiPart(true);

        add(new Label("displayName", roleTO.getId() == 0 ? "" : roleTO.getDisplayName()));

        form.setModel(new CompoundPropertyModel<RoleTO>(roleTO));

        this.rolePanel = new RolePanel.Builder("rolePanel").form(form).roleTO(roleTO).
                roleModalPageMode(mode).pageRef(getPageReference()).build();
        form.add(rolePanel);

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }
                    closeAction(target, form);
                    send(pageRef.getPage(), Broadcast.BREADTH, new TreeNodeUpdate(target, ((RoleTO) form.
                            getDefaultModelObject()).getParent()));
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                closeAction(target, form);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, xmlRolesReader.getEntitlement("Roles",
                createFlag
                        ? "create"
                        : "update"));

        add(form);
    }

    protected void submitAction(final AjaxRequestTarget target, final Form<?> form) {
        final RoleTO roleTO = (RoleTO) form.getDefaultModelObject();
        final List<String> entitlementList = new ArrayList<String>(rolePanel.getSelectedEntitlements());
        roleTO.getEntitlements().clear();
        roleTO.getEntitlements().addAll(entitlementList);

        RoleTO result;
        if (createFlag) {
            result = roleRestClient.create(roleTO);
        } else {
            RoleMod roleMod = AttributableOperations.diff(roleTO, originalRoleTO);

            // update role just if it is changed
            if (roleMod.isEmpty()) {
                result = roleTO;
            } else {
                result = roleRestClient.update(originalRoleTO.getETagValue(), roleMod);
            }
        }

        setResponsePage(new ResultStatusModalPage.Builder(window, result).build());
    }

    protected void closeAction(final AjaxRequestTarget target, final Form<?> form) {
        window.close(target);
    }
}
