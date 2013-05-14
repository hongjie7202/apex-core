/*
 *  Copyright (c) 2012-2013 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pramod Immaneni <pramod@malhar-inc.com>
 */
public class WebServicesClient
{

  private static final Logger LOG = LoggerFactory.getLogger(WebServicesClient.class);

  private static final CredentialsProvider credentialsProvider;

  private Client client;

  static {
    credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new Credentials() {

      @Override
      public Principal getUserPrincipal()
      {
        return null;
      }

      @Override
      public String getPassword()
      {
        return null;
      }

    });
  }

  public WebServicesClient() {
    if (UserGroupInformation.isSecurityEnabled()) {
      DefaultHttpClient httpClient = new DefaultHttpClient();
      httpClient.setCredentialsProvider(credentialsProvider);
      httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new SPNegoSchemeFactory(true));
      ApacheHttpClient4Handler httpClientHandler = new ApacheHttpClient4Handler(httpClient, new BasicCookieStore(), false);
      client = new Client(httpClientHandler);
    } else {
      client = Client.create();
    }
  }

  public WebServicesClient(Client client) {
    this.client = client;
  }

  public Client getClient() {
    return client;
  }

  public <T> T process(String url, Class<T> clazz, WebServicesHandler<T> handler) throws IOException {
    final WebResource wr = client.resource(url);
    final WebServicesHandler<T> webHandler = handler;
    final Class<T> webClazz = clazz;
    if (UserGroupInformation.isSecurityEnabled()) {
      UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
      return loginUser.doAs(new PrivilegedAction<T>() {

        @Override
        public T run()
        {
          return webHandler.process(wr, webClazz);
        }

      });
    } else {
      return webHandler.process(wr, webClazz);
    }
  }

  public static abstract class WebServicesHandler<T> {
    public abstract T process(WebResource webResource, Class<T> clazz);
  }

  public static class GetWebServicesHandler<T> extends WebServicesHandler<T> {

    @Override
    public T process(WebResource webResource, Class<T> clazz)
    {
      return webResource.get(clazz);
    }

  }

}
