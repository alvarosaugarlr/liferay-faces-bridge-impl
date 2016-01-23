/**
 * Copyright (c) 2000-2016 Liferay, Inc. All rights reserved.
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
package com.liferay.faces.bridge.component.icefaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;

import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;


/**
 * <p>This class is part of a workaround for <a href="http://jira.icesoft.org/browse/ICE-6398">ICE-6398</a>.</p>
 *
 * <p>The basic approach is to make sure that the DataPaginator.setData(UIData) method is called ahead of time, so that
 * ICEfaces will not bother to call it's internal CoreComponentUtils.findComponent(String, UIComponent) method.</p>
 *
 * @author  Neil Griffin
 */
public class DataPaginatorBridgeImpl extends DataPaginatorWrapper implements Serializable {

	// serialVersionUID
	private static final long serialVersionUID = 2792168719410424941L;

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(DataPaginatorBridgeImpl.class);

	// Private Constants
	private static final String ATTR_NAME_FOR = "for";
	private static final String DATA_PAGINATOR_GROUP_FQCN =
		"com.icesoft.faces.component.datapaginator.DataPaginatorGroup";

	// Private Data Members
	private Object wrappedDataPaginator;

	public DataPaginatorBridgeImpl(Object dataPaginator) {
		this.wrappedDataPaginator = dataPaginator;
	}

	@Override
	public void decode(FacesContext facesContext) {

		UIData uiData = null;

		try {
			uiData = getUIData();
		}
		catch (Exception e) {
			logger.error(e);
		}

		if (uiData != null) {

			if (!uiData.getAttributes().containsKey(DATA_PAGINATOR_GROUP_FQCN)) {
				uiData.getAttributes().put(DATA_PAGINATOR_GROUP_FQCN, new ArrayList<String>());
			}

			@SuppressWarnings("unchecked")
			List<String> paginatorList = (List<String>) uiData.getAttributes().get(DATA_PAGINATOR_GROUP_FQCN);
			String clientId = getClientId(facesContext);

			if (!paginatorList.contains(clientId)) {
				paginatorList.add(clientId);
			}
		}

		super.decode(facesContext);
	}

	@Override
	public UIData findUIData(FacesContext facesContext) {

		UIData uiData = null;

		// Get the value of the component's "for" attribute.
		ValueExpression valueExpression = getValueExpression(ATTR_NAME_FOR);

		if (valueExpression != null) {
			String forComponentId = (String) valueExpression.getValue(facesContext.getELContext());

			// If the component is "for" a component of type UIData, then return the component that it is for. Otherwise
			// return null.
			if (forComponentId != null) {
				Object forComponent = findComponent(forComponentId);

				if (forComponent == null) {
					forComponent = matchComponentInHierarchy(facesContext, facesContext.getViewRoot(), forComponentId);
				}

				if (forComponent != null) {

					if (forComponent instanceof UIData) {
						uiData = (UIData) forComponent;
					}
					else {
						throw new IllegalArgumentException(
							"for attribute must be an instance of javax.faces.component.UIData");
					}
				}
			}
		}

		return uiData;
	}

	@Override
	public void processDecodes(FacesContext facesContext) {

		try {
			setUIData(findUIData(facesContext));
		}
		catch (Exception e) {
			logger.error(e);
		}

		super.processDecodes(facesContext);
	}

	protected UIComponent matchComponentInHierarchy(FacesContext facesContext, UIComponent parent,
		String partialClientId) {
		UIComponent uiComponent = null;

		if (parent != null) {

			String parentClientId = parent.getClientId(facesContext);

			if ((parentClientId != null) && (parentClientId.indexOf(partialClientId) >= 0)) {
				uiComponent = parent;
			}
			else {
				Iterator<UIComponent> itr = parent.getFacetsAndChildren();

				if (itr != null) {

					while (itr.hasNext()) {
						UIComponent child = itr.next();
						uiComponent = matchComponentInHierarchy(facesContext, child, partialClientId);

						if (uiComponent != null) {
							break;
						}
					}
				}
			}
		}

		return uiComponent;
	}

	@Override
	public Object getWrapped() {
		return wrappedDataPaginator;
	}
}
