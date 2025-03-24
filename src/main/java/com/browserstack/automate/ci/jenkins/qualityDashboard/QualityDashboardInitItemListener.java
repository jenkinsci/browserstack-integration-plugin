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
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import java.io.IOException;
import java.io.Serializable;

@Extension
public class QualityDashboardInitItemListener extends ItemListener {

    @Override
    public void onCreated(Item job) {
        String itemName = job.getFullName();
        String itemType = getItemTypeModified(job);
        if(itemType != null && itemType.equals("PIPELINE")) {
            try {
                String jsonBody = getJsonReqBody(new ItemUpdate(itemName, itemType));
                syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "POST");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDeleted(Item job) {
        String itemName = job.getFullName();
        String itemType = getItemTypeModified(job);
        if(itemType != null) {
            try {
                String jsonBody = getJsonReqBody(new ItemUpdate(itemName, itemType));
                syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "DELETE");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRenamed(Item job, String oldName, String newName) {
        String itemType = getItemTypeModified(job);
        if(itemType != null) {
            try {
                oldName = job.getParent().getFullName() + "/" + oldName;
                newName = job.getParent().getFullName() + "/" + newName;
                String jsonBody = getJsonReqBody(new ItemRename(oldName, newName, itemType));
                syncItemListToQD(jsonBody, Constants.QualityDashboardAPI.getItemCrudEndpoint(), "PUT");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getItemTypeModified(Item job) {
        String itemType = null;
        boolean isFolderRenamed = job.getClass().getName().contains("Folder");
        boolean isPipelineRenamed = job instanceof WorkflowJob;
        if(isFolderRenamed || isPipelineRenamed) {
            itemType = isPipelineRenamed ? "PIPELINE" : "FOLDER";
        }
        return itemType;
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
