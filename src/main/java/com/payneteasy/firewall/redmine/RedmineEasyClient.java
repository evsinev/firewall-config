package com.payneteasy.firewall.redmine;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.xml.XmlHttpContent;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
        return pageName.replaceAll("\\.", "_") + ".xml";
    }

    private final HttpRequestFactory requestFactory;
    private final String url;

    public RedmineEasyClient(String url, String apiKey) throws NoSuchAlgorithmException, KeyManagementException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkArgument(url.length() > 0, "illegal url value <%s>", url);
        Preconditions.checkNotNull(apiKey, "apiKey");
        Preconditions.checkArgument(apiKey.length() > 0, "illegal apiKey value <%s>", apiKey);

        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(null, new TrustManager[] {new TrustAllTrustManager()}, null);
        HttpTransport transport = new NetHttpTransport.Builder().setSslSocketFactory(sslContext.getSocketFactory()).build();
        this.requestFactory = transport.createRequestFactory(new RedmineHttpRequestInitializer(apiKey));
        this.url = url;
    }

    public final void executeCreateOrUpdateWikiPage(String pageName, String title, String text, String comment) throws IOException {
        execute(pageName, title, text, comment, null);
    }

    private void executeDeletePage(String pageName) throws IOException {
        pageName = normalizePageName(pageName);

        GenericUrl genericUrl = new GenericUrl(url);
        genericUrl.getPathParts().add(pageName);
        HttpRequest httpRequest = requestFactory.buildDeleteRequest(genericUrl);
        HttpResponse response = httpRequest.execute();
        if (response.getStatusCode() != 200) {
            throw new IOException("Cannot delete page <" + pageName + ">. "
                    + "Response [statusCode: " + response.getStatusCode() + "; statusMessage: " + response.getStatusMessage());
        }
    }

    private void executeCreateOrUpdateWikiPage(String pageName, String title, String text, String comment, Integer version) throws IOException {
        execute(pageName, title, text, comment, version);
    }

    private boolean executeIsPageExists(String pageName) throws IOException {
        pageName = normalizePageName(pageName);
        
        GenericUrl genericUrl = new GenericUrl(url);
        genericUrl.getPathParts().add(pageName);
        HttpRequest httpRequest = requestFactory.buildGetRequest(genericUrl);
        httpRequest.setThrowExceptionOnExecuteError(false);
        return httpRequest.execute().isSuccessStatusCode();
    }
    
    private void execute(String pageName, String title, String text, String comment, Integer version) throws IOException {
        pageName = normalizePageName(pageName);

        long startTime = System.currentTimeMillis();
        System.out.print(pageName + " ... ");
        
        WikiPageXml wikiPageXml = new WikiPageXml();
        wikiPageXml.setComments(comment);
        wikiPageXml.setText(text);
        wikiPageXml.setTitle(title);
        wikiPageXml.setVersion(version);

        XmlNamespaceDictionary xmlNamespaceDictionary = new XmlNamespaceDictionary();
        xmlNamespaceDictionary.set("", "");
        XmlHttpContent httpContent = new XmlHttpContent(xmlNamespaceDictionary, "wiki_page", wikiPageXml);
        httpContent.setMediaType(new HttpMediaType("application", "xml"));

        GenericUrl genericUrl = new GenericUrl(url);
        genericUrl.getPathParts().add(pageName);
        HttpRequest httpRequest = requestFactory.buildPutRequest(genericUrl, httpContent);

        HttpResponse response = httpRequest.execute();
        switch (response.getStatusCode()) {
            case 200:
            case 201:
                System.out.println("OK in "+(System.currentTimeMillis() - startTime)+" ms");
                break;

            case 409:
                throw new IOException("Conflict: occurs when trying to update a stale page. "
                        + "Response [statusCode: " + response.getStatusCode() + "; statusMessage: " + response.getStatusMessage());
            case 422:
                throw new IOException("Unprocessable Entity: page was not saved due to validation failures "
                        + "(response body contains the error messages)"
                        + "Response [statusCode: " + response.getStatusCode() + "; statusMessage: " + response.getStatusMessage());

            default:
                // do nothing
                throw new IllegalStateException("Unknown error: "+response.getStatusCode()+" : "+response.getStatusMessage());
        }
    }
}
