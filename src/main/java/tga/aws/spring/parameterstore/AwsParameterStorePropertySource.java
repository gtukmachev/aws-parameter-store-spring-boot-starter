package tga.aws.spring.parameterstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class AwsParameterStorePropertySource extends PropertySource<AwsParameterStoreSource> {

    private final static Logger logger = LoggerFactory.getLogger(AwsParameterStorePropertySource.class);

    private final Map<String, Object> cache = new HashMap<>();
    private final String MISSED_VALUE = "!<NULL>";

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
                    logger.info("AWS Parameter Store loaded: '{\"property\": \"{}\", \"key\" = \"{}\", \"value\" = \"{}\"}"
                            , name_
                            , ssnKey
                            , possibleValue
                    );
                    return possibleValue;
                }
            }

            return MISSED_VALUE;
        });

        return value == MISSED_VALUE ? null : value;
    }
}
