/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.cstudio.alfresco.to;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;


/**
 * site notification configuration
 * 
 * @author hyanghee
 * 
 */
public class NotificationConfigTO implements TimeStamped {

	/** site key **/
	protected String _site = null;
	/** canned messages **/
	protected Map<String, List<MessageTO>> _cannedMessages = null;
	/** email template messages **/
	protected Map<String, EmailMessageTemplateTO> _emailMessageTemplates = null;
	/** complete messages used for displaying complete pop-ups **/
	protected Map<String, String> _completeMessages = null;
	/** general messages **/
	protected Map<String, String> messages = null;

    /** send notice on action mapping **/
    protected Map<String, Boolean> _sendNoticeMapping = null;
    
    protected Map<String, String> errorMessages = null;
	/** configuration time stamp **/
	protected Date _lastUpdated = null;

	/**
	 * @return the site
	 */
	public String getSite() {
		return _site;
	}

	/**
	 * @param site
	 *            the site to set
	 */
	public void setSite(String site) {
		this._site = site;
	}

	/**
	 * @return the canned messages
	 */
	public Map<String, List<MessageTO>> getCannedMessages() {
		return _cannedMessages;
	}

	/**
	 * @param cannedMessages
	 *            the canned messages to set
	 */
	public void setCannedMessages(Map<String, List<MessageTO>> cannedMessages) {
		this._cannedMessages = cannedMessages;
	}

	/**
	 * @return the email message templates
	 */
	public Map<String, EmailMessageTemplateTO> getEmailMessageTemplates() {
		return _emailMessageTemplates;
	}

	/**
	 * @param emailMessageTemplates
	 *            the email message templates to set
	 */
	public void setEmailMessageTemplates(Map<String, EmailMessageTemplateTO> emailMessageTemplates) {
		this._emailMessageTemplates = emailMessageTemplates;
	}

	
	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return _lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this._lastUpdated = lastUpdated;
	}

	/**
	 * @param completeMessages the completeMessages to set
	 */
	public void setCompleteMessages(Map<String, String> completeMessages) {
		this._completeMessages = completeMessages;
	}

	/**
	 * @return the completeMessages
	 */
	public Map<String, String> getCompleteMessages() {
		return _completeMessages;
	}

	/**
	 * @param messages the messages to set
	 */
	public void setMessages(Map<String, String> messages) {
		this.messages = messages;
	}

	/**
	 * @return the messages
	 */
	public Map<String, String> getMessages() {
		return messages;
	}

    public void setErrorMessages(Map<String, String> errorMessages) {
        this.errorMessages=errorMessages;
        //To change body of created methods use File | Settings | File Templates.
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

	/**
	 * @return the sendNoticeMapping
	 */
	public Map<String, Boolean> getSendNoticeMapping() {
		if (this._sendNoticeMapping == null) {
			this._sendNoticeMapping = new FastMap<String, Boolean>(); 
		}
		return _sendNoticeMapping;
	}

	/**
	 * @param sendNoticeMapping the sendNoticeMapping to set
	 */
	public void setSendNoticeMapping(Map<String, Boolean> sendNoticeMapping) {
		this._sendNoticeMapping = sendNoticeMapping;
	}
}
