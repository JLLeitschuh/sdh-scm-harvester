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
 *   Artifact    : org.smartdeveloperhub.harvesters.scm:scm-harvester-testing:0.3.0
 *   Bundle      : scm-harvester-testing-0.3.0.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.smartdeveloperhub.harvesters.scm.testing.enhancer;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.smartdeveloperhub.harvesters.scm.backend.notification.CommitterCreatedEvent;
import org.smartdeveloperhub.harvesters.scm.backend.notification.CommitterDeletedEvent;
import org.smartdeveloperhub.harvesters.scm.backend.notification.Event;
import org.smartdeveloperhub.harvesters.scm.backend.notification.GitCollector;
import org.smartdeveloperhub.harvesters.scm.backend.notification.RepositoryCreatedEvent;
import org.smartdeveloperhub.harvesters.scm.backend.notification.RepositoryDeletedEvent;
import org.smartdeveloperhub.harvesters.scm.backend.notification.RepositoryUpdatedEvent;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.Branch;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.Commit;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.Enhancer;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.Repository;
import org.smartdeveloperhub.harvesters.scm.backend.pojos.User;
import org.smartdeveloperhub.harvesters.scm.testing.enhancer.ActivityTracker.ActivityContext;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class GitLabEnhancer {

	private final class InnerOwner implements ActivityContext {

		@Override
		public String resolve(final String path, final Object... args) {
			return GitLabEnhancer.this.base.resolve(String.format(path,args)).toString();
		}

		@Override
		public void onActivity(final Activity<?> activity) {
			final ActivityListener current=GitLabEnhancer.this.listener;
			if(current!=null) {
				current.onActivity(activity);
			}
		}

	}

	public static class UpdateReport {

		private Event event;
		private final List<String> warnings;
		private IOException failure;
		private String failureMessage;
		private final List<Event> sideEffects;

		UpdateReport() {
			this.warnings=Lists.newArrayList();
			this.sideEffects=Lists.newArrayList();
		}

		UpdateReport curatedEvent(final Event event) {
			this.event = event;
			return this;
		}

		UpdateReport sideEffect(final Event event) {
			this.sideEffects.add(event);
			return this;
		}

		UpdateReport updateFailed(final IOException failure, final String message, final Object...args) {
			this.failure = failure;
			this.failureMessage=String.format(message,args);
			return this;
		}

		UpdateReport updateFailed(final IOException failure) {
			this.failure = failure;
			this.failureMessage="";
			return this;
		}

		UpdateReport warn(final String message, final Object... args) {
			this.warnings.add(String.format(message,args));
			return this;
		}

		public boolean enhancerUpdated() {
			return this.event!=null;
		}

		public boolean notificationSent() {
			return enhancerUpdated() && this.failure==null;
		}

		public String updateFailure() {
			return
				this.failureMessage+
				(this.failureMessage.isEmpty()?"":"\n")+
				Throwables.getStackTraceAsString(this.failure);
		}

		public Event curatedEvent() {
			return this.event;
		}

		public List<Event> sideEffects() {
			return Collections.unmodifiableList(this.sideEffects);
		}

		public List<String> warnings() {
			return Collections.unmodifiableList(this.warnings);
		}

	}

	private final Map<String,CommitterState> committers;
	private final Map<String,RepositoryState> repositories;
	private final GitCollector collector;
	private final URI base;

	private ActivityListener listener;

	private GitLabEnhancer(final GitCollector collector, final URI base) {
		this.collector = collector;
		this.base = base;
		this.committers=Maps.newLinkedHashMap();
		this.repositories=Maps.newLinkedHashMap();
	}

	public GitLabEnhancer registerListener(final ActivityListener listener) {
		this.listener = listener;
		return this;
	}

	public Enhancer getEnhancer(final String instance) {
		final Enhancer en = new Enhancer();
		en.setId(instance);
		en.setName("Testing GitLab Enhancer Service");
		en.setStatus("OK");
		en.setVersion("0.1.0");
		en.getCollectors().add(this.collector.getConfig());
		return en;
	}

	public List<String> getCommitters() {
		return ImmutableList.copyOf(this.committers.keySet());
	}

	public User getCommitter(final String committerId) {
		return findCommitter(committerId).getRepresentation();
	}

	public List<String> getRepositories() {
		return ImmutableList.copyOf(this.repositories.keySet());
	}

	public Repository getRepository(final String repositoryId) {
		return findRepository(repositoryId).getRepresentation();
	}

	public List<String> getRepositoryBranches(final String repositoryId) {
		return findRepository(repositoryId).branches();
	}

	public List<String> getRepositoryCommits(final String repositoryId) {
		return findRepository(repositoryId).commits();
	}

	public Commit getRepositoryCommit(final String repositoryId, final String commitId) {
		return findRepository(repositoryId).commit(commitId).getRepresentation();
	}

	public Branch getRepositoryBranch(final String repositoryId, final String name) {
		return findRepository(repositoryId).branch(name).getRepresentation();
	}

	public List<String> getRepositoryBranchCommits(final String repositoryId, final String name) {
		return findRepository(repositoryId).branch(name).commits();
	}

	public UpdateReport update(final Event event) {
		Reports.remove();
		ActivityTracker.useContext(new InnerOwner());
		try {
			updateEnhancer(event);
			notifyUpdate(Reports.currentReport().curatedEvent());
			for(final Event sideEffect:Reports.currentReport().sideEffects()) {
				notifyUpdate(sideEffect);
			}
			return Reports.currentReport();
		} finally {
			ActivityTracker.remove();
		}
	}

	private void notifyUpdate(final Event event) {
		if(event!=null) {
			event.setInstance(this.collector.getInstance());
			event.setTimestamp(System.currentTimeMillis());
			try {
				this.collector.notify(event);
			} catch (final IOException e) {
				Reports.currentReport().updateFailed(e);
			}
		}
	}

	private void updateEnhancer(final Event event) {
		if(event instanceof CommitterCreatedEvent) {
			createCommitters((CommitterCreatedEvent)event);
		} else if(event instanceof CommitterDeletedEvent) {
			deleteCommitters((CommitterDeletedEvent)event);
		} else if(event instanceof RepositoryCreatedEvent) {
			createRepositories((RepositoryCreatedEvent)event);
		} else if(event instanceof RepositoryDeletedEvent) {
			deleteRepositories((RepositoryDeletedEvent)event);
		} else if(event instanceof RepositoryUpdatedEvent) {
			updateRepository((RepositoryUpdatedEvent)event);
		} else {
			Reports.
				currentReport().
					updateFailed(
						new IOException("Unsupported event type "+event.getClass().getSimpleName()));
		}
	}

	private CommitterState findCommitter(final String committerId) {
		CommitterState state = this.committers.get(committerId);
		if(state==null) {
			Reports.currentReport().warn("Committer %s does not exist",committerId);
			state=new NullCommitterState(committerId);
		}
		return state;
	}

	private RepositoryState findRepository(final String repositoryId) {
		RepositoryState state = this.repositories.get(repositoryId);
		if(state==null) {
			Reports.currentReport().warn("Repository %s does not exist",repositoryId);
			state=new NullRepositoryState(repositoryId);
		}
		return state;
	}

	private void  updateRepository(final RepositoryUpdatedEvent event) {
		final UpdateReport report = Reports.currentReport();

		final RepositoryState state = this.repositories.get(event.getRepository());
		if(state==null) {
			Reports.currentReport().warn("Repository %s does not exist",event.getRepository());
			return;
		}

		final RepositoryUpdatedEvent curated=new RepositoryUpdatedEvent();
		curated.setRepository(event.getRepository());
		curated.getContributors().addAll(event.getContributors());
		curated.getContributors().retainAll(getCommitters());

		for(final String id:event.getDeletedBranches()) {
			if(state.deleteBranch(id)) {
				curated.getDeletedBranches().add(id);
			}
		}
		for(final String id:event.getDeletedCommits()) {
			if(state.deleteCommit(id)) {
				curated.getDeletedCommits().add(id);
			}
		}

		final List<String> dismissed=Lists.newArrayList(event.getContributors());
		dismissed.removeAll(curated.getContributors());
		if(!dismissed.isEmpty()) {
			report.warn("Dismissing non-existing contributors (%s)",Joiner.on(", ").join(dismissed));
		}

		if(!curated.getContributors().isEmpty()) {
			final Set<String> contributingContributors=Sets.newHashSet();
			final Iterator<String> candidateContributors=Iterators.cycle(curated.getContributors());
			for(final String id:event.getNewBranches()) {
				final String contributor=candidateContributors.next();
				if(state.createBranch(id)) {
					curated.getNewBranches().add(id);
					contributingContributors.add(contributor);
				}
			}
			for(final String id:event.getNewCommits()) {
				final String contributor=candidateContributors.next();
				if(state.createCommit(id,this.committers.get(contributor))) {
					curated.getNewCommits().add(id);
					contributingContributors.add(contributor);
				}
			}
			curated.getContributors().retainAll(contributingContributors);
		} else {
			logDismissal(event.getNewBranches(), "branches");
			logDismissal(event.getNewCommits(), "commits");
		}
		if (!curated.getDeletedBranches().isEmpty() ||
			!curated.getDeletedCommits().isEmpty()  ||
			!curated.getNewBranches().isEmpty()     ||
			!curated.getNewCommits().isEmpty()) {
			report.curatedEvent(curated);
		}
	}

	private void logDismissal(final List<String> members, final String memberType) {
		if(!members.isEmpty()) {
			Reports.
				currentReport().
					warn(
						"Dismissing new %s (%s) as no existing contributors were specified",
						memberType,
						Joiner.on(", ").join(members));
		}
	}

	/**
	 * NOTE: What happens to all the committers related to the repositories?
	 */
	private void deleteRepositories(final RepositoryDeletedEvent event) {
		final UpdateReport report = Reports.currentReport();
		final RepositoryDeletedEvent curated=new RepositoryDeletedEvent();
		for(final String repositoryId:event.getDeletedRepositories()) {
			final RepositoryState repository = this.repositories.remove(repositoryId);
			if(repository!=null) {
				curated.getDeletedRepositories().add(repositoryId);
				ActivityTracker.currentTracker().deleted(repository);
				ActivityTracker.currentTracker().log("Deleted repository %s (%s)",repository.getId(),repository.getName());
			} else {
				report.warn("Repository %s does not exist",repositoryId);
			}
		}
		if(!curated.getDeletedRepositories().isEmpty()) {
			report.curatedEvent(curated);
		}
	}

	private void createRepositories(final RepositoryCreatedEvent event) {
		final UpdateReport report = Reports.currentReport();
		if(!this.committers.isEmpty()) {
			final RepositoryCreatedEvent curated=new RepositoryCreatedEvent();
			final Iterator<String> committerIds = Iterators.cycle(this.committers.keySet());
			for(final String repositoryId:event.getNewRepositories()) {
				if(!this.repositories.containsKey(repositoryId)) {
					final CommitterState owner = this.committers.get(committerIds.next());
					final RepositoryState repository =
						new ImmutableRepositoryState(
							repositoryId,
							owner);
					repository.createBranch("1");
					this.repositories.put(repositoryId, repository);
					curated.getNewRepositories().add(repositoryId);
					final RepositoryUpdatedEvent event2 = new RepositoryUpdatedEvent();
					event2.setRepository(repositoryId);
					event2.getNewBranches().add("1");
					event2.getContributors().add(owner.getId());
					report.sideEffect(event2);
				} else {
					report.warn("Repository %s already exists",repositoryId);
				}
			}
			if(!curated.getNewRepositories().isEmpty()) {
				report.curatedEvent(curated);
			}
		}
	}

	/**
	 * NOTE: What happens to all the artifacts associated to this committers?
	 */
	private void deleteCommitters(final CommitterDeletedEvent event) {
		final UpdateReport report = Reports.currentReport();
		final CommitterDeletedEvent curated=new CommitterDeletedEvent();
		for(final String committerId:event.getDeletedCommitters()) {
			final CommitterState committer = this.committers.remove(committerId);
			if(committer!=null) {
				curated.getDeletedCommitters().add(committerId);
				ActivityTracker.currentTracker().deleted(committer);
				ActivityTracker.currentTracker().log("Deleted committer %s (%s)",committerId,committer.getName());
			} else {
				report.warn("Committer %s does not exist",committerId);
			}
		}
		if(!curated.getDeletedCommitters().isEmpty()) {
			report.curatedEvent(curated);
		}
	}

	private void createCommitters(final CommitterCreatedEvent event) {
		final UpdateReport report = Reports.currentReport();
		final CommitterCreatedEvent curated=new CommitterCreatedEvent();
		for(final String committerId:event.getNewCommitters()) {
			if(!this.committers.containsKey(committerId)) {
				final CommitterState committer = new ImmutableCommitterState(committerId);
				this.committers.put(committerId, committer);
				curated.getNewCommitters().add(committerId);
			} else {
				report.warn("Committer %s already exists",committerId);
			}
		}
		if(!curated.getNewCommitters().isEmpty()) {
			report.curatedEvent(curated);
		}
	}

	public static GitLabEnhancer newInstance(final GitCollector collector, final URI base) {
		return new GitLabEnhancer(collector,base);
	}

}