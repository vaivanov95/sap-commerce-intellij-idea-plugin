/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for Intellij IDEA.
 * Copyright (C) 2019-2023 EPAM Systems <hybrisideaplugin@epam.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.intellij.idea.plugin.hybris.system.type.psi.reference

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.idea.plugin.hybris.common.HybrisConstants
import com.intellij.idea.plugin.hybris.psi.util.PsiUtils
import com.intellij.idea.plugin.hybris.system.type.codeInsight.completion.TSCompletionService
import com.intellij.idea.plugin.hybris.system.type.meta.TSGlobalMetaModel
import com.intellij.idea.plugin.hybris.system.type.meta.TSMetaItemService
import com.intellij.idea.plugin.hybris.system.type.meta.TSMetaModelAccess
import com.intellij.idea.plugin.hybris.system.type.meta.model.TSGlobalMetaEnum
import com.intellij.idea.plugin.hybris.system.type.meta.model.TSGlobalMetaItem
import com.intellij.idea.plugin.hybris.system.type.meta.model.TSGlobalMetaRelation
import com.intellij.idea.plugin.hybris.system.type.psi.reference.result.AttributeResolveResult
import com.intellij.idea.plugin.hybris.system.type.psi.reference.result.RelationEndResolveResult
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.*
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.start

abstract class AbstractAttributeDeclarationReference : PsiReferenceBase.Poly<PsiElement>, HighlightedReference {

    constructor(element: PsiElement) : super(element, false)
    constructor(element: PsiElement, textRange: TextRange) : super(element, textRange, false)

    private val cacheKey = Key.create<ParameterizedCachedValue<Array<ResolveResult>, AbstractAttributeDeclarationReference>>("HYBRIS_TS_CACHED_REFERENCE_${rangeInElement.start}")

    override fun calculateDefaultRangeInElement(): TextRange =
        if (element.textLength == 0) super.calculateDefaultRangeInElement()
        else TextRange.from(1, element.textLength - HybrisConstants.QUOTE_LENGTH)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> = CachedValuesManager.getManager(element.project)
        .getParameterizedCachedValue(element, cacheKey, provider, false, this)
        .let { PsiUtils.getValidResults(it) }


    override fun getVariants(): Array<Any> {
        val type = resolveType(element) ?: return emptyArray()
        val project = element.project

        return TSCompletionService.getInstance(project)
            .getCompletions(type)
            .toTypedArray()
    }

    protected abstract fun resolveType(element: PsiElement): String?

    companion object {
        private val provider = ParameterizedCachedValueProvider<Array<ResolveResult>, AbstractAttributeDeclarationReference> { ref ->
            val project = ref.element.project
            val metaModelAccess = TSMetaModelAccess.getInstance(project)
            val metaItemService = TSMetaItemService.getInstance(project)
            val metaModel = metaModelAccess.getMetaModel()

            val type = ref.resolveType(ref.element) ?: return@ParameterizedCachedValueProvider emptyResult(metaModel)

            val metaItem = when (val meta = metaModelAccess.findMetaClassifierByName(type)) {
                is TSGlobalMetaItem -> meta
                is TSGlobalMetaRelation -> metaModelAccess.findMetaItemByName(HybrisConstants.TS_TYPE_LINK)
                    ?: return@ParameterizedCachedValueProvider emptyResult(metaModel)

                is TSGlobalMetaEnum -> metaModelAccess.findMetaItemByName(HybrisConstants.TS_TYPE_ENUMERATION_VALUE)
                    ?: return@ParameterizedCachedValueProvider emptyResult(metaModel)

                else -> return@ParameterizedCachedValueProvider emptyResult(metaModel)
            }

            val originalValue = ref.value
            val result = metaItemService.findAttributesByName(metaItem, originalValue, true)
                ?.firstOrNull()
                ?.let { PsiUtils.getValidResults(arrayOf(AttributeResolveResult(it))) }
                ?: metaItemService.findRelationEndsByQualifier(metaItem, originalValue, true)
                    ?.firstOrNull()
                    ?.let { PsiUtils.getValidResults(arrayOf(RelationEndResolveResult(it))) }
                ?: emptyArray()

            CachedValueProvider.Result.create(
                result,
                metaModel, PsiModificationTracker.MODIFICATION_COUNT
            )
        }

        private fun emptyResult(metModel: TSGlobalMetaModel): CachedValueProvider.Result<Array<ResolveResult>> = CachedValueProvider.Result.create(
            emptyArray(),
            metModel, PsiModificationTracker.MODIFICATION_COUNT
        )
    }
}
