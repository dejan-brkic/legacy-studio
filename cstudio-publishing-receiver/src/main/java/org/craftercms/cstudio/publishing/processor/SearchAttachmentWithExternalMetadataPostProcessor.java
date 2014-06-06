package org.craftercms.cstudio.publishing.processor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class SearchAttachmentWithExternalMetadataPostProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(SearchAttachmentWithExternalMetadataPostProcessor.class);

    private String siteName;
    private SearchService searchService;
    private List<String> excludeProperties;
    private List<String> attachmentPathPatterns;
    private List<String> metadataPathPatterns;
    private List<String> referenceXpathList;
    private String multivalueSeparator = ",";



    @Override
    public void doProcess(final PublishedChangeSet changeSet, final Map<String, String> parameters, final PublishingTarget target) throws PublishingException {
        String root = target.getParameter(FileUploadServlet.CONFIG_ROOT);
        String contentFolder = target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        String siteId = parameters.get(FileUploadServlet.PARAM_SITE);
        if (StringUtils.isEmpty(siteId)) {
            siteId = siteName;
        }

        root += "/" + contentFolder;
        if (org.springframework.util.StringUtils.hasText(siteId)) {
            root = root.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteId);
        }

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();

        try {
            if (CollectionUtils.isNotEmpty(createdFiles)) {
                processFiles(siteId, root, createdFiles, false);
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                processFiles(siteId, root, updatedFiles, false);
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                processFiles(siteId, root, deletedFiles, true);
            }
        } catch (Exception exc) {
            logger.error("Error: ", exc);
            throw new PublishingException("Failed to complete postprocessing.", exc);
        }
    }

    private void processFiles(String siteId, String root, List<String> fileList, boolean isDelete)
        throws IOException {
        for (String filePath : fileList) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Processing file %s for site %s", filePath, siteId));
            }
            File file = new File(root + filePath);
            String updateIndexPath = filePath;
            Map<String, String> externalProperties = null;
            boolean searchIndexUpdate = false;
            if (!isDelete) {
                if (isMetadataFile(filePath)){
                    if (logger.isDebugEnabled()) {
                        logger.debug("Metadata processing started.");
                    }
                    SAXReader reader = new SAXReader();
                    try {
                        Document document = reader.read(file);
                        updateIndexPath = getAttachmentPath(document);
                        if (StringUtils.isNotBlank(updateIndexPath)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Extracting properties.");
                            }
                            externalProperties = parseMetadataFile(document);
                            file = new File(root + updateIndexPath);
                            if (!file.exists()) {
                                File dir = file.getParentFile();
                                dir.mkdirs();
                                file.createNewFile();
                            }
                            searchIndexUpdate = true;
                        }
                    } catch (DocumentException e) {
                        logger.error(String.format("Error while opening xml file %s for site %s", filePath, siteId),
                            e);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Metadata processing finished.");
                    }
                } else if (isAttachmentFile(filePath)) {
                    searchIndexUpdate = true;
                }
            }
            if (searchIndexUpdate) {
                if(isDelete) {
                    searchService.delete(siteId, updateIndexPath);
                } else  {
                    if (logger.isDebugEnabled()){
                        logger.debug(String.format("Sending search update request for file %s [%s] for site %s",
                            updateIndexPath, filePath, siteId));
                    }
                    searchService.updateDocument(siteId, updateIndexPath, file, externalProperties);
                }
            }
            searchService.commit();
        }
    }

    private Map<String, String> parseMetadataFile(final Document document) {
        Map<String, String> properties = new HashMap<String, String>();
        Element root = document.getRootElement();
        processElementChildren(root, StringUtils.EMPTY, properties);
        return properties;
    }

    private void processElementChildren(final Element element, final String key, final Map<String,String> properties) {
        for (int i = 0, size = element.nodeCount(); i < size; i++) {
            Node node = element.node(i);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Processing xml element %s", node.getPath()));
            }
            if (node instanceof Element) {
                StringBuilder sbPrefix = new StringBuilder(key);
                if (sbPrefix.length() > 0) {
                    sbPrefix.append(".");
                }
                sbPrefix.append(node.getName());
                if (!excludeProperties.contains(sbPrefix.toString())) {
                    processElementChildren((Element)node, sbPrefix.toString(), properties);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                if (properties.containsKey(key)) {
                    sb.append(properties.get(key));
                    if (sb.length() > 0) {
                        sb.append(multivalueSeparator);
                    }
                }
                String value = node.getText();
                if (StringUtils.isNotBlank(value)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Adding value [%s] for property [%s].", value, key));
                    }
                    sb.append(value);
                    properties.put(key, StringUtils.trim(sb.toString()));
                }
            }
        }
    }

    private String getAttachmentPath(final Document document) {
        String referenceValue = StringUtils.EMPTY;
        for (String refXpath : referenceXpathList) {
            Node reference = document.selectSingleNode(refXpath);
            if (reference != null) {
                referenceValue = reference.getText();
            }
            if (StringUtils.isNotBlank(referenceValue)) {
                break;
            }
        }
        return referenceValue;
    }

    private boolean isMetadataFile(String filePath) {
        boolean metadataFile = false;
        for (String metadataPattern : metadataPathPatterns) {
            Pattern pattern = Pattern.compile(metadataPattern);
            Matcher matcher = pattern.matcher(filePath);
            if (matcher.matches()) {
                metadataFile = true;
                break;
            }
        }
        return metadataFile;
    }

    private boolean isAttachmentFile(String filePath) {
        boolean metadataFile = false;
        for (String attachmentPattern : attachmentPathPatterns) {
            Pattern pattern = Pattern.compile(attachmentPattern);
            Matcher matcher = pattern.matcher(filePath);
            if (matcher.matches()) {
                metadataFile = true;
                break;
            }
        }
        return metadataFile;
    }


    @Override
    public String getName() {
        return SearchAttachmentWithExternalMetadataPostProcessor.class.getSimpleName();
    }


    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(final String siteName) {
        this.siteName = siteName;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(final SearchService searchService) {
        this.searchService = searchService;
    }

    public List<String> getExcludeProperties() {
        return excludeProperties;
    }

    public void setExcludeProperties(final List<String> excludeProperties) {
        this.excludeProperties = excludeProperties;
    }

    public List<String> getAttachmentPathPatterns() {
        return attachmentPathPatterns;
    }

    public void setAttachmentPathPatterns(final List<String> attachmentPathPatterns) {
        this.attachmentPathPatterns = attachmentPathPatterns;
    }

    public List<String> getMetadataPathPatterns() {
        return metadataPathPatterns;
    }

    public void setMetadataPathPatterns(final List<String> metadataPathPatterns) {
        this.metadataPathPatterns = metadataPathPatterns;
    }

    public List<String> getReferenceXpathList() {
        return referenceXpathList;
    }

    public void setReferenceXpathList(final List<String> referenceXpathList) {
        this.referenceXpathList = referenceXpathList;
    }

    public String getMultivalueSeparator() {
        return multivalueSeparator;
    }

    public void setMultivalueSeparator(final String multivalueSeparator) {
        this.multivalueSeparator = multivalueSeparator;
    }
}
