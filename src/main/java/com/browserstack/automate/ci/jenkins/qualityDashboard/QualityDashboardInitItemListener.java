package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.Serializable;
import java.util.logging.Logger;

@Extension
public class QualityDashboardInitItemListener extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(QualityDashboardInitItemListener.class.getName());

    @Override
    public void onCreated(Item job) {
        try {
            BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
            if(browserStackCredentials == null) {
                LOGGER.info("BrowserStackCredentials not found. Please ensure they are configured correctly.");
                return;
            }
            QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();

            String itemName = job.getFullName();
            String itemType = QualityDashboardUtil.getItemTypeModified(job);

            apiUtil.logToQD(browserStackCredentials, "Item Created : " + itemName + " - " + "Item Type : " + itemType);
            if(itemType != null && !itemType.equals("FOLDER")) {
                try {
                    String jsonBody = getJsonReqBody(new ItemUpdate(itemName, itemType));
                    syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "POST");

                } catch(Exception e) {
                    LOGGER.info("Error syncing item creation to Quality Dashboard: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                apiUtil.logToQD(browserStackCredentials, "Skipping item creation sync: " + itemName);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeleted(Item job) {
        String itemName = job.getFullName();
        String itemType = QualityDashboardUtil.getItemTypeModified(job);
        if(itemType != null) {
            try {
                String jsonBody = getJsonReqBody(new ItemUpdate(itemName, itemType));
                syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "DELETE");
            } catch(Exception e) {
                LOGGER.info("Error syncing item deletion to Quality Dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRenamed(Item job, String oldName, String newName) {
        String itemType = QualityDashboardUtil.getItemTypeModified(job);
        if(itemType != null) {
            try {
                oldName = job.getParent().getFullName() + "/" + oldName;
                newName = job.getParent().getFullName() + "/" + newName;
                String jsonBody = getJsonReqBody(new ItemRename(oldName, newName, itemType));
                syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "PUT");
            } catch(Exception e) {
            LOGGER.info("Error syncing item rename to Quality Dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private <T> String getJsonReqBody( T item) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(item);
        return jsonBody;
    }

    private Response syncItemListToQD(String jsonBody, String url, String typeOfRequest) throws JsonProcessingException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
        QualityDashboardAPIUtil apiUtil = new QualityDashboardAPIUtil();
        BrowserStackCredentials browserStackCredentials = QualityDashboardUtil.getBrowserStackCreds();
        if(browserStackCredentials == null) {
            LOGGER.info("BrowserStack credentials not found. Please ensure they are configured correctly.");
            return null;
        }  
        if(typeOfRequest.equals("PUT")) {
            apiUtil.logToQD(browserStackCredentials, "Syncing Item Update - PUT");
            return apiUtil.makePutRequestToQd(url, browserStackCredentials, requestBody);
        } else if(typeOfRequest.equals("DELETE")) {
            apiUtil.logToQD(browserStackCredentials, "Syncing Item Deleted - DELETE");
            return apiUtil.makeDeleteRequestToQd(url, browserStackCredentials, requestBody);
        } else {
            apiUtil.logToQD(browserStackCredentials, "Syncing Item Added - POST");
            return apiUtil.makePostRequestToQd(url, browserStackCredentials, requestBody);
        }
    }
}

class ItemUpdate implements Serializable {
    @JsonProperty("item")
    private String itemName;

    @JsonProperty("itemType")
    private String itemType;

    public ItemUpdate(String itemName, String itemType) {
        this.itemName = itemName;
        this.itemType = itemType;
    }
}

class ItemRename implements Serializable {
    @JsonProperty("fromName")
    private String fromItemName;

    @JsonProperty("toName")
    private String toItemName;

    @JsonProperty("itemType")
    private String itemType;

    public ItemRename(String fromItemName, String toItemName, String itemType) {
        this.fromItemName = fromItemName;
        this.toItemName = toItemName;
        this.itemType = itemType;
    }
}
