/*
 * This file is part of "hybris integration" plugin for Intellij IDEA.
 * Copyright (C) 2014-2016 Alexander Bartash <AlexanderBartash@gmail.com>
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

package com.intellij.idea.plugin.hybris.project.configurators.impl;

import com.intellij.idea.plugin.hybris.project.configurators.VersionControlSystemConfigurator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by Martin Zdarsky-Jones (martin.zdarsky@hybris.com) on 17/10/2016.
 */
public class DefaultVersionControlSystemConfigurator implements VersionControlSystemConfigurator {

    @Override
    public void configure(@NotNull Project project) {
        final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        final Collection<VcsRoot> detectedRoots = ServiceManager.getService(project, VcsRootDetector.class).detect();
        final ArrayList<VcsDirectoryMapping> directoryMappings =  detectedRoots
            .stream()
            .map(root -> new VcsDirectoryMapping(root.getPath().getPath(), root.getVcs().getName()))
            .collect(Collectors.toCollection(ArrayList::new));
        vcsManager.setDirectoryMappings(directoryMappings);
    }
}