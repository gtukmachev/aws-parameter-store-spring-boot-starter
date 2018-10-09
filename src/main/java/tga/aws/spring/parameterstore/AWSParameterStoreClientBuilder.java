package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

/**
 * It just a wrapper for instatiating an AWS client.
 * This wrapper needed to allow create an AWS Mock in tests using Mockito framework.
 * It's impossible to make the mock without this wrapper, becouse
 * the default AWS client implementation uses static methods for a new client creation.
 *
 */
public class AWSParameterStoreClientBuilder {

    static private final SystemOutLogger logger = new SystemOutLogger();

    public AWSSimpleSystemsManagement getClient() {

        try {
            return AWSSimpleSystemsManagementClientBuilder.defaultClient();
        } catch (Throwable ex) {
            logger.warn("Cant build an AWS client: " + ex.getClass().getSimpleName() + "\n" + ex.getMessage());
        }

        return null;
    }

}
