package tga.aws.spring.parameterstore.exception;

public class AwsParameterStoreConnectorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AwsParameterStoreConnectorException(String propertyName, Exception e) {
        super(String.format("Accessing AWS Parameter Store for parameter '%s' failed.", propertyName), e);
    }
}
