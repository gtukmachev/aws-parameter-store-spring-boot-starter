package tga.aws.spring.parameterstore;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static com.amazonaws.SDKGlobalConfiguration.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static tga.aws.spring.parameterstore.AwsParameterStoreConnector.pName_AcceptedSpringProfiles;
import static tga.aws.spring.parameterstore.AwsParameterStoreConnector.pName_Roots;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AwsParameterStoreConnectorTest
{
    private static final String CUSTOM_PROFILES = "open,source,this";

    @Mock private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock private MutablePropertySources mutablePropertySourcesMock;
    @Mock private SpringApplication applicationMock;

    private AwsParameterStoreConnector awsParameterStoreConnector = new AwsParameterStoreConnector();

    @Before
    public void setUp()
    {
        AwsParameterStoreConnector.initialized = false;

        when(configurableEnvironmentMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn("ANY");
        when(configurableEnvironmentMock.getProperty(pName_Roots, String.class, "")).thenReturn("common");
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
    }

    @Test
    public void testParameterStoreIsEnabledWithProfile()
    {
        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        verify(mutablePropertySourcesMock).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsDisabledByDefault()
    {
        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles() {
        when(configurableEnvironmentMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES.split(","))).thenReturn(true);

        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        verify(mutablePropertySourcesMock).addFirst(any(AwsParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty() {
        when(configurableEnvironmentMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn("");
        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        verify(configurableEnvironmentMock, never()).acceptsProfiles("");
        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        when(configurableEnvironmentMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES.split(","))).thenReturn(false);

        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCantBeCalledTwice()
    {
        when(configurableEnvironmentMock.getProperty(pName_AcceptedSpringProfiles, String.class, "")).thenReturn("ANY");

        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        awsParameterStoreConnector.postProcessEnvironment(configurableEnvironmentMock, applicationMock);

        verify(mutablePropertySourcesMock, times(1)).addFirst(any(AwsParameterStorePropertySource.class));
    }
}
