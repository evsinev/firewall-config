package com.payneteasy.firewall.redmine;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

import java.io.IOException;

final class RedmineHttpRequestInitializer implements HttpRequestInitializer, HttpExecuteInterceptor {

    private static final String HEADER_API_KEY = "X-Redmine-API-Key";

    private final String apiKey;

    public RedmineHttpRequestInitializer(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override public void initialize(HttpRequest request) throws IOException {
        request.setInterceptor(this);
    }

    @Override public void intercept(HttpRequest request) throws IOException {
        request.getHeaders().put(HEADER_API_KEY, apiKey);
    }
}
