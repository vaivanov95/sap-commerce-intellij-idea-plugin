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

package com.intellij.idea.plugin.hybris.system.type.settings

import com.intellij.idea.plugin.hybris.settings.FoldingSettings

data class TypeSystemSettings(
    var folding: TypeSystemFoldingSettings = TypeSystemFoldingSettings(),
)

data class TypeSystemFoldingSettings(
    override var enabled: Boolean = true,
    var tablifyAtomics: Boolean = true,
    var tablifyCollections: Boolean = true,
    var tablifyMaps: Boolean = true,
    var tablifyRelations: Boolean = true,
    var tablifyItemAttributes: Boolean = true,
    var tablifyItemIndexes: Boolean = true,
    var tablifyItemCustomProperties: Boolean = true,
): FoldingSettings
