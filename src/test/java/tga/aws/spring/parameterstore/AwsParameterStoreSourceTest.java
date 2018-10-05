package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import tga.aws.spring.parameterstore.exception.AwsParameterStoreConnectorException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStoreSourceTest {
    private static final String VALID_PROPERTY_NAME = "awesomeproperty";
    private static final String VALID_PROPERTY_VALUE = "awesomepropertyVALUE";

    private static final String INVALID_PROPERTY_NAME = "notawesomeproperty";

    @Mock
    private AWSSimpleSystemsManagement ssmClientMock;

    private AwsParameterStoreSource parameterStoreSource;

    @Before
    public void setUp() {
        parameterStoreSource = new AwsParameterStoreSource(ssmClientMock);
    }

    @Test
    public void testGetProperty() {
        when(ssmClientMock.getParameter(request(VALID_PROPERTY_NAME)))
                .thenReturn(new GetParameterResult()
                        .withParameter(new Parameter().withValue(VALID_PROPERTY_VALUE)));

        Object value = parameterStoreSource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value, is(VALID_PROPERTY_VALUE));
    }

    @Test
    public void testGetPropertyWhenNotFoundReturnsNull() {
        when(ssmClientMock.getParameter(request(INVALID_PROPERTY_NAME)))
                .thenThrow(new ParameterNotFoundException(""));

        Object value = parameterStoreSource.getProperty(INVALID_PROPERTY_NAME);

        assertThat(value, is(nullValue()));
    }

    @Test(expected = AwsParameterStoreConnectorException.class)
    public void shouldThrowOnUnexpectedExceptionAccessingParameterStore() {
        when(ssmClientMock.getParameter(request(VALID_PROPERTY_NAME)))
                .thenThrow(new RuntimeException());

        parameterStoreSource.getProperty(VALID_PROPERTY_NAME);
    }

    private GetParameterRequest request(String parameterName) {
        return new GetParameterRequest().withName(parameterName).withWithDecryption(true);
    }
}
