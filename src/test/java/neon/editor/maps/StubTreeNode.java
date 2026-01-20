package neon.editor.maps;

import java.util.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class StubTreeNode implements MutableTreeNode {
  /** this node's parent, or null if this node has no parent */
  protected MutableTreeNode parent;

  /** array of children, may be null if this node has no children */
  protected LinkedList<MutableTreeNode> children = new LinkedList<>();

  /** optional user object */
  protected transient Object userObject;

  /** true if the node is able to have children */
  protected boolean allowsChildren;

  /**
   * Returns the child <code>TreeNode</code> at index <code>childIndex</code>.
   *
   * @param childIndex index of child
   * @return the child node at given index
   */
  @Override
  public TreeNode getChildAt(int childIndex) {
    return children.get(childIndex);
  }

  /**
   * Returns the number of children <code>TreeNode</code>s the receiver contains.
   *
   * @return the number of children the receiver contains
   */
  @Override
  public int getChildCount() {
    return children.size();
  }

  /**
   * Returns the parent <code>TreeNode</code> of the receiver.
   *
   * @return the parent of the receiver
   */
  @Override
  public TreeNode getParent() {
    return parent;
  }

  /**
   * Returns the index of <code>node</code> in the receivers children. If the receiver does not
   * contain <code>node</code>, -1 will be returned.
   *
   * @param node node to be loked for
   * @return index of specified node
   */
  @Override
  public int getIndex(TreeNode node) {
    int i = 0;
    for (TreeNode child : children) {
      if (child.equals(node)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  /**
   * Returns true if the receiver allows children.
   *
   * @return whether the receiver allows children
   */
  @Override
  public boolean getAllowsChildren() {
    return allowsChildren;
  }

  /**
   * Returns true if the receiver is a leaf.
   *
   * @return whether the receiver is a leaf
   */
  @Override
  public boolean isLeaf() {
    return children.isEmpty();
  }

  /**
   * Returns the children of the receiver as an <code>Enumeration</code>.
   *
   * @return the children of the receiver as an {@code Enumeration}
   */
  @Override
  public Enumeration<? extends TreeNode> children() {
    return new TreeNodeEnumeration(this);
  }

  /**
   * Adds <code>child</code> to the receiver at <code>index</code>. <code>child</code> will be
   * messaged with <code>setParent</code>.
   *
   * @param child node to be added
   * @param index index of the receiver
   */
  @Override
  public void insert(MutableTreeNode child, int index) {
    if (index == children.size()) {
      children.add(child);
      child.setParent(this);
    } else {
      throw new IllegalArgumentException("Can only insert at end of children");
    }
  }

  /**
   * Removes the child at <code>index</code> from the receiver.
   *
   * @param index index of child to be removed
   */
  @Override
  public void remove(int index) {}

  /**
   * Removes <code>node</code> from the receiver. <code>setParent</code> will be messaged on <code>
   * node</code>.
   *
   * @param node node to be removed from the receiver
   */
  @Override
  public void remove(MutableTreeNode node) {}

  /**
   * Resets the user object of the receiver to <code>object</code>.
   *
   * @param object object to be set as a receiver
   */
  @Override
  public void setUserObject(Object object) {}

  /** Removes the receiver from its parent. */
  @Override
  public void removeFromParent() {}

  /**
   * Sets the parent of the receiver to <code>newParent</code>.
   *
   * @param newParent node to be set as parent of the receiver
   */
  @Override
  public void setParent(MutableTreeNode newParent) {}

  private class TreeNodeEnumeration implements Enumeration<TreeNode> {
    final Iterator<MutableTreeNode> iterator;
    final StubTreeNode root;

    public TreeNodeEnumeration(StubTreeNode treeNode) {
      root = treeNode;
      iterator = root.children.iterator();
    }

    @Override
    public boolean hasMoreElements() {
      return iterator.hasNext();
    }

    @Override
    public TreeNode nextElement() {
      return iterator.next();
    }
  }
}
