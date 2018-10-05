package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.springframework.core.env.PropertySource;

import java.util.Map;

public class AwsParameterStorePropertySource extends PropertySource<AwsParameterStoreSource> {

    private final static SystemOutLogger logger = new SystemOutLogger();

    private final Map<String, Parameter> parameters;

    private final Parameter MISSED_VALUE = new Parameter().withValue(null);

    private final String[] rootSsnFolders;


    public AwsParameterStorePropertySource(String name,
                                           Map<String, Parameter> parameters,
                                           AwsParameterStoreSource source,
                                           String[] rootSsnFolders) {
        super(name, source);
        this.parameters = parameters;
        this.rootSsnFolders = rootSsnFolders;

    }

    @Override
    public Object getProperty(String name) {
        return parameters.getOrDefault(name, MISSED_VALUE).getValue();

/*
        // it's commented, becouse of the reading properties one by one takes too long time itself. now we are reading all available props at start time
        Parameter param = parameters.computeIfAbsent(name, name_ -> {
            String key = "/" + name_.replace(".", "/");

            for (String folder : rootSsnFolders) {
                String ssnKey = folder + key;
                Parameter possibleValue = source.getProperty(ssnKey);
                if (possibleValue != null) {
                    logger.info("AWS Parameter Store loaded: '{\"springProperty\": \"" + name
                            + "\", \"name\" = \"" + ssnKey
                            + "\", \"value\" = \"" + possibleValue + "\"}"
                    );
                    return possibleValue;
                }
            }

            return MISSED_VALUE;
        });

        return param.getValue();
*/
    }
}
