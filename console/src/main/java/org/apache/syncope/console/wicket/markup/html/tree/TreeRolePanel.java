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
package org.apache.syncope.console.wicket.markup.html.tree;

import static org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State.EXPANDED;

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import org.apache.syncope.common.to.AbstractAttributableTO;

import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.Roles.TreeNodeClick;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TreeRolePanel extends Panel {

    private static final long serialVersionUID = 1762003213871836869L;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    final WebMarkupContainer treeContainer;

    private NestedTree<DefaultMutableTreeNode> tree;

    public TreeRolePanel(final String id) {
        super(id);

        treeContainer = new WebMarkupContainer("treeContainer");
        treeContainer.setOutputMarkupId(true);
        add(treeContainer);
        updateTree();
    }

    private DefaultMutableTreeNode updateTree(final long selectedNodeId) {
        DefaultMutableTreeNode parent = roleTreeBuilder.refresh(selectedNodeId);
        return parent;
    }

    private void updateTree() {
        final ITreeProvider<DefaultMutableTreeNode> treeProvider = new TreeRoleProvider(roleTreeBuilder, true);
        final DefaultMutableTreeNodeExpansionModel treeModel = new DefaultMutableTreeNodeExpansionModel();

        tree = new DefaultNestedTree<DefaultMutableTreeNode>("treeTable", treeProvider, treeModel) {

            private static final long serialVersionUID = 7137658050662575546L;

            @Override
            public void updateBranch(DefaultMutableTreeNode parentNode, AjaxRequestTarget target) {
                if (tree.getState(parentNode) == EXPANDED) {
                    DefaultMutableTreeNode newParent = ((TreeRoleProvider) treeProvider).update(parentNode,
                            roleTreeBuilder);
                    super.updateBranch(newParent, target);
                } else {
                    super.updateBranch(parentNode, target);
                }
            }

            @Override
            public Component newSubtree(String id, IModel<DefaultMutableTreeNode> model) {
                if (model.getObject() != null && model.getObject().isRoot()) {
                    tree.expand(model.getObject());
                }
                return super.newSubtree(id, model); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> node) {
                final DefaultMutableTreeNode treeNode = node.getObject();
                final RoleTO roleTO = (RoleTO) treeNode.getUserObject();

                tree.collapse(node.getObject());

                return new Folder<DefaultMutableTreeNode>(id, TreeRolePanel.this.tree, node) {

                    private static final long serialVersionUID = 9046323319920426493L;

                    @Override
                    protected boolean isClickable() {
                        return true;
                    }

                    @Override
                    protected IModel<?> newLabelModel(final IModel<DefaultMutableTreeNode> model) {
                        return new Model<String>(roleTO.getDisplayName());
                    }

                    @Override
                    protected void onClick(final AjaxRequestTarget target) {
                        super.onClick(target);

                        send(getPage(), Broadcast.BREADTH, new TreeNodeClick(target, roleTO.getId()));
                    }
                };
            }

            @Override
            public void expand(DefaultMutableTreeNode t) {
                super.expand(t); //To change body of generated methods, choose Tools | Templates.
                
                try{
                    throw new Exception("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            
            
        };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(tree, ENABLE, xmlRolesReader.getEntitlement("Roles", "read"));

        treeContainer.addOrReplace(tree);
    }

    @Override

    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof TreeNodeUpdate) {
            final TreeNodeUpdate update = (TreeNodeUpdate) event.getPayload();
            DefaultMutableTreeNode parent = updateTree(update.getSelectedNodeId());

            tree.updateBranch(parent, update.getTarget());
            Component p = tree;
            /*for (Object node : parent.getUserObjectPath()) {
                long id = ((AbstractAttributableTO) node).getId();
                p = id == 0 ? p.get("") : p.get("subtree").get("branches").get(String.valueOf(id));
            }*/
            tree.expand(parent);
            update.getTarget().add(this);            
        }
    }

    public static class TreeNodeUpdate {

        private final AjaxRequestTarget target;

        private Long selectedNodeId;

        public TreeNodeUpdate(final AjaxRequestTarget target, final Long selectedNodeId) {
            this.target = target;
            this.selectedNodeId = selectedNodeId;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Long getSelectedNodeId() {
            return selectedNodeId;
        }

        public void setSelectedNodeId(final Long selectedNodeId) {
            this.selectedNodeId = selectedNodeId;
        }
    }
}
