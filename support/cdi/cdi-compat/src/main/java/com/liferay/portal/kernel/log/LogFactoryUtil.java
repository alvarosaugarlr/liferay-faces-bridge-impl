/**
 * Copyright (c) 2000-2017 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.portal.kernel.log;

/**
 * Since the Liferay CDI Portlet Bridge has a dependency on the Liferay Portal logging API, this class is necessary to
 * provide a compatibility layer with the Liferay Faces logging API.
 *
 * @author  Neil Griffin
 */
public class LogFactoryUtil {

	public static Log getLog(Class<?> c) {
		return new LogImpl(c);
	}

	public static Log getLog(String name) {
		return new LogImpl(name);
	}

}