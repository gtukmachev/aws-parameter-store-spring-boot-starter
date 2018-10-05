package tga.aws.spring.parameterstore;

import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class AwsParameterStorePropertySource extends PropertySource<AwsParameterStoreSource> {

    private final static SystemOutLogger logger = new SystemOutLogger();

    private final Map<String, Object> cache = new HashMap<>();
    private final String MISSED_VALUE = "!@<NULL>";

    private final String[] rootSsnFolders;


    public AwsParameterStorePropertySource(String name, AwsParameterStoreSource source, String[] rootSsnFolders) {
        super(name, source);
        this.rootSsnFolders = rootSsnFolders;
    }

    @Override
    public Object getProperty(String name) {

        Object value = cache.computeIfAbsent(name, name_ -> {
            String key = "/" + name_.replace(".", "/");

            for (String folder : rootSsnFolders) {
                String ssnKey = folder + key;
                Object possibleValue = source.getProperty(ssnKey);
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

        return value == MISSED_VALUE ? null : value;
    }
}
