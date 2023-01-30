package com.intellij.idea.plugin.hybris.flexibleSearch.references

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.idea.plugin.hybris.common.HybrisConstants
import com.intellij.idea.plugin.hybris.common.HybrisConstants.CODE_ATTRIBUTE_NAME
import com.intellij.idea.plugin.hybris.common.HybrisConstants.NAME_ATTRIBUTE_NAME
import com.intellij.idea.plugin.hybris.common.HybrisConstants.SOURCE_ATTRIBUTE_NAME
import com.intellij.idea.plugin.hybris.common.HybrisConstants.TARGET_ATTRIBUTE_NAME
import com.intellij.idea.plugin.hybris.flexibleSearch.psi.*
import com.intellij.idea.plugin.hybris.psi.reference.TSReferenceBase
import com.intellij.idea.plugin.hybris.psi.utils.PsiUtils
import com.intellij.idea.plugin.hybris.system.type.meta.TSMetaModelAccess
import com.intellij.idea.plugin.hybris.system.type.psi.reference.result.AttributeResolveResult
import com.intellij.idea.plugin.hybris.system.type.psi.reference.result.EnumResolveResult
import com.intellij.idea.plugin.hybris.system.type.psi.reference.result.RelationEndResolveResult
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * @author Nosov Aleksandr <nosovae.dev@gmail.com>
 */
abstract class ColumnReferenceMixin(node: ASTNode) : ASTWrapperPsiElement(node),
    FlexibleSearchColumnReference {

    private var reference: TSAttributeReference? = null

    override fun getReferences(): Array<PsiReference?> {
        if (PsiUtils.shouldCreateNewReference(reference, text)) {
            reference = TSAttributeReference(this)
        }
        return arrayOf(reference)
    }

    override fun clone(): Any {
        val result = super.clone() as ColumnReferenceMixin
        result.reference = null
        return result
    }

    companion object {
        private const val serialVersionUID: Long = -4980389791496425285L
    }

}

internal class TSAttributeReference(owner: FlexibleSearchColumnReference) : TSReferenceBase<FlexibleSearchColumnReference>(owner),
    HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val featureName = element.text.replace("!", "")
        val result = if (hasPrefix(element)) {
            findReference(deepSearchOfTypeReference(element, element.firstChild.text), element.lastChild.text)
        } else {
            findReference(findItemTypeReference(), featureName)
        }
        return PsiUtils.getValidResults(result)
    }

    private fun hasPrefix(element: FlexibleSearchColumnReference) =
        ((element.firstChild as LeafPsiElement).elementType == FlexibleSearchTypes.TABLE_NAME_IDENTIFIER)

    private fun findReference(itemType: FlexibleSearchTableName?, refName: String): Array<ResolveResult> {
        val metaService = TSMetaModelAccess.getInstance(project)
        val type = itemType
            ?.text
            ?.replace("!", "")
            ?: return ResolveResult.EMPTY_ARRAY
        return tryResolveByItemType(type, refName, metaService)
            ?: tryResolveByRelationType(type, refName, metaService)
            ?: tryResolveByEnumType(type, refName, metaService)
            ?: ResolveResult.EMPTY_ARRAY
    }

    private fun tryResolveByItemType(type: String, refName: String, metaService: TSMetaModelAccess): Array<ResolveResult>? =
        metaService.findMetaItemByName(type)
            ?.let {
                val attributes = it.allAttributes
                    .filter { refName.equals(it.name, true) }
                    .map { AttributeResolveResult(it) }

                val relations = it.allRelationEnds
                    .filter { refName.equals(it.name, true) }
                    .map { RelationEndResolveResult(it) }

                (attributes + relations).toTypedArray()
            }

    private fun tryResolveByRelationType(type: String, refName: String, metaService: TSMetaModelAccess): Array<ResolveResult>? {
        val meta = metaService.findMetaRelationByName(type) ?: return null

        if (SOURCE_ATTRIBUTE_NAME.equals(refName, true)) {
            return arrayOf(RelationEndResolveResult(meta.source))
        } else if (TARGET_ATTRIBUTE_NAME.equals(refName, true)) {
            return arrayOf(RelationEndResolveResult(meta.target))
        }

        return metaService.findMetaItemByName(HybrisConstants.TS_TYPE_LINK)
            ?.attributes
            ?.get(refName)
            ?.let { arrayOf(AttributeResolveResult(it)) }
    }

    private fun tryResolveByEnumType(type: String, refName: String, metaService: TSMetaModelAccess): Array<ResolveResult>? {
        val meta = metaService.findMetaEnumByName(type) ?: return null

        return if (CODE_ATTRIBUTE_NAME == refName || NAME_ATTRIBUTE_NAME == refName) {
            arrayOf(EnumResolveResult(meta))
        } else return null
    }

    private fun findItemTypeReference(): FlexibleSearchTableName? {
        return PsiTreeUtil.getParentOfType(element, FlexibleSearchQuerySpecification::class.java)
            ?.let {
                PsiTreeUtil.findChildOfType(it, FlexibleSearchFromClause::class.java)
                    ?.tableReferenceList
                    ?.let { PsiTreeUtil.findChildOfType(it, FlexibleSearchTableName::class.java) }
            }
    }

    private fun deepSearchOfTypeReference(elem: PsiElement, prefix: String): FlexibleSearchTableName? {
        val parent = PsiTreeUtil.getParentOfType(elem, FlexibleSearchQuerySpecification::class.java)
        val tables = PsiTreeUtil.findChildrenOfType(parent, FlexibleSearchTableReference::class.java).toList()

        val tableReference = tables.find {
            val tableName = PsiTreeUtil.findChildOfAnyType(it, FlexibleSearchTableName::class.java)
            val corName = findCorName(tableName)
            prefix == corName
        }
        return if (tableReference == null && parent != null) {
            deepSearchOfTypeReference(parent, prefix)
        } else {
            PsiTreeUtil.findChildOfType(tableReference, FlexibleSearchTableName::class.java)
        }
    }

    private fun findCorName(tableName: FlexibleSearchTableName?): String {
        val corNameEl = PsiTreeUtil.findSiblingForward(tableName!!.originalElement, FlexibleSearchTypes.CORRELATION_NAME, null)
        if (corNameEl == null) {
            return tableName.text
        }
        return corNameEl.text
    }

}
