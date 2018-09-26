package com.coveo.configuration.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    static final String PARAMETER_STORE_ACCEPTED_PROFILE = "awsParameterStorePropertySourceEnabled";

    static final String PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.enabledProfiles";
    static final String PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.enabled";
    static final String PARAMETER_STORE_IGNORE_MISSED_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.ignoreMissed";

    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "AWSParameterStorePropertySource";

    static boolean initialized;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            environment.getPropertySources()
                    .addFirst(new ParameterStorePropertySource(
                            PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                            new ParameterStoreSource(AWSSimpleSystemsManagementClientBuilder.defaultClient(),
                                    environment.getProperty(PARAMETER_STORE_IGNORE_MISSED_CONFIGURATION_PROPERTY,
                                            Boolean.class,
                                            Boolean.TRUE)),
                            new String[]{"common", "app"}
                            ));
            initialized = true;
        }
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY,
                                                                      String[].class);
        return environment.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY, Boolean.class, Boolean.FALSE)
                || environment.acceptsProfiles(PARAMETER_STORE_ACCEPTED_PROFILE)
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(userDefinedEnabledProfiles));
    }
}
