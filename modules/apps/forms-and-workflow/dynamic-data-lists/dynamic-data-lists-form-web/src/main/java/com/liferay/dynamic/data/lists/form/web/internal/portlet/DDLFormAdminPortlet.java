/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.lists.form.web.internal.portlet;

import com.liferay.dynamic.data.lists.form.web.configuration.DDLFormWebConfigurationActivator;
import com.liferay.dynamic.data.lists.form.web.constants.DDLFormPortletKeys;
import com.liferay.dynamic.data.lists.form.web.internal.display.context.DDLFormAdminDisplayContext;
import com.liferay.dynamic.data.lists.model.DDLRecordSet;
import com.liferay.dynamic.data.lists.model.DDLRecordSetSettings;
import com.liferay.dynamic.data.lists.service.DDLRecordLocalService;
import com.liferay.dynamic.data.lists.service.DDLRecordSetLocalService;
import com.liferay.dynamic.data.lists.service.DDLRecordSetService;
import com.liferay.dynamic.data.mapping.constants.DDMWebKeys;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeServicesTracker;
import com.liferay.dynamic.data.mapping.form.renderer.DDMFormRenderer;
import com.liferay.dynamic.data.mapping.form.renderer.DDMFormRenderingContext;
import com.liferay.dynamic.data.mapping.form.values.factory.DDMFormValuesFactory;
import com.liferay.dynamic.data.mapping.io.DDMFormFieldTypesJSONSerializer;
import com.liferay.dynamic.data.mapping.io.DDMFormJSONSerializer;
import com.liferay.dynamic.data.mapping.io.DDMFormLayoutJSONSerializer;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormLayout;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.storage.StorageEngine;
import com.liferay.dynamic.data.mapping.util.DDMFormFactory;
import com.liferay.dynamic.data.mapping.util.DDMFormLayoutFactory;
import com.liferay.dynamic.data.mapping.util.DDMFormValuesMerger;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowEngineManager;

import java.io.IOException;

import java.util.Map;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Bruno Basto
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.css-class-wrapper=portlet-forms-admin",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.header-portlet-css=/admin/css/main.css",
		"com.liferay.portlet.icon=/admin/icons/form.png",
		"com.liferay.portlet.instanceable=false",
		"com.liferay.portlet.preferences-owned-by-group=true",
		"com.liferay.portlet.private-request-attributes=false",
		"com.liferay.portlet.private-session-attributes=false",
		"com.liferay.portlet.render-weight=50",
		"com.liferay.portlet.use-default-template=true",
		"javax.portlet.display-name=Forms", "javax.portlet.expiration-cache=0",
		"javax.portlet.init-param.template-path=/admin/",
		"javax.portlet.init-param.view-template=/admin/view.jsp",
		"javax.portlet.name=" + DDLFormPortletKeys.DYNAMIC_DATA_LISTS_FORM_ADMIN,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user",
		"javax.portlet.supports.mime-type=text/html"
	},
	service = Portlet.class
)
public class DDLFormAdminPortlet extends MVCPortlet {

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		try {
			setRenderRequestAttributes(renderRequest, renderResponse);
		}
		catch (Exception e) {
			if (isSessionErrorException(e)) {
				if (_log.isWarnEnabled()) {
					_log.warn(e, e);
				}

				hideDefaultErrorMessage(renderRequest);

				SessionErrors.add(renderRequest, e.getClass());
			}
			else {
				_log.error(e, e);

				throw new PortletException(e);
			}
		}

		super.render(renderRequest, renderResponse);
	}

	protected DDMFormRenderingContext createDDMFormRenderingContext(
		RenderRequest renderRequest, RenderResponse renderResponse) {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		DDMFormRenderingContext ddmFormRenderingContext =
			new DDMFormRenderingContext();

		ddmFormRenderingContext.setHttpServletRequest(
			_portal.getHttpServletRequest(renderRequest));
		ddmFormRenderingContext.setHttpServletResponse(
			_portal.getHttpServletResponse(renderResponse));
		ddmFormRenderingContext.setContainerId("settings");
		ddmFormRenderingContext.setLocale(themeDisplay.getLocale());
		ddmFormRenderingContext.setPortletNamespace(
			renderResponse.getNamespace());

		return ddmFormRenderingContext;
	}

	protected DDMForm createSettingsDDMForm(
			long recordSetId, ThemeDisplay themeDisplay)
		throws PortalException {

		DDMForm ddmForm = DDMFormFactory.create(DDLRecordSetSettings.class);

		ddmForm.addAvailableLocale(themeDisplay.getLocale());
		ddmForm.setDefaultLocale(themeDisplay.getLocale());

		Map<String, DDMFormField> ddmFormFieldsMap =
			ddmForm.getDDMFormFieldsMap(false);

		// Storage type

		DDMFormField ddmFormField = ddmFormFieldsMap.get("storageType");

		if (recordSetId > 0) {
			ddmFormField.setReadOnly(true);
		}

		return ddmForm;
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	protected void setDDLFormWebConfigurationActivator(
		DDLFormWebConfigurationActivator ddlFormWebConfigurationActivator) {

		_ddlFormWebConfigurationActivator = ddlFormWebConfigurationActivator;
	}

	@Reference(unbind = "-")
	protected void setDDLRecordLocalService(
		DDLRecordLocalService ddlRecordLocalService) {

		_ddlRecordLocalService = ddlRecordLocalService;
	}

	@Reference(unbind = "-")
	protected void setDDLRecordSetLocalService(
		DDLRecordSetLocalService ddlRecordSetLocalService) {

		_ddlRecordSetLocalService = ddlRecordSetLocalService;
	}

	@Reference(unbind = "-")
	protected void setDDLRecordSetService(
		DDLRecordSetService ddlRecordSetService) {

		_ddlRecordSetService = ddlRecordSetService;
	}

	@Reference(unbind = "-")
	protected void setDDMFormFieldTypeServicesTracker(
		DDMFormFieldTypeServicesTracker ddmFormFieldTypeServicesTracker) {

		_ddmFormFieldTypeServicesTracker = ddmFormFieldTypeServicesTracker;
	}

	@Reference(unbind = "-")
	protected void setDDMFormFieldTypesJSONSerializer(
		DDMFormFieldTypesJSONSerializer ddmFormFieldTypesJSONSerializer) {

		_ddmFormFieldTypesJSONSerializer = ddmFormFieldTypesJSONSerializer;
	}

	@Reference(unbind = "-")
	protected void setDDMFormJSONSerializer(
		DDMFormJSONSerializer ddmFormJSONSerializer) {

		_ddmFormJSONSerializer = ddmFormJSONSerializer;
	}

	@Reference(unbind = "-")
	protected void setDDMFormLayoutJSONSerializer(
		DDMFormLayoutJSONSerializer ddmFormLayoutJSONSerializer) {

		_ddmFormLayoutJSONSerializer = ddmFormLayoutJSONSerializer;
	}

	@Reference(unbind = "-")
	protected void setDDMFormRenderer(DDMFormRenderer ddmFormRenderer) {
		_ddmFormRenderer = ddmFormRenderer;
	}

	protected void setDDMFormRenderingContextDDMFormValues(
			DDMFormRenderingContext ddmFormRenderingContext, DDMForm ddmForm,
			long recordSetId)
		throws PortalException {

		DDLRecordSet recordSet = _ddlRecordSetLocalService.fetchRecordSet(
			recordSetId);

		if (recordSet == null) {
			return;
		}

		DDMFormValues ddmFormValues = recordSet.getSettingsDDMFormValues();

		ddmFormRenderingContext.setDDMFormValues(ddmFormValues);
	}

	@Reference(unbind = "-")
	protected void setDDMFormValuesFactory(
		DDMFormValuesFactory ddmFormValuesFactory) {

		_ddmFormValuesFactory = ddmFormValuesFactory;
	}

	@Reference(unbind = "-")
	protected void setDDMFormValuesMerger(
		DDMFormValuesMerger ddmFormValuesMerger) {

		_ddmFormValuesMerger = ddmFormValuesMerger;
	}

	@Reference(unbind = "-")
	protected void setDDMStructureLocalService(
		DDMStructureLocalService ddmStructureLocalService) {

		_ddmStructureLocalService = ddmStructureLocalService;
	}

	@Reference(unbind = "-")
	protected void setJSONFactory(JSONFactory jsonFactory) {
		_jsonFactory = jsonFactory;
	}

	protected void setRenderRequestAttributes(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortalException {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long recordSetId = ParamUtil.getLong(renderRequest, "recordSetId");

		DDMForm ddmForm = createSettingsDDMForm(recordSetId, themeDisplay);

		DDMFormRenderingContext ddmFormRenderingContext =
			createDDMFormRenderingContext(renderRequest, renderResponse);

		setDDMFormRenderingContextDDMFormValues(
			ddmFormRenderingContext, ddmForm, recordSetId);

		DDMFormLayout ddmFormLayout = DDMFormLayoutFactory.create(
			DDLRecordSetSettings.class);

		ddmFormLayout.setPaginationMode(DDMFormLayout.TABBED_MODE);

		String ddmFormHTML = _ddmFormRenderer.render(
			ddmForm, ddmFormLayout, ddmFormRenderingContext);

		renderRequest.setAttribute(
			DDMWebKeys.DYNAMIC_DATA_MAPPING_FORM_HTML, ddmFormHTML);

		DDLFormAdminDisplayContext ddlFormAdminDisplayContext =
			new DDLFormAdminDisplayContext(
				renderRequest, renderResponse,
				_ddlFormWebConfigurationActivator.getDDLFormWebConfiguration(),
				_ddlRecordLocalService, _ddlRecordSetService,
				_ddmFormFieldTypeServicesTracker,
				_ddmFormFieldTypesJSONSerializer, _ddmFormJSONSerializer,
				_ddmFormLayoutJSONSerializer, _ddmFormRenderer,
				_ddmFormValuesFactory, _ddmFormValuesMerger,
				_ddmStructureLocalService, _jsonFactory, _storageEngine,
				_workflowEngineManager);

		renderRequest.setAttribute(
			WebKeys.PORTLET_DISPLAY_CONTEXT, ddlFormAdminDisplayContext);
	}

	@Reference(unbind = "-")
	protected void setStorageEngine(StorageEngine storageEngine) {
		_storageEngine = storageEngine;
	}

	@Reference(unbind = "-")
	protected void setWorkflowEngineManager(
		WorkflowEngineManager workflowEngineManager) {

		_workflowEngineManager = workflowEngineManager;
	}

	protected void unsetDDLFormWebConfigurationActivator(
		DDLFormWebConfigurationActivator ddlFormWebConfigurationActivator) {

		_ddlFormWebConfigurationActivator = null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DDLFormAdminPortlet.class);

	private DDLFormWebConfigurationActivator _ddlFormWebConfigurationActivator;
	private DDLRecordLocalService _ddlRecordLocalService;
	private DDLRecordSetLocalService _ddlRecordSetLocalService;
	private DDLRecordSetService _ddlRecordSetService;
	private DDMFormFieldTypeServicesTracker _ddmFormFieldTypeServicesTracker;
	private DDMFormFieldTypesJSONSerializer _ddmFormFieldTypesJSONSerializer;
	private DDMFormJSONSerializer _ddmFormJSONSerializer;
	private DDMFormLayoutJSONSerializer _ddmFormLayoutJSONSerializer;
	private DDMFormRenderer _ddmFormRenderer;
	private DDMFormValuesFactory _ddmFormValuesFactory;
	private DDMFormValuesMerger _ddmFormValuesMerger;
	private DDMStructureLocalService _ddmStructureLocalService;
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

	private StorageEngine _storageEngine;
	private WorkflowEngineManager _workflowEngineManager;

}