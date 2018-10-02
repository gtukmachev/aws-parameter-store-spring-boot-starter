package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class AwsParameterStoreConnector implements EnvironmentPostProcessor {

    static private final Logger logger = LoggerFactory.getLogger(AwsParameterStoreConnector.class);

    /**
     * <p>List of spring profiles (comma separated). If one of these profiles is active - the AWS Property Source Connector will be activated</p>
     * <p>use ANY profile name to activate it for all profiles (or !xxx)</p>
     */
    static public final String pName_AcceptedSpringProfiles = "psSpringProfiles";

    /**
     * <p>A list of comma separated root folders inside AWS Parameter Store</p>
     * <p>Example: "/app,/common"</p>
     * <p>In this case, the connector will search for your properties inside /app and then (if no one found) inside /common folders of th AWS Parameter Store</p>
     * <p>The symbol '/' in front of a folder name is important - see documentation of AWS Property Source service</p>
     * <p>Your spring-property names will be converted to AWS Property Source keys via the following convention:</p>
     * <p>ROOT_FOLDER/PROPERTY_NAME - all '.' (dots) will be replaced to '/' </p>
     * <p>So, for psSpringProfiles.roots="/app,/common", and you'll request 'server.port' property, the following properties will be read from AWS:<p/>
     * <ul>
     *     <li>/app/server/port</li>
     *     <li>/common/server/port <i>(if /app/server/port is undefined)</i></li>
     * </ul>
     */
    static public final String pName_Roots                  = "psRoots";

    static boolean initialized;

    private AWSParameterStoreClientBuilder awsParameterStoreClientBuilder = new AWSParameterStoreClientBuilder();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            String[] roots = environment.getProperty(pName_Roots, String.class, "")
                    .split(",");

            if (roots.length == 0) roots = new String[]{""};

            AWSSimpleSystemsManagement client = buildAwsClient(roots, environment);

            if (client != null) {
                environment.getPropertySources()
                        .addFirst(
                                new AwsParameterStorePropertySource(
                                        "AwsParameterStorePropertySource",
                                        new AwsParameterStoreSource( client ),
                                        roots
                                )
                        );
            }

            initialized = true;
        }
    }

    private AWSSimpleSystemsManagement buildAwsClient(String[] roots, ConfigurableEnvironment environment) {
        AWSSimpleSystemsManagement client = getAwsParameterStoreClientBuilder().getClient();

        try {
            // AWS Connection checking: trying to read properties from the specified roots
            for (String root : roots) {
                GetParametersByPathResult result;
                do {
                    result = client.getParametersByPath( new GetParametersByPathRequest()
                            .withMaxResults(1)
                            .withPath(root)
                            .withWithDecryption(true)
                            .withRecursive(true)
                    );
                } while (result.getParameters().isEmpty() && result.getNextToken() != null);
            }

        } catch (Exception e) {
            if (environment.acceptsProfiles("Prod")) {
                logger.error("AWS Parameter Store is unreachable vs default credentials!");
                throw e;
            } else {
                logger.info("AWS Parameter Store is unreachable vs default credentials. Local instance settings only will be used for the application configuration.");
                return null;
            }
        }

        return client;
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment){
        String userDefinedEnabledProfiles = environment.getProperty(pName_AcceptedSpringProfiles, String.class, "");

        if (userDefinedEnabledProfiles.length() == 0) return false;
        if ("ANY".equals(userDefinedEnabledProfiles)) return true;

        String[] profiles = userDefinedEnabledProfiles.split(",");
        return environment.acceptsProfiles(profiles);
    }

    public AWSParameterStoreClientBuilder getAwsParameterStoreClientBuilder() {
        return awsParameterStoreClientBuilder;
    }

    public void setAwsParameterStoreClientBuilder(AWSParameterStoreClientBuilder awsParameterStoreClientBuilder) {
        this.awsParameterStoreClientBuilder = awsParameterStoreClientBuilder;
    }
}
