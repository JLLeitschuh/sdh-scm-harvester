/**
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   This file is part of the Smart Developer Hub Project:
 *     http://www.smartdeveloperhub.org/
 *
 *   Center for Open Middleware
 *     http://www.centeropenmiddleware.com/
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Copyright (C) 2015-2016 Center for Open Middleware.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Artifact    : org.smartdeveloperhub.harvesters.scm:scm-harvester-frontend:0.3.0-SNAPSHOT
 *   Bundle      : scm-harvester.war
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.smartdeveloperhub.harvesters.scm.frontend.core.user;

import java.net.URI;

import org.joda.time.DateTime;
import org.ldp4j.application.data.DataSet;
import org.ldp4j.application.data.DataSetHelper;
import org.ldp4j.application.data.DataSetUtils;
import org.ldp4j.application.data.DataSets;
import org.ldp4j.application.data.Name;
import org.ldp4j.application.ext.ApplicationRuntimeException;
import org.ldp4j.application.ext.ResourceHandler;
import org.ldp4j.application.ext.annotations.Resource;
import org.ldp4j.application.session.ResourceSnapshot;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.User;
import org.smartdeveloperhub.harvesters.scm.frontend.core.publisher.BackendController;
import org.smartdeveloperhub.harvesters.scm.frontend.core.util.Mapper;

@Resource(id=UserHandler.ID)
public class UserHandler implements ResourceHandler {

	public static final String ID="UserHandler";

	private static final URI IMG_PATH = URI.create("#img");

	private final BackendController backendController;

	public UserHandler(final BackendController backendController) {
		this.backendController = backendController;
	}

	@Override
	public DataSet get(final ResourceSnapshot resource) {
		@SuppressWarnings("unchecked")
		final Name<String> name = (Name<String>)resource.name();
		try{
			final User user = this.backendController.getUser(name.id().toString());
			return maptoDataSet(user,name);
		} catch(final Exception e){
			 throw new ApplicationRuntimeException(e);
		}
	}

	private DataSet maptoDataSet(final User user, final Name<String> userName) {
		final DataSet dataSet=DataSets.createDataSet(userName);
		final DataSetHelper helper=DataSetUtils.newHelper(dataSet);

		helper.
			managedIndividual(userName, UserHandler.ID).
				property(UserVocabulary.TYPE).
					withIndividual(UserVocabulary.PERSONTYPE).
				property(UserVocabulary.NAME).
					withLiteral(user.getName()).
				property(UserVocabulary.NICK).
					withLiteral(user.getUsername()).
				property(UserVocabulary.EXTERNAL).
					withLiteral(new Boolean(user.isExternal())).
				property(UserVocabulary.COMMITTERID).
					withLiteral(user.getId()).
				property(UserVocabulary.FIRSTCOMMIT).
					withLiteral(Mapper.toLiteral(new DateTime(user.getFirstCommitAt()).toDate())).
				property(UserVocabulary.LASTCOMMIT).
					withLiteral(Mapper.toLiteral(new DateTime(user.getLastCommitAt()).toDate()));

		for(final String email:user.getEmails()){
			helper.
				managedIndividual(userName, UserHandler.ID).
					property(UserVocabulary.MBOX).
						withLiteral(email);
		}

		if(user.getAvatarUrl()!=null){
			helper.
				managedIndividual(userName, UserHandler.ID).
					property(UserVocabulary.IMG).
						withIndividual(userName, UserHandler.ID,IMG_PATH);
			helper.
				relativeIndividual(userName,UserHandler.ID,IMG_PATH).
					property(UserVocabulary.TYPE).
						withIndividual(UserVocabulary.IMAGE).
					property(UserVocabulary.DEPICTS).
						withIndividual(user.getAvatarUrl());
		}

		return dataSet;
	}

}