package tga.aws.spring.parameterstore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStorePropertySourceTest
{
    private final String pName = "valid.property";
    private static final String VALID_VALUE = "myvalidvalue";

    @Mock
    private AwsParameterStoreSource pssMock;

    private AwsParameterStorePropertySource psps;

    private void setUp(String rootFoldersStr) {
        psps = new AwsParameterStorePropertySource("aName", pssMock, rootFoldersStr.split(","));
    }

    private void withVal(String name, String value) {
        when(pssMock.getProperty(name)).thenReturn(value);
    }

    @Test public void getExistedPropertyShouldReturnValue() {
        setUp("/common");
        withVal("/common/test/prop/val", "Ok");

        assertThat( psps.getProperty("test.prop.val"), is("Ok") );
    }

    @Test public void getUnExistedPropertyShouldReturnNull() {
        setUp("/common");
        withVal("/common/test/prop/val", "Ok");

        assertThat( psps.getProperty("test.prop.val.another"), is(nullValue()) );
    }

    @Test(expected = RuntimeException.class)
    public void anyExceptionOfAwsClientShuoldRiseRuntimeException() {
        setUp("/common");
        withVal("/common/test/prop/val", "Ok");

        when(pssMock.getProperty(any()))
                .thenThrow(new RuntimeException("Some exception"));

        psps.getProperty("test.prop.val");
    }

    @Test public void getExistedPropertyShouldRequestAwsOnlyOnce() {
        setUp("/common");
        withVal("/common/test/prop/val", "Ok");

        psps.getProperty("test.prop.val");
        psps.getProperty("test.prop.val");
        verify(pssMock, times(1)).getProperty("/common/test/prop/val");
    }

    @Test public void getUnExistedPropertyShouldRequestAwsOnlyOnce() {
        setUp("/common");
        withVal("/common/test/prop/val", "Ok");

        psps.getProperty("test.prop.val.wrong");
        psps.getProperty("test.prop.val.wrong");
        verify(pssMock, times(1)).getProperty("/common/test/prop/val/wrong");
    }

    @Test public void getExistedPropertyShouldReturnValueFromTheSecondFolderOf3() {
        setUp("/app,/common,/fallback");
        withVal("/app/test/prop/another", "No");
        withVal("/common/test/prop/val", "Ok");

        assertThat( psps.getProperty("test.prop.val"), is("Ok") );

        verify(pssMock, times(1)).getProperty(  "/common/test/prop/val");
        verify(pssMock, times(1)).getProperty(     "/app/test/prop/val");
        verify(pssMock, times(0)).getProperty("/fallback/test/prop/val");
    }


    @Test public void getUnExistedPropertyScanAllFolders() {
        setUp("/app,/common,/fallback");
        withVal("/app/test/prop/another", "a-value");

        assertThat( psps.getProperty("test.prop.val"), is(nullValue()) );

        verify(pssMock, times(1)).getProperty(  "/common/test/prop/val");
        verify(pssMock, times(1)).getProperty(     "/app/test/prop/val");
        verify(pssMock, times(1)).getProperty("/fallback/test/prop/val");
    }

}
