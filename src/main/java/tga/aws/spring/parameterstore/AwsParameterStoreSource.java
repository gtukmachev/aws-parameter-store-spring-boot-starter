package tga.aws.spring.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import tga.aws.spring.parameterstore.exception.AwsParameterStoreConnectorException;

public class AwsParameterStoreSource {
    private AWSSimpleSystemsManagement ssmClient;

    public AwsParameterStoreSource(AWSSimpleSystemsManagement ssmClient) {
        this.ssmClient = ssmClient;
    }

    public Object getProperty(String propertyName) {
        try {
            return ssmClient.getParameter(new GetParameterRequest().withName(propertyName).withWithDecryption(true))
                            .getParameter()
                            .getValue();
        } catch (ParameterNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new AwsParameterStoreConnectorException(propertyName, e);
        }
    }
}
