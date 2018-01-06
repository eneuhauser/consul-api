package com.ecwid.consul.v1;

import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.Service;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import com.pszymczyk.consul.ConsulPorts;
import com.pszymczyk.consul.infrastructure.Ports;
import com.pszymczyk.consul.LogLevel;
import org.hamcrest.collection.IsMapContaining;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class ConsulClientTest {

    private ConsulProcess consul;
    private int randomHttpsPort = Ports.nextAvailable();

    @Before
    public void setup() {
        consul = ConsulStarterBuilder.consulStarter()
                .withConsulVersion("1.0.0")
				.withLogLevel(LogLevel.INFO)
				.withConsulPorts(ConsulPorts.consulPorts()
					.withServerPort(randomHttpsPort)
					.build())
                .build()
                .start();
    }

    @After
    public void cleanup() throws Exception {
        consul.close();
    }

    @Test
    public void agentHttpTest() throws Exception {
        String host = "http://localhost";
        int port = consul.getHttpPort();
        ConsulClient consulClient = new ConsulClient(host, port);
        serviceRegisterTest(consulClient);
    }

    @Test
    public void agentHttpsTest() throws Exception {

        String host = "https://localhost";
        //TODO make https random port in consul
        int httpsPort = randomHttpsPort;

        String path = "src/test/resources/ssl";
        String certRootPath = new File(path).getAbsolutePath();
        String certificatePath = certRootPath + "/trustStore.jks";
        String certificatePassword = "change_me";
        String keyStorePath = certRootPath + "/keyStore.jks";
        String keyStorePassword = "change_me";

        TLSConfig tlsConfig = new TLSConfig(TLSConfig.KeyStoreInstanceType.JKS,
                certificatePath, certificatePassword,
                keyStorePath, keyStorePassword);
        ConsulClient consulClient = new ConsulClient(host, httpsPort, tlsConfig);
        serviceRegisterTest(consulClient);
    }

    private void serviceRegisterTest(ConsulClient consulClient) {
        NewService newService = new NewService();
        String serviceName = "abc";
        newService.setName(serviceName);
        consulClient.agentServiceRegister(newService);

        Response<Map<String, Service>> agentServicesResponse = consulClient.getAgentServices();
        Map<String, Service> services = agentServicesResponse.getValue();
        assertThat(services, IsMapContaining.hasKey(serviceName));
    }
}