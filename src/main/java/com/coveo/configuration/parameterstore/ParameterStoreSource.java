package com.coveo.configuration.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundRuntimeException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreRuntimeException;

public class ParameterStoreSource
{
    private AWSSimpleSystemsManagement ssmClient;
    private boolean ignoreMissed;

    public ParameterStoreSource(AWSSimpleSystemsManagement ssmClient, boolean ignoreMissed) {
        this.ssmClient = ssmClient;
        this.ignoreMissed = ignoreMissed;
    }

    public Object getProperty(String propertyName) {
        try {
            return ssmClient.getParameter(new GetParameterRequest().withName(propertyName).withWithDecryption(true))
                            .getParameter()
                            .getValue();
        } catch (ParameterNotFoundException e) {
            if (!ignoreMissed) {
                throw new ParameterStoreParameterNotFoundRuntimeException(propertyName, e);
            }
        } catch (Exception e) {
            throw new ParameterStoreRuntimeException(propertyName, e);
        }
        return null;
    }
}
