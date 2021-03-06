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
 *   Artifact    : org.smartdeveloperhub.harvesters.scm:scm-harvester-backend:0.3.0
 *   Bundle      : scm-harvester-backend-0.3.0.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.smartdeveloperhub.harvesters.scm.backend.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.ldp4j.commons.testing.Utils;


public class CloseablesTest {

	@Test
	public void verifyIsUtilityClass() {
		assertThat(Utils.isUtilityClass(Closeables.class),equalTo(true));
	}

	@Test
	public void testCloseQuietly$null() {
		Closeables.closeQuietly(null);
	}

	@Test
	public void testCloseQuietly$notNullNoFailure() {
		Closeables.closeQuietly(new AutoCloseable() {
			@Override
			public void close() throws Exception {
			}
		});
	}

	@Test
	public void testCloseQuietly$notNullCheckedException() {
		Closeables.closeQuietly(new AutoCloseable() {
			@Override
			public void close() throws Exception {
				throw new Exception("Exception");
			}
		});
	}

	@Test
	public void testCloseQuietly$notNullUncheckedException() {
		Closeables.closeQuietly(new AutoCloseable() {
			@Override
			public void close() throws Exception {
				throw new RuntimeException("Exception");
			}
		});
	}

}
