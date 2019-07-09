/**
 * Copyright (c) 2000-2019 Liferay, Inc. All rights reserved.
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
package com.liferay.faces.demos.applicant.primefaces.facelets.mbf;

import java.io.File;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.portlet.PortletSession;

import org.primefaces.event.FileUploadEvent;

import com.liferay.faces.demos.applicant.primefaces.facelets.dto.Applicant;
import com.liferay.faces.demos.applicant.primefaces.facelets.dto.Attachment;
import com.liferay.faces.demos.applicant.primefaces.facelets.dto.City;
import com.liferay.faces.demos.applicant.primefaces.facelets.dto.UploadedFileWrapper;
import com.liferay.faces.util.context.FacesContextHelperUtil;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;
import com.liferay.faces.util.model.UploadedFile;


/**
 * This is a JSF backing managed-bean for the applicant.xhtml composition.
 *
 * @author  "Neil Griffin"
 */
@ManagedBean(name = "applicantBacking")
@RequestScoped
public class ApplicantBacking {

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(ApplicantBacking.class);

	// Injections
	@ManagedProperty(value = "#{applicantView}")
	private ApplicantView applicantView;
	@ManagedProperty(value = "#{attachmentManager}")
	private AttachmentManager attachmentManager;
	@ManagedProperty(value = "#{listManager}")
	private ListManager listManager;

	public void deleteUploadedFile(ActionEvent actionEvent) {

		try {
			List<Attachment> attachments = applicant.getAttachments();

			String attachmentId = applicantView.getUploadedFileId();

			Attachment attachmentToDelete = null;

			for (Attachment attachment : attachments) {

				if (attachment.getId().equals(attachmentId)) {
					attachmentToDelete = attachment;

					break;
				}
			}

			if (attachmentToDelete != null) {
				attachmentToDelete.getFile().delete();
				attachments.remove(attachmentToDelete);
				logger.debug("Deleted file=[{0}]", attachmentToDelete.getName());
			}
		}
		catch (Exception e) {
			logger.error(e);
		}
	}

	public Applicant getModel() {
		return applicant;
	}

	public void handleFileUpload(FileUploadEvent event) {
		List<Attachment> attachments = applicant.getAttachments();
		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
		PortletSession portletSession = (PortletSession) externalContext.getSession(false);
		String uniqueFolderName = portletSession.getId();
		org.primefaces.model.UploadedFile attachment = event.getFile();
		UploadedFileWrapper attachmentWrapper = new UploadedFileWrapper(attachment, UploadedFile.Status.FILE_SAVED,
				uniqueFolderName);
		attachments.add(attachmentWrapper);
		logger.debug("Received fileName=[{0}] absolutePath=[{1}]", attachmentWrapper.getName(),
			attachmentWrapper.getAbsolutePath());
	}

	public void postalCodeListener(ValueChangeEvent valueChangeEvent) {

		try {
			String newPostalCode = (String) valueChangeEvent.getNewValue();
			City city = listManager.getCityByPostalCode(newPostalCode);

			if (city != null) {
				applicant.setAutoFillCity(city.getCityName());
				applicant.setAutoFillProvinceId(city.getProvinceId());
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			FacesContextHelperUtil.addGlobalUnexpectedErrorMessage();
		}
	}

	@PostConstruct
	public void postConstruct() {
		applicant = new Applicant();

		FacesContext facesContext = FacesContext.getCurrentInstance();
		File attachmentDir = attachmentManager.getAttachmentDir(facesContext);
		List<Attachment> attachments = attachmentManager.getAttachments(attachmentDir);
		applicant.setAttachments(attachments);
	}

	public void setApplicant(Applicant applicant) {

		// Injected via @ManagedProperty annotation
		this.applicant = applicant;
	}

	public void setApplicantView(ApplicantView applicantView) {

		// Injected via @ManagedProperty annotation
		this.applicantView = applicantView;
	}

	public void setAttachmentManager(AttachmentManager attachmentManager) {

		// Injected via @ManagedProperty annotation
		this.attachmentManager = attachmentManager;
	}

	public void setAttachmentManager(AttachmentManager attachmentManager) {

		// Injected via @ManagedProperty annotation
		this.attachmentManager = attachmentManager;
	}

	public void setListManager(ListManager listManager) {

		// Injected via @ManagedProperty annotation
		this.listManager = listManager;
	}

	public String submit() {

		if (logger.isDebugEnabled()) {
			logger.debug("firstName=" + applicant.getFirstName());
			logger.debug("lastName=" + applicant.getLastName());
			logger.debug("emailAddress=" + applicant.getEmailAddress());
			logger.debug("phoneNumber=" + applicant.getPhoneNumber());
			logger.debug("dateOfBirth=" + applicant.getDateOfBirth());
			logger.debug("city=" + applicant.getCity());
			logger.debug("provinceId=" + applicant.getProvinceId());
			logger.debug("postalCode=" + applicant.getPostalCode());
			logger.debug("comments=" + applicant.getComments());

			List<Attachment> attachments = applicant.getAttachments();

			for (Attachment attachment : attachments) {
				logger.debug("attachment=[{0}]", attachment.getName());
			}
		}

		// Delete the uploaded files.
		try {
			List<Attachment> attachments = applicant.getAttachments();

			for (Attachment attachment : attachments) {
				attachment.getFile().delete();
				logger.debug("Deleted file=[{0}]", attachment.getName());
			}

			// Store the applicant's first name in JSF 2 Flash Scope so that it can be picked up
			// for use inside of confirmation.xhtml
			FacesContext facesContext = FacesContext.getCurrentInstance();
			facesContext.getExternalContext().getFlash().put("firstName", applicant.getFirstName());

			applicant.clearProperties();

			return "success";

		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			FacesContextHelperUtil.addGlobalUnexpectedErrorMessage();

			return "failure";
		}
	}
}
