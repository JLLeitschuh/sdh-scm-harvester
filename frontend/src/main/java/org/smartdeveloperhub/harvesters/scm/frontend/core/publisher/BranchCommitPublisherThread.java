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
package org.smartdeveloperhub.harvesters.scm.frontend.core.publisher;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.ldp4j.application.ApplicationContext;
import org.ldp4j.application.data.Name;
import org.ldp4j.application.data.NamingScheme;
import org.ldp4j.application.session.ContainerSnapshot;
import org.ldp4j.application.session.WriteSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.Repository;
import org.smartdeveloperhub.harvesters.scm.frontend.core.GitLabHarvester;
import org.smartdeveloperhub.harvesters.scm.frontend.core.branch.BranchContainerHandler;
import org.smartdeveloperhub.harvesters.scm.frontend.core.branch.BranchKey;
import org.smartdeveloperhub.harvesters.scm.frontend.core.commit.CommitContainerHandler;
import org.smartdeveloperhub.harvesters.scm.frontend.core.commit.CommitKey;

import com.google.common.base.Stopwatch;

public class BranchCommitPublisherThread extends Thread {

	private static final Logger LOGGER=LoggerFactory.getLogger(BranchCommitPublisherThread.class);

	private static final String THREAD_NAME = "BranchCommitPublisher";

	private final BackendController controller;

	public BranchCommitPublisherThread(final BackendController controller) {
		super();
		setName(THREAD_NAME);
		this.controller = controller;
	}

	@Override
	public void run(){
		LOGGER.info("Running {}...",THREAD_NAME);
		final Stopwatch watch = Stopwatch.createStarted();
		final ApplicationContext ctx = ApplicationContext.getInstance();
		try {
			final GitLabHarvester gitLabHarvester=this.controller.getGitLabHarvester();
			for(final Integer repositoryId:gitLabHarvester.getRepositories()){
				final Repository repository=this.controller.getRepository(Integer.toString(repositoryId));
				addBranchMemberstToRepository(ctx,repositoryId,repository);
				addCommitMembersToRepository(ctx,repositoryId,repository);
			}
		} catch (final Exception e) {
			LOGGER.error("Could not update repository members",e);
		} finally {
			LOGGER.debug("Finalized repository member update process");
		}
		watch.stop();
		LOGGER.info("{} Elapsed time (ms): {}",THREAD_NAME,watch.elapsed(TimeUnit.MILLISECONDS));
	}

	@Override
	public void start() {
		LOGGER.info("Starting {}...",THREAD_NAME);
	}

	private void addBranchMemberstToRepository(final ApplicationContext ctx, final Integer repositoryId, final Repository repository) throws IOException {
		try(WriteSession session=ctx.createSession()) {
			final Name<String> repositoryName = NamingScheme.getDefault().name(Integer.toString(repositoryId));
			final ContainerSnapshot branchContainerSnapshot = session.find(ContainerSnapshot.class,repositoryName,BranchContainerHandler.class);
			if(branchContainerSnapshot!=null){
				for (final String branchId:repository.getBranches().getBranchIds()){
					final Name<String> branchName = NamingScheme.getDefault().name(Integer.toString(repository.getId()),branchId);
					// Keep track of the branch key and resource name
					this.controller.getBranchIdentityMap().addKey(new BranchKey(Integer.toString(repository.getId()),branchId), branchName);
					branchContainerSnapshot.addMember(branchName);
				}
			}
			session.modify(branchContainerSnapshot);
			session.saveChanges();
		} catch(final Exception e) {
			throw new IOException("Could not add branch members to repository "+repositoryId,e);
		}
	}

	private void addCommitMembersToRepository(final ApplicationContext ctx, final Integer repositoryId, final Repository repository) throws IOException {
		try(WriteSession session = ctx.createSession()) {
			final Name<String> repositoryName = NamingScheme.getDefault().name(Integer.toString(repositoryId));
			final ContainerSnapshot commitContainerSnapshot = session.find(ContainerSnapshot.class,repositoryName,CommitContainerHandler.class);
			if(commitContainerSnapshot!=null){
				for(final String commitId:repository.getCommits().getCommitIds()){
					final Name<String> commitName = NamingScheme.getDefault().name(Integer.toString(repository.getId()),commitId);
					// Keep track of the branch key and resource name
					this.controller.getCommitIdentityMap().addKey(new CommitKey(Integer.toString(repository.getId()),commitId), commitName);
					commitContainerSnapshot.addMember(commitName);
				}
			}
			session.modify(commitContainerSnapshot);
			session.saveChanges();
		} catch(final Exception e) {
			throw new IOException("Could not add commit members to repository "+repositoryId,e);
		}
	}

}
