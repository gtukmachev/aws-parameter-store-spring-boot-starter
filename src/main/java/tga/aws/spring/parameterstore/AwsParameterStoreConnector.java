package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 *  The class will add a new PropertySource to spring boot PropertySources chain (at the beginning od the chain).
 *  <p>
 *      As far, this class defined in META-INF/spring.factories - Spring Boot will create an instance of this class
 *      and run them {@link #postProcessEnvironment(ConfigurableEnvironment, SpringApplication)} method automatically.
 *  </p>
 *  <br/>
 *  <h2>System properties</h2>
 *  <p>
 *      You can manage of the library behaviour using 2 system properties: <strong>psSpringProfiles</strong> and <strong>psRoots</strong>
 *      <br/> Please, make sure, you setted up these properties before your application will be started, for instance:
 *      <pre>
 *      {@code
 *         // the best way to setup the psSpringProfiles is:
 *         @SpringBootApplication
 *         public class App {
 *             public static void main(String[] args) {
 *                 System.setProperty("psSpringProfiles","ANY"); // activated for any profile
 *                 System.setProperty("psRoots","/myapp,/common"); // 2 root folders will be used for reading properties (see bellow in the documentation)
 *
 *                 SpringApplication.run(App.class, args);
 *            }
 *         }
 *      }
 *      </pre>
 *  </p>
 *  <p>
 *      <strong>psSpringProfiles</strong> system property
 *      <br/>
 *      List of spring profiles (comma separated). If one of these profiles is active - the AWS Property Source Connector will be activated
 *      <br/>
 *      Use <strong>ANY</strong> profile name to activate it for all profiles</p>
 *  </p>
 *  <br/>
 *  <p>
 *      <strong>psRoots</strong> system property
 *        A list of comma separated root folders inside AWS Parameter Store
 *        <br/>Example: "/app,/common"
 *        <br/>In this case, the connector will search for your properties inside /app and then (if no one found) inside /common folders of th AWS Parameter Store
 *        <br/>The symbol '/' in front of a folder name is important - see documentation of AWS Property Source service
 *        <br/>Your spring-property names will be converted to AWS Property Source keys via the following convention:
 *        <br/>ROOT_FOLDER/PROPERTY_NAME - all '.' (dots) will be replaced to '/'
 *        <br/>So, for psSpringProfiles.roots="/app,/common", and you'll request 'server.port' property, the following properties will be read from AWS:
 *        <ul>
 *          <li>/app/server/port</li>
 *          <li>/common/server/port <i>(if /app/server/port is undefined)</i></li>
 *        </ul>
 *  </p>
 *
 * @see org.springframework.boot.env.EnvironmentPostProcessor
 *
 */
public class AwsParameterStoreConnector implements EnvironmentPostProcessor {

    static private final SystemOutLogger logger = new SystemOutLogger();

    /*
      <p>List of spring profiles (comma separated). If one of these profiles is active - the AWS Property Source Connector will be activated</p>
      <p>use ANY profile name to activate it for all profiles (or !xxx)</p>
     */
    static public final String pName_AcceptedSpringProfiles = "psSpringProfiles";

    /*
      <p>A list of comma separated root folders inside AWS Parameter Store</p>
      <p>Example: "/app,/common"</p>
      <p>In this case, the connector will search for your properties inside /app and then (if no one found) inside /common folders of th AWS Parameter Store</p>
      <p>The symbol '/' in front of a folder name is important - see documentation of AWS Property Source service</p>
      <p>Your spring-property names will be converted to AWS Property Source keys via the following convention:</p>
      <p>ROOT_FOLDER/PROPERTY_NAME - all '.' (dots) will be replaced to '/' </p>
      <p>So, for psSpringProfiles.roots="/app,/common", and you'll request 'server.port' property, the following properties will be read from AWS:<p/>
      <ul>
          <li>/app/server/port</li>
          <li>/common/server/port <i>(if /app/server/port is undefined)</i></li>
      </ul>
     */
    static public final String pName_Roots                  = "psRoots";

    static boolean initialized;

    private AWSParameterStoreClientBuilder awsParameterStoreClientBuilder = new AWSParameterStoreClientBuilder();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        logger.info("AWS Parameter Store integration: initialization started...");

        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {

            String[] roots = environment.getProperty(pName_Roots, String.class, "")
                    .split(",");
            if (roots.length == 0) roots = new String[]{""};

            AWSSimpleSystemsManagement client = buildAwsClient(roots, environment);

            if (client != null) {
                Map<String, Parameter> params = readAllProps(client, roots);
                if ( !params.isEmpty() ) {
                    environment.getPropertySources()
                            .addFirst( new AwsParameterStorePropertySource( "AwsParameterStorePropertySource", params ) );
                    logger.info("AWS Parameter Store integration: activated ("+ params.size() +" parameters loaded)");
                } else {
                    logger.warn("AWS Parameter Store integration: was not activated (no parameters found)");
                }
            } else {
                logger.warn("AWS Parameter Store integration: was not activated due a connection issue");
            }

            initialized = true;
        } else {
            logger.warn("AWS Parameter Store integration: was not activated");
        }
    }

    private Map<String, Parameter> readAllProps(AWSSimpleSystemsManagement client, String[] roots) {

        Map<String, Parameter> props = new HashMap<>();

        for (String root : roots) {
            String nextToken = null;

            do {
                GetParametersByPathResult result = client.getParametersByPath( new GetParametersByPathRequest()
                        .withPath(root)
                        .withWithDecryption(true)
                        .withRecursive(true)
                        .withNextToken(nextToken)
                );
                nextToken = result.getNextToken();

                if (result.getParameters() != null){
                    for ( Parameter p : result.getParameters() ) {
                        props.computeIfAbsent(p.getName().substring(root.length()+1).replace("/", "."),
                                key -> {
                                    logger.info("AWS Parameter Store loaded: {\"springProperty\": \"" + key
                                            + "\", \"name\" = \"" + p.getName()
                                            + "\", \"value\" = \"" + getSecureValue( p ) + "\"}"
                                    );
                                    return p;
                                }
                        );
                    }
                }

            } while (nextToken != null);

        }

        return props;
    }

    private String getSecureValue(Parameter p) {
        String t = p.getType();
        if (t == null) return "???";
        return (t.startsWith("Secure") || t.contains("pass") || t.contains("priva")) ? "***" : p.getValue();
    }

    private AWSSimpleSystemsManagement buildAwsClient(String[] roots, ConfigurableEnvironment environment) {
        AWSSimpleSystemsManagement client = getAwsParameterStoreClientBuilder().getClient();

        try {
            // AWS Connection checking: trying to read properties from the specified roots
            client.getParametersByPath( new GetParametersByPathRequest()
                    .withMaxResults(1)
                    .withPath(roots[0])
                    .withWithDecryption(true)
                    .withRecursive(true)
            );

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
