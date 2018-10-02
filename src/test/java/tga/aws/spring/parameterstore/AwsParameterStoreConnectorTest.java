package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.util.List;

import static com.amazonaws.SDKGlobalConfiguration.*;
import static org.mockito.Matchers.any;
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
    @Mock private GetParametersByPathResult getParametersByPathResultMock;
    @Mock private List<Parameter> parametersMock;

    private AwsParameterStoreConnector awsParameterStoreConnector = new AwsParameterStoreConnector();

    @Before
    public void setUp() {
        AwsParameterStoreConnector.initialized = false;
        awsParameterStoreConnector.setAwsParameterStoreClientBuilder( clientBuilderMock );

        when(clientBuilderMock.getClient()).thenReturn(awsClientMock);
        when(awsClientMock.getParametersByPath(any())).thenReturn(getParametersByPathResultMock);
        when(getParametersByPathResultMock.getNextToken()).thenReturn(null);
        when(getParametersByPathResultMock.getParameters()).thenReturn(parametersMock);
        when(parametersMock.isEmpty()).thenReturn(false);

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

    @Test
    public void testParameterStoreIsEnabledWithProfileProd() {
        activateSpringProfiles("Prod");
        setupRootFolders("/my-app,/common");
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
        setupRootFolders("");
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(envMock, never()).acceptsProfiles("");
        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsDisabledOnUnaceptedProfile() {
        activateSpringProfiles("Dev");
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCantBeCalledTwice() {
        activateSpringProfiles("ANY");
        setupRootFolders("");

        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);
        awsParameterStoreConnector.postProcessEnvironment(envMock, applicationMock);

        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }
}
