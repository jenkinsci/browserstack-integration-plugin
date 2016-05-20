package com.browserstack.automate;

import com.browserstack.client.BrowserStackClient;
import com.browserstack.client.exception.BrowserStackException;
import com.browserstack.client.model.BrowserListing;

public class AutomateJenkinsClient extends BrowserStackClient {

    public AutomateJenkinsClient() {
    }

    @Override
    public BrowserListing getBrowsersForProduct(Product product) throws BrowserStackException {
        return super.getBrowsersForProduct(product);
    }
}
