package com.intellij.psi.impl.source.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiLock;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.Constants;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.*;

/**
 *
 */
public class ChameleonTransforming implements Constants {
  private static final Logger LOG = Logger.getInstance("com.intellij.psi.impl.source.parsing.ChameleonTransforming");

  public static TreeElement transform(ChameleonElement chameleon) {
    synchronized (PsiLock.LOCK) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("\"transforming chameleon:\" + chameleon + \" in \" + chameleon.parent");
      }
      final ASTNode parent = chameleon.getTreeParent();
      parent.getTextLength();
      PsiFileImpl file = (PsiFileImpl)SourceTreeToPsiMap.treeElementToPsi(parent).getContainingFile();
      if (file == null) return null;

      TreeElement newElement = chameleon.transform(file.getTreeElement().getCharTable(), file.createLexer());
      final TreeElement treeNext = chameleon.getTreeNext();
      TreeUtil.replaceWithList(chameleon, newElement);
      if (DebugUtil.CHECK) {
        if (newElement != null) {
          DebugUtil.checkTreeStructure(newElement);
        }

        String text1 = chameleon.getText();

        int length2 = 0;
        for (ASTNode element = newElement; element != treeNext; element = element.getTreeNext()) {
          length2 += element.getTextLength();
        }
        char[] buffer = new char[length2];
        int offset = 0;
        for (ASTNode element = newElement; element != treeNext; element = element.getTreeNext()) {
          offset = SourceUtil.toBuffer(element, buffer, offset);
        }
        String text2 = new String(buffer);

        if (!text1.equals(text2)) {
          LOG.error("Text changed after chameleon transformation!\nWas:\n" + text1 + "\nbecame:\n" + text2);
        }
      }
      return newElement;
    }
  }

  public static void transformChildren(ASTNode element) {
    transformChildren(element, false);
  }

  public static void transformChildren(ASTNode element, boolean recursive) {
    synchronized (PsiLock.LOCK) {
      ASTNode child = element.getFirstChildNode();
      while (child != null) {
        if (child instanceof ChameleonElement) {
          ASTNode next = child.getTreeNext();
          child = transform((ChameleonElement)child);
          if (child == null) {
            child = next;
          }
          continue;
        }
        if (recursive && child instanceof CompositeElement) {
          transformChildren(child, recursive);
        }
        child = child.getTreeNext();
      }
    }
  }
}
