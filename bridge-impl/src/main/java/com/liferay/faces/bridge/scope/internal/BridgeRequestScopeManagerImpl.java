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
package com.liferay.faces.bridge.scope.internal;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.faces.Bridge;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.liferay.faces.bridge.BridgeFactoryFinder;
import com.liferay.faces.bridge.servlet.BridgeSessionListener;
import com.liferay.faces.util.cache.Cache;
import com.liferay.faces.util.cache.CacheFactory;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;


/**
 * @author  Neil Griffin
 */
public class BridgeRequestScopeManagerImpl implements BridgeRequestScopeManager {

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(BridgeRequestScopeManagerImpl.class);

	// Private Constants
	private static final String ATTR_BRIDGE_REQUEST_SCOPE_CACHE = "com.liferay.faces.bridge.bridgeRequestScopeCache";

	@Override
	public Cache<String, BridgeRequestScope> getBridgeRequestScopeCache(PortletContext portletContext) {

		Cache<String, BridgeRequestScope> bridgeRequestScopeCache;

		synchronized (portletContext) {

			bridgeRequestScopeCache = (Cache<String, BridgeRequestScope>) portletContext.getAttribute(
					ATTR_BRIDGE_REQUEST_SCOPE_CACHE);

			if (bridgeRequestScopeCache == null) {

				CacheFactory cacheFactory = (CacheFactory) BridgeFactoryFinder.getFactory(portletContext,
						CacheFactory.class);

				// Spec Section 3.2: Support for configuration of maximum number of bridge request scopes.
				Integer maxManagedRequestScope = null;
				String maxManagedRequestScopeString = portletContext.getInitParameter(
						Bridge.MAX_MANAGED_REQUEST_SCOPES);

				if (maxManagedRequestScopeString != null) {

					try {
						maxManagedRequestScope = Integer.parseInt(maxManagedRequestScopeString);
					}
					catch (NumberFormatException e) {
						logger.error("Unable to parse portlet.xml init-param name=[{0}] error=[{1}]",
							Bridge.MAX_MANAGED_REQUEST_SCOPES, e.getMessage());
					}
				}

				if (maxManagedRequestScope != null) {

					int initialCacheCapacity = cacheFactory.getDefaultInitialCapacity();
					bridgeRequestScopeCache = cacheFactory.getConcurrentCache(initialCacheCapacity,
							maxManagedRequestScope);
				}
				else {
					bridgeRequestScopeCache = cacheFactory.getConcurrentCache();
				}

				portletContext.setAttribute(ATTR_BRIDGE_REQUEST_SCOPE_CACHE, bridgeRequestScopeCache);
			}
		}

		return bridgeRequestScopeCache;
	}

	@Override
	public void removeBridgeRequestScopesByPortlet(PortletConfig portletConfig) {

		String portletNameToRemove = portletConfig.getPortletName();
		PortletContext portletContext = portletConfig.getPortletContext();
		Cache<String, BridgeRequestScope> bridgeRequestScopeCache = getBridgeRequestScopeCache(portletContext);
		removeBridgeRequestScopes(bridgeRequestScopeCache, true, portletNameToRemove);
	}

	/**
	 * This method is designed to be invoked from a {@link javax.servlet.http.HttpSessionListener} like {@link
	 * BridgeSessionListener} when a session timeout/expiration occurs. The logic in this method is a little awkward
	 * because we have to try and remove BridgeRequestScope instances from {@link Cache} instances in the {@link
	 * ServletContext} rather than the {@link PortletContext} because we only have access to the Servlet-API when
	 * sessions expire.
	 */
	@Override
	public void removeBridgeRequestScopesBySession(HttpSession httpSession) {

		ServletContext servletContext = httpSession.getServletContext();
		Enumeration<String> attributeNames = servletContext.getAttributeNames();
		String sessionId = httpSession.getId();

		while (attributeNames.hasMoreElements()) {

			String attributeName = attributeNames.nextElement();
			Object attribute = servletContext.getAttribute(attributeName);

			if (attribute instanceof Cache) {

				boolean bridgeRequestScopeCacheFound = false;
				Cache cache = (Cache) attribute;
				Set keySet = cache.keySet();
				Iterator iterator = keySet.iterator();

				while (iterator.hasNext()) {

					Object key = iterator.next();

					if (key instanceof String) {

						Object value = cache.get(key);

						if (value instanceof BridgeRequestScope) {

							bridgeRequestScopeCacheFound = true;

							break;
						}
						else {
							break;
						}
					}
					else {
						break;
					}
				}

				if (bridgeRequestScopeCacheFound) {
					removeBridgeRequestScopes(cache, false, sessionId);
				}
			}
		}
	}

	private void removeBridgeRequestScopes(Cache bridgeRequestScopeCache, boolean removeByPortletId,
		String portletOrSessionId) {

		int indexOfSessionIdSection = -1;

		// Iterate over the map entries, and build up a list of BridgeRequestScope keys that are to be
		// removed. Doing it this way prevents ConcurrentModificationExceptions from being thrown.
		List<String> keysToRemove = new ArrayList<String>();
		Set<String> keySet = (Set<String>) bridgeRequestScopeCache.keySet();
		String portletOrSessionIdWithSeparatorSuffix = portletOrSessionId + ":::";

		for (String bridgeRequestScopeId : keySet) {

			if (removeByPortletId) {

				if (bridgeRequestScopeId.startsWith(portletOrSessionIdWithSeparatorSuffix)) {
					keysToRemove.add(bridgeRequestScopeId);
				}
			}
			else {

				if (indexOfSessionIdSection < 0) {
					indexOfSessionIdSection = bridgeRequestScopeId.indexOf(":::") + ":::".length();
				}

				String idWithoutPortletNamePrefix = bridgeRequestScopeId.substring(indexOfSessionIdSection);

				if (idWithoutPortletNamePrefix.startsWith(portletOrSessionIdWithSeparatorSuffix)) {
					keysToRemove.add(bridgeRequestScopeId);
				}
			}
		}

		for (String keyToRemove : keysToRemove) {

			Object bridgeRequestScope = bridgeRequestScopeCache.remove(keyToRemove);

			if (!removeByPortletId) {
				logger.debug(
					"Removed bridgeRequestScopeId=[{0}] bridgeRequestScope=[{1}] from cache due to session timeout",
					keyToRemove, bridgeRequestScope);
			}
		}
	}
}
