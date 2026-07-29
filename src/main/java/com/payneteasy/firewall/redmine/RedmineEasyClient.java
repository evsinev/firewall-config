package com.payneteasy.firewall.redmine;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.payneteasy.firewall.redmine.messages.RedmineWikiPageUpdateRequest;
import com.payneteasy.firewall.redmine.model.RedmineWikiPage;
import com.payneteasy.http.client.api.HttpHeader;
import com.payneteasy.http.client.api.HttpHeaders;
import com.payneteasy.http.client.api.HttpMethod;
import com.payneteasy.http.client.api.HttpRequest;
import com.payneteasy.http.client.api.HttpRequestParameters;
import com.payneteasy.http.client.api.HttpResponse;
import com.payneteasy.http.client.api.HttpTimeouts;
import com.payneteasy.http.client.api.IHttpClient;
import com.payneteasy.http.client.impl.HttpClientImpl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Pushes wiki pages into Redmine over its REST API.
 *
 * The page is sent as JSON (<code>PUT .../&lt;page&gt;.json</code>), the same format
 * <code>redmine/impl/RedmineIssueClientImpl</code> uses for issues.
 */
public class RedmineEasyClient implements IRedmineClient {

    final class TrustAllTrustManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            // do nothing
        }

        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            // do nothing
        }

        @Override public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private static String normalizePageName(String pageName) {
        Preconditions.checkNotNull(pageName, "page name");
        return pageName.replaceAll("\\.", "_") + ".json";
    }

    private final IHttpClient           httpClient = new HttpClientImpl();
    private final Gson                  gson       = new Gson();
    private final HttpHeaders           headers;
    private final HttpRequestParameters params;
    private final String                url;

    public RedmineEasyClient(String url, String apiKey) throws NoSuchAlgorithmException, KeyManagementException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkArgument(url.length() > 0, "illegal url value <%s>", url);
        Preconditions.checkNotNull(apiKey, "apiKey");
        Preconditions.checkArgument(apiKey.length() > 0, "illegal apiKey value <%s>", apiKey);

        HttpRequestParameters.HttpRequestParametersBuilder parameters = HttpRequestParameters.builder()
                .timeouts(new HttpTimeouts(10_000, 30_000));

        // Only for https: HttpClientImpl throws if a socket factory or a hostname verifier
        // is set on a plain http connection, and MainWiki accepts any url starting with
        // "http". When we do speak TLS the certificate is deliberately not verified - see
        // the Security section of the docs.
        if (url.toLowerCase().startsWith("https")) {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[] {new TrustAllTrustManager()}, null);
            parameters.sslSocketFactory(sslContext.getSocketFactory())
                      .hostnameVerifier((hostname, session) -> true);
        }

        this.params = parameters.build();

        this.headers = new HttpHeaders(
                Arrays.asList(
                        new HttpHeader("X-Redmine-API-Key", apiKey)
                        , new HttpHeader("Content-Type", "application/json")
                )
        );

        this.url = url;
    }

    /** The title is ignored: a Redmine wiki page is titled by its name. */
    @Override public final void executeCreateOrUpdateWikiPage(String pageName, String title, String text, String comment) throws IOException {
        execute(pageName, text, comment, null);
    }

    public void execute(String pageName, String text, String comment, Integer version) throws IOException {
        String normalizedPageName = normalizePageName(pageName);
        String pageUrl            = url.endsWith("/") ? url + normalizedPageName : url + "/" + normalizedPageName;

        long startTime = System.currentTimeMillis();
        System.out.print(normalizedPageName + " ... ");

        RedmineWikiPageUpdateRequest wikiPage = RedmineWikiPageUpdateRequest.builder()
                .wikiPage(RedmineWikiPage.builder()
                        .text(text)
                        .comments(comment)
                        .version(version)
                        .build())
                .build();

        HttpRequest request = HttpRequest.builder()
                .url(pageUrl)
                .method(HttpMethod.PUT)
                .headers(headers)
                .body(gson.toJson(wikiPage).getBytes(UTF_8))
                .build();

        HttpResponse response = sendHttp(request, pageUrl);

        switch (response.getStatusCode()) {
            case 200:
            case 201:
            case 204:
                System.out.println("OK in "+(System.currentTimeMillis() - startTime)+" ms");
                break;

            case 409:
                throw new IOException("Conflict: occurs when trying to update a stale page."
                        + describe(pageUrl, response));

            case 422:
                throw new IOException("Unprocessable Entity: page was not saved due to validation failures."
                        + describe(pageUrl, response));

            default:
                throw new IllegalStateException("Unknown error." + describe(pageUrl, response));
        }
    }

    private HttpResponse sendHttp(HttpRequest aRequest, String aUrl) throws IOException {
        try {
            return httpClient.send(aRequest, params);
        } catch (Exception e) {
            throw new IOException("Cannot send request to " + aUrl, e);
        }
    }

    private static String describe(String aUrl, HttpResponse aResponse) {
        return "\n\n    Url           : " + aUrl
                + "\n\n    Status        : " + aResponse.getStatusCode() + " " + aResponse.getReasonPhrase()
                + "\n\n    Response body : " + new String(aResponse.getBody(), UTF_8);
    }
}
