package javagi.casestudies.publicationbrowser;

import java.util.*;
import javax.swing.tree.*;

public implementation TreeNode[Publication] {

  public Enumeration<TreeNode> children() {
    final Publication[] citations = this.citations;
    return new Enumeration<TreeNode>() {
      private int next = 0;
      public boolean hasMoreElements() {
        return citations.length < next;
      }
      public TreeNode nextElement() {
        return citations[next++];
      }
    };
  }

  public boolean getAllowsChildren() {
    return true;
  }

  public TreeNode getChildAt(int i) {
    return citations[i];
  }

  public int getChildCount() {
    return this.citations.length;
  }

  public int getIndex(TreeNode n) {
    for (int i = 0; i < this.citations.length; i++) {
      if (this.citations[i].equals(n)) return i;
    }
    return -1;
  }

  public TreeNode getParent() {
    TreeNode n = this.citedBy;
    return n;
  }

  public boolean isLeaf() {
    return this.citations.length == 0;
  }
}