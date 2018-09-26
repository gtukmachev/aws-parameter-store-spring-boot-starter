package com.coveo.configuration.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStoreSourceTest
{
    private static final String VALID_PROPERTY_NAME = "awesomeproperty";
    private static final String VALID_PROPERTY_VALUE = "awesomepropertyVALUE";

    private static final String INVALID_PROPERTY_NAME = "notawesomeproperty";

    @Mock
    private AWSSimpleSystemsManagement ssmClientMock;

    private ParameterStoreSource parameterStoreSource;

    @Before
    public void setUp()
    {
        parameterStoreSource = new ParameterStoreSource(ssmClientMock);
    }

    @Test
    public void testGetProperty()
    {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(new GetParameterResult().withParameter(new Parameter().withValue(VALID_PROPERTY_VALUE)));

        Object value = parameterStoreSource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value, is(VALID_PROPERTY_VALUE));
    }

    @Test
    public void testGetPropertyWhenNotFoundReturnsNull()
    {
        when(ssmClientMock.getParameter(getParameterRequest(INVALID_PROPERTY_NAME))).thenThrow(new ParameterNotFoundException(""));

        Object value = parameterStoreSource.getProperty(INVALID_PROPERTY_NAME);

        assertThat(value, is(nullValue()));
    }

    @Test(expected = ParameterStoreRuntimeException.class)
    public void shouldThrowOnUnexpectedExceptionAccessingParameterStore()
    {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenThrow(new RuntimeException());

        parameterStoreSource.getProperty(VALID_PROPERTY_NAME);
    }

    private GetParameterRequest getParameterRequest(String parameterName)
    {
        return new GetParameterRequest().withName(parameterName).withWithDecryption(true);
    }
}
