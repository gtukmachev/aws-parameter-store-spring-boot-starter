package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.springframework.core.env.PropertySource;

import java.util.Map;

public class AwsParameterStorePropertySource extends PropertySource<AwsParameterStorePropertySource.EmptySource> {

    private final Map<String, Parameter> parameters;

    private final Parameter MISSED_VALUE = new Parameter().withValue(null);

    public AwsParameterStorePropertySource(String name, Map<String, Parameter> parameters) {
        super(name, new EmptySource());
        this.parameters = parameters;

    }

    @Override
    public Object getProperty(String name) {
        return parameters.getOrDefault(name, MISSED_VALUE).getValue();
    }

    public static class EmptySource {
        public Object getProperty(String propertyName) {
            return null;
        }
    }
}
