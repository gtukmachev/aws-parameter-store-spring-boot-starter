package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStorePropertySourceTest
{

    private AwsParameterStorePropertySource psps;

    @Before
    public void setUp() {
        psps = new AwsParameterStorePropertySource("a name", buildParams(
                "prop.val.x", "/common/prop/val/x", "valid value x",
                "prop.val.y", "/myapp/prop/val/y", "valid value y"
        ));
    }

    @Test
    public void sourceShouldReturnValidValue() {
        assertThat( psps.getProperty("prop.val.x"), is("valid value x"));
        assertThat( psps.getProperty("prop.val.y"), is("valid value y"));
    }

    @Test
    public void sourceShouldReturnNullOnMissedKey() {
        assertThat( psps.getProperty("prop.val.no"), is(nullValue()));
    }

    private Map<String, Parameter> buildParams(String... p) {
        Map<String, Parameter> params = new HashMap<>();

        for (int i = 0; i < p.length; i += 3 )
            params.put(p[i], new Parameter().withName(p[i+1]).withValue(p[i+2]));

        return params;
    }

}
