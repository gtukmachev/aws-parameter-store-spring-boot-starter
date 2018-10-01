package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

/**
 * Created by grigory@clearscale.net on 10/1/2018.
 */
public class AWSParameterStoreClientBuilder {

    public AWSSimpleSystemsManagement getClient() {
        return AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

}
