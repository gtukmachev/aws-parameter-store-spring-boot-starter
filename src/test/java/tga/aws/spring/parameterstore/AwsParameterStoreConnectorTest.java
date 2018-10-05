package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.SDKGlobalConfiguration.*;
import static org.mockito.Mockito.*;
import static tga.aws.spring.parameterstore.AwsParameterStoreConnector.pName_AcceptedSpringProfiles;
import static tga.aws.spring.parameterstore.AwsParameterStoreConnector.pName_Roots;

@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStoreConnectorTest
{
    @Mock private ConfigurableEnvironment envMock;
    @Mock private MutablePropertySources mutablePropertySourcesMock;
    @Mock private SpringApplication applicationMock;
    @Mock private AWSParameterStoreClientBuilder clientBuilderMock;
    @Mock private AWSSimpleSystemsManagement awsClientMock;

    private AwsParameterStoreConnector awsParameterStoreConnector = new AwsParameterStoreConnector();

    @Before
    public void setUp() {
        AwsParameterStoreConnector.initialized = false;
        awsParameterStoreConnector.setAwsParameterStoreClientBuilder( clientBuilderMock );
        when(clientBuilderMock.getClient()).thenReturn(awsClientMock);

        when(envMock.acceptsProfiles(any(String.class))).thenAnswer( invocation -> {
            String profile = (String)(invocation.getArguments()[0]);
            return "Prod".equals(profile);
        });

        when(envMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
    }

    private void activateSpringProfiles(String profiles) {
        when(envMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn(profiles);
    }

    private void setupRootFolders(String rootFolders) {
        when(envMock.getProperty(pName_Roots, String.class, "")).thenReturn(rootFolders);
    }

    private GetParametersByPathResult setupParameterStoreResponse(String folder, String... p){

        GetParametersByPathResult checkMock = mock(GetParametersByPathResult.class);
        when(awsClientMock.getParametersByPath(
                new GetParametersByPathRequest()
                        .withMaxResults(1)
                        .withPath(folder)
                        .withWithDecryption(true)
                        .withRecursive(true)
        )).thenReturn(checkMock);

        GetParametersByPathResult respMock = mock(GetParametersByPathResult.class);
        when(awsClientMock.getParametersByPath(
                new GetParametersByPathRequest()
                        .withPath(folder)
                        .withWithDecryption(true)
                        .withRecursive(true)
                        .withNextToken(null)
        )).thenReturn(respMock);

        when(respMock.getNextToken()).thenReturn(null);
        List<Parameter> parameters = new ArrayList<>();
        for (int i = 0; i < p.length; i += 2 )
            parameters.add(new Parameter()
                    .withName(p[i])
                    .withType("String")
                    .withValue(p[i+1]));
        when(respMock.getParameters()).thenReturn(parameters);

        return respMock;
    }

    private void setupStandardResponses() {
        setupRootFolders("/my-app,/common");
        setupParameterStoreResponse("/my-app",
                "/my-app/prop/val/x", "valid value x",
                "/my-app/prop/val/y", "valid value y"
        );
        setupParameterStoreResponse("/common",
                "/common/prop/val/y", "invalid value y",
                "/common/prop/val/z", "valid value z"
        );
    }

    @Test
    public void testParameterStoreIsEnabledWithProfileProd() {
        activateSpringProfiles("Prod");
        setupStandardResponses();

        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsDisabledByDefault() {
        activateSpringProfiles("");
        setupRootFolders("");
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(envMock, never()).acceptsProfiles("");
        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithProfileANY() {
        activateSpringProfiles("ANY");
        setupStandardResponses();
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(envMock, never()).acceptsProfiles("");
        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsDisabledOnUnacceptedProfile() {
        activateSpringProfiles("Dev");
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCantBeCalledTwice() {
        activateSpringProfiles("ANY");
        setupStandardResponses();

        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testPropertiesShouldBeDefinedInAProperWay(){
        activateSpringProfiles("ANY");
        setupRootFolders("/my-app,/common");
        setupStandardResponses();

        ArgumentCaptor<AwsParameterStorePropertySource> argument = ArgumentCaptor.forClass(AwsParameterStorePropertySource.class);

        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        Map<String, Parameter> params = new HashMap<>();
        params.put("prop.val.x", new Parameter().withValue("valid value x").withName("/my-app/prop/val/x").withType("String"));
        params.put("prop.val.y", new Parameter().withValue("valid value y").withName("/my-app/prop/val/y").withType("String"));
        params.put("prop.val.z", new Parameter().withValue("valid value z").withName("/common/prop/val/z").withType("String"));

        verify(mutablePropertySourcesMock).addFirst( new AwsParameterStorePropertySource( "AwsParameterStorePropertySource", params ) );
    }
}
