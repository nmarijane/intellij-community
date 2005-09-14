package com.intellij.psi.impl.source;

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.source.parsing.JavaParsingContext;
import com.intellij.psi.impl.source.parsing.Parsing;
import com.intellij.psi.impl.source.tree.*;

public class PsiImportStaticStatementImpl extends PsiImportStatementBaseImpl implements PsiImportStaticStatement {

  public PsiImportStaticStatementImpl(PsiManagerImpl manager, RepositoryTreeElement treeElement) {
    super(manager, treeElement);
  }

  public PsiImportStaticStatementImpl(PsiManagerImpl manager, SrcRepositoryPsiElement owner, int index) {
    super(manager, owner, index);
  }

  public PsiClass resolveTargetClass() {
    final PsiJavaCodeReferenceElement classReference = getClassReference();
    if (classReference == null) return null;
    final PsiElement result = classReference.resolve();
    if (result instanceof PsiClass) {
      return (PsiClass) result;
    }
    else {
      return null;
    }
  }

  public String getReferenceName() {
    if (isOnDemand()) return null;
    final PsiImportStaticReferenceElement memberReference = getMemberReference();
    if (memberReference != null) {
      return memberReference.getReferenceName();
    }
    else {
      return null;
    }
  }

  public PsiJavaCodeReferenceElement getImportReference() {
    return (PsiJavaCodeReferenceElement)calcTreeElement().findChildByRoleAsPsiElement(ChildRole.IMPORT_REFERENCE);
  }

  private PsiImportStaticReferenceElement getMemberReference() {
    if (isOnDemand()) {
      return null;
    }
    else {
      return (PsiImportStaticReferenceElement) getMirrorReference();
    }
  }

  private PsiJavaCodeReferenceElement getClassReference() {
    if (isOnDemand()) {
      return getMirrorReference();
    }
    else {
      final PsiImportStaticReferenceElement memberReference = getMemberReference();
      if (memberReference != null) {
        return memberReference.getClassReference();
      }
      else {
        return null;
      }
    }
  }

  public void accept(PsiElementVisitor visitor){
    visitor.visitImportStaticStatement(this);
  }

  public String toString(){
    return "PsiImportStaticStatement";
  }

  public PsiJavaCodeReferenceElement getMirrorReference() {
    if (myOwner != null){
      PsiJavaCodeReferenceElement refElement = getCachedMirrorReference();
      if (refElement == null) {
        CompositeElement treeElement = getTreeElement();
        if (treeElement != null){
          refElement = (PsiJavaCodeReferenceElement)treeElement.findChildByRole(ChildRole.IMPORT_REFERENCE);
        }
        else{
          final FileElement holderElement = new DummyHolder(myManager, this).getTreeElement();
          final JavaParsingContext context = new JavaParsingContext(holderElement.getCharTable(), myManager.getEffectiveLanguageLevel());
          final String refText;
          synchronized (PsiLock.LOCK) {
            refText = getRepositoryManager().getFileView().getImportQualifiedName(getRepositoryId(), getIndex());
          }
          if (refText == null) return null;
          CompositeElement parsedRef = Parsing.parseJavaCodeReferenceText(myManager, refText.toCharArray(), context.getCharTable());
          refElement = (PsiJavaCodeReferenceElement)parsedRef;
          final boolean onDemand = isOnDemand();
          if (onDemand) {
            TreeUtil.addChildren(holderElement, (TreeElement)refElement);
            ((PsiJavaCodeReferenceElementImpl)refElement).setKindWhenDummy(
              PsiJavaCodeReferenceElementImpl.CLASS_FQ_NAME_KIND);
          }
          else {
            refElement = (PsiImportStaticReferenceElement)context.getImportsTextParsing().convertToImportStaticReference(parsedRef);
            TreeUtil.addChildren(holderElement, (TreeElement)refElement);
          }
        }

        setCachedMirrorReference(refElement);
      }

      return refElement;
    }
    else{
      return (PsiJavaCodeReferenceElement)calcTreeElement().findChildByRoleAsPsiElement(ChildRole.IMPORT_REFERENCE);
    }
  }
}

