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
package org.apache.syncope.console.commons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.console.rest.RoleRestClient;
import org.springframework.beans.factory.annotation.Autowired;

public class RoleTreeBuilder {

    @Autowired
    private RoleRestClient restClient;

    private final RoleTOComparator comparator = new RoleTOComparator();

    private List<RoleTO> allRoles;

    private DefaultTreeModel fakerootModel;

    private DefaultMutableTreeNode fakeroot;

    private List<RoleTO> getChildRoles(final long parentRoleId, final List<RoleTO> roles) {
        List<RoleTO> result = new ArrayList<RoleTO>();
        for (RoleTO role : roles) {
            if (role.getParent() == parentRoleId) {
                result.add(role);
            }
        }

        Collections.sort(result, comparator);
        return result;
    }

    private void populateSubtree(final DefaultMutableTreeNode subRoot, final List<RoleTO> roles) {
        RoleTO role = (RoleTO) subRoot.getUserObject();

        DefaultMutableTreeNode child;
        for (RoleTO subRoleTO : getChildRoles(role.getId(), roles)) {
            child = new DefaultMutableTreeNode(subRoleTO);
            subRoot.add(child);
            populateSubtree(child, roles);
        }
    }

    private void populateSubtreePartially(final DefaultMutableTreeNode subRoot, final List<RoleTO> roles) {
        RoleTO role = (RoleTO) subRoot.getUserObject();
        subRoot.removeAllChildren();
        DefaultMutableTreeNode child;
        for (RoleTO subRoleTO : getChildRoles(role.getId(), roles)) {
            child = new DefaultMutableTreeNode(subRoleTO);
            if (subRoleTO.isSubtree()) {
                insertFakeChild(child);
            }
            subRoot.add(child);
        }

    }

    private void insertFakeChild(final DefaultMutableTreeNode subRoot) {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("fakeChild");
        roleTO.setParent(((AbstractAttributableTO) subRoot.getUserObject()).getId());
        DefaultMutableTreeNode fakeChild = new DefaultMutableTreeNode(roleTO);
        subRoot.add(fakeChild);
    }

    public List<RoleTO> getAllRoles() {
        return this.allRoles;
    }

    public TreeModel build() {
        if (this.allRoles == null) {
            this.allRoles = this.restClient.children(0);
            return build(this.allRoles);
        } else {
            fakeroot = new DefaultMutableTreeNode(new FakeRootRoleTO());
            populateSubtree(fakeroot, this.allRoles);
            fakerootModel = new DefaultTreeModel(fakeroot);
            return fakerootModel;
        }
    }

    public DefaultMutableTreeNode refresh(final long roleId) {
        Enumeration nodes = getRoot().breadthFirstEnumeration();
        boolean found = false;
        DefaultMutableTreeNode node = null;
        for (; nodes.hasMoreElements() && node == null;) {
            DefaultMutableTreeNode currentNode = ((DefaultMutableTreeNode) nodes.nextElement());
            if (((AbstractAttributableTO) currentNode.getUserObject()).getId() == roleId) {
                node = currentNode;
            }
        }

        if (node != null) {
            List<RoleTO> updatedTO = this.restClient.children(((AbstractAttributableTO) node.getUserObject()).getId());
            for(RoleTO roleTO : updatedTO){
                if(!this.allRoles.contains(roleTO)){
                    this.allRoles.add(roleTO);
                }
            }
            populateSubtree(node, allRoles);
        }
        
        return node;
    }

    public TreeNode update(final DefaultMutableTreeNode treeNode) {

        if (((AbstractAttributableTO) treeNode.getUserObject()).getId() == 0) {
            populateSubtreePartially(treeNode, allRoles);
        } else if (((RoleTO) treeNode.getUserObject()).isSubtree()) {
            if (getChildRoles(
                    ((AbstractAttributableTO) treeNode.getUserObject()).getId(), allRoles).isEmpty()) {
                this.allRoles.addAll(this.restClient.children(((AbstractAttributableTO) treeNode.getUserObject()).
                        getId()));
            }
            populateSubtreePartially(treeNode, allRoles);
        }
        return treeNode;
    }

    public TreeModel build(final List<RoleTO> roles) {
        fakeroot = new DefaultMutableTreeNode(new FakeRootRoleTO());
        populateSubtreePartially(fakeroot, roles);
        fakerootModel = new DefaultTreeModel(fakeroot);
        return fakerootModel;
    }

    public DefaultMutableTreeNode getRoot() {
        return fakeroot;
    }

    public boolean isExpandend(final DefaultMutableTreeNode parentNode) {
        parentNode.removeAllChildren();
        populateSubtree(parentNode, this.allRoles);
        if (((RoleTO) parentNode.getUserObject()).isSubtree() && parentNode.children().hasMoreElements()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isVisible(final long roleId) {
        Enumeration nodes = getRoot().breadthFirstEnumeration();
        boolean found = false;
        for (; nodes.hasMoreElements() && !found;) {
            if (((AbstractAttributableTO) ((DefaultMutableTreeNode) nodes.nextElement()).getUserObject()).getId()
                    == roleId) {
                found = true;
            }
        }
        return found;
    }

    private static class RoleTOComparator implements Comparator<RoleTO>, Serializable {

        private static final long serialVersionUID = 7085057398406518811L;

        @Override
        public int compare(final RoleTO r1, final RoleTO r2) {
            if (r1.getId() < r2.getId()) {
                return -1;
            }
            if (r1.getId() == r2.getId()) {
                return 0;
            }

            return 1;
        }
    }

    private static class FakeRootRoleTO extends RoleTO {

        private static final long serialVersionUID = 4839183625773925488L;

        public FakeRootRoleTO() {
            super();

            setId(0);
            setName("");
            setParent(-1);
        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
