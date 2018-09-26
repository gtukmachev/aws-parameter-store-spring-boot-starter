package tga.aws.spring.parameterstore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStorePropertySourceTest
{
    private static final String VALID_PROPERTY_NAME = "valid.property";
    private static final String FOLDER = "/common";
    private static final String VALID_VALUE = "myvalidvalue";

    @Mock
    private ParameterStoreSource parameterStoreSourceMock;

    private ParameterStorePropertySource parameterStorePropertySource;

    @Before
    public void setUp()
    {
        parameterStorePropertySource = new ParameterStorePropertySource("someuselessname", parameterStoreSourceMock, new String[]{FOLDER});
        when(parameterStoreSourceMock
                .getProperty(FOLDER + "/" + VALID_PROPERTY_NAME.replace(".", "/")))
                .thenReturn(VALID_VALUE);
    }

    @Test
    public void testGetProperty()
    {
        Object value = parameterStorePropertySource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value, is(VALID_VALUE));
        verify(parameterStoreSourceMock).getProperty(FOLDER + "/" + VALID_PROPERTY_NAME.replace(".", "/"));
    }
}
