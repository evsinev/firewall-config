package com.payneteasy.firewall.redmine.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.payneteasy.firewall.redmine.IRedmineIssueClient;
import com.payneteasy.firewall.redmine.messages.RedmineIssueCreateRequest;
import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.impl.HttpClientImpl;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RedmineIssueClientImpl implements IRedmineIssueClient {

    private final String      baseUrl;
    private final String      apiKey;
    private final Gson        gson;
    private final IHttpClient httpClient;
    private final HttpHeaders headers;

    private HttpRequestParameters params = HttpRequestParameters.builder()
            .timeouts(new HttpTimeouts(10_000, 30_000))
            .build();

    public RedmineIssueClientImpl(String baseUrl, String apiKey, Gson gson, IHttpClient httpClient) {
        this.baseUrl    = baseUrl;
        this.apiKey     = apiKey;
        this.gson       = gson;
        this.httpClient = httpClient;

        headers = new HttpHeaders(
                Arrays.asList(
                        new HttpHeader("X-Redmine-API-Key", apiKey)
                        , new HttpHeader("Content-Type", "application/json")
                )
        );
    }

    @Override
    public void createIssue(RedmineIssueCreateRequest aRequest) {
        String url = baseUrl + "/issues.json";
        HttpRequest request = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.POST)
                .headers(headers)
                .body(gson.toJson(aRequest).getBytes(UTF_8))
                .build();

        HttpResponse response = sendHttp(request, url);

        String text = new String(response.getBody(), UTF_8);

        if (response.getStatusCode() != 201) {
            throw new IllegalStateException("bad response code " + response.getStatusCode() + " " + text);
        }
    }

    @Override
    public void getIssuesByParentId(String aProjectId, int aParentId) {
        String url = baseUrl + "/issues.json?project_id=" + aProjectId + "&parent_id" + aParentId;
        HttpRequest request = HttpRequest.builder()
                .url(url)
                .method(HttpMethod.GET)
                .headers(headers)
                .build();

        HttpResponse response = sendHttp(request, url);

        String text = new String(response.getBody(), UTF_8);

        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("bad response code " + response.getStatusCode() + " " + text);
        }
    }

    private HttpResponse sendHttp(HttpRequest request, String url) {
        try {
            return httpClient.send(request, params);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot send request to " + url, e);
        }
    }

}
