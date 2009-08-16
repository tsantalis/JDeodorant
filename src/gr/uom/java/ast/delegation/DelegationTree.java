package gr.uom.java.ast.delegation;

import gr.uom.java.ast.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;


public class DelegationTree {
    private DefaultMutableTreeNode rootNode;
    private SystemObject systemObject;

    public DelegationTree(SystemObject systemObject, MethodObject methodObject) {
        rootNode = new DefaultMutableTreeNode(methodObject);
        this.systemObject = systemObject;
        getDelegations(rootNode);
    }

    private void getDelegations(DefaultMutableTreeNode node) {
        MethodObject methodObject = (MethodObject)node.getUserObject();
        if(methodObject != null) {
            List<MethodInvocationObject> methodInvocationList = methodObject.getMethodInvocations();
            for(MethodInvocationObject mio : methodInvocationList) {
                //int methodPos = systemObject.getPositionInClassList(methodObject.getClassName());
                int methodInvocationPos = systemObject.getPositionInClassList(mio.getOriginClassName());
                if(methodInvocationPos != -1) {
	                MethodObject methodInvocation = systemObject.getClassObject(mio.getOriginClassName()).getMethod(mio);
	                // methodPos != methodInvocationPos -> removes self-delegations
	                // !existsNode(node.children(),methodInvocation) -> removes duplicate delegations
	                // !existsNode(node.getUserObjectPath(),methodInvocation) -> avoids cyclic delegations
	                if(!existsNode(node.children(),methodInvocation) && !existsNode(node.getUserObjectPath(),methodInvocation)) {
	                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(methodInvocation);
	                    node.add(childNode);
	                    getDelegations(childNode);
	                }
                }
            }
        }
    }

    private boolean existsNode(Object[] path, MethodObject mo) {
        for(Object pathLevel : path) {
            if(mo.equals(pathLevel))
                return true;
        }
        return false;
    }

    private boolean existsNode(Enumeration<DefaultMutableTreeNode> children, MethodObject mo) {
        while(children.hasMoreElements()) {
            DefaultMutableTreeNode child = children.nextElement();
            MethodObject childMethodObject = (MethodObject)child.getUserObject();
            if(childMethodObject.equals(mo))
                return true;
        }
        return false;
    }

    public int getDepth() {
        return rootNode.getDepth();
    }

    public List<DelegationPath> getDelegationPathList() {
        List<DelegationPath> pathList = new ArrayList<DelegationPath>();

        DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
        while(leaf != null) {
            Object[] path = leaf.getUserObjectPath();
            DelegationPath delegationPath = new DelegationPath();
            for (Object pathLevel : path) {
                MethodObject mo = (MethodObject)pathLevel;
                delegationPath.addMethodInvocation(mo);
            }
            pathList.add(delegationPath);
            leaf = leaf.getNextLeaf();
        }
        return pathList;
    }
}
