package tga.aws.spring.parameterstore.integration;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.DeleteParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import tga.aws.spring.parameterstore.AwsParameterStoreConnector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by grigory@clearscale.net on 9/28/2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadPropertiesTests {

    @Mock SpringApplication springApplication;
    @Mock ConfigurableEnvironment environment;

    AwsParameterStoreConnector connector;
    MutablePropertySources mutablePropertySources;
    PropertySource<?> propertySource;

    @Before
    public void prepareEnvironment(){
        // Spring Application Mock
        this.mutablePropertySources = new MutablePropertySources();
        when( environment.getPropertySources() )
                .thenReturn(mutablePropertySources);

        // we going to read properties from '/testqwe' and then from '/commonqwe' root folders
        when( environment.getProperty(AwsParameterStoreConnector.pName_Roots, String.class, "") )
                .thenReturn("/testqwe,/commonqwe");

        // we activates this property source for all possible Spring profiles
        when( environment.getProperty(AwsParameterStoreConnector.pName_AcceptedSpringProfiles, String.class, "") )
                .thenReturn("ANY");

        //Real AWS credentials
        //use -Daws.accessKeyId=... -Daws.secretKey=...
        //or any other way: https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html


        //ow we are ready to make a AwsParameterStoreConnector
        AwsParameterStoreConnector connector = new AwsParameterStoreConnector();
        connector.postProcessEnvironment(environment, springApplication);

        // now, this.mutablePropertySources contains our PropertySource
        propertySource = this.mutablePropertySources.get("AwsParameterStorePropertySource");
    }

    @Test public void readParameterFromFirstRootFolder() {
        AWSSimpleSystemsManagement store = AWSSimpleSystemsManagementClientBuilder.defaultClient();

        withParameter(store, "/commonqwe/server/port", "8090", () -> {
            String value = (String) propertySource.getProperty("server.port");
            assertThat(value, is("8090"));
        });

        withParameter(store, "/testqwe/server/port", "8080", () -> {
            String value = (String) propertySource.getProperty("server.port");
            assertThat(value, is("8080"));
        });

    }


    private void withParameter(AWSSimpleSystemsManagement store, String name, String value, CodeBlock codeBlock){
        store.putParameter( new PutParameterRequest()
                .withName(name)
                .withType(ParameterType.String)
                .withValue(value)
        );

        try {
            codeBlock.apply();
        } finally {
            store.deleteParameter( new DeleteParameterRequest().withName(name));
        }
    }


    @FunctionalInterface
    interface CodeBlock {
        void apply();
    }

}
