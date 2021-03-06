package com.king.yyl.service.utils.swagger.parser.processors;

import com.king.yyl.domain.apis.swagger.ArrayModel;
import com.king.yyl.domain.apis.swagger.ComposedModel;
import com.king.yyl.domain.apis.swagger.Model;
import com.king.yyl.domain.apis.swagger.ModelImpl;
import com.king.yyl.domain.apis.swagger.RefModel;
import com.king.yyl.domain.apis.swagger.Swagger;
import com.king.yyl.domain.apis.swagger.properties.Property;
import com.king.yyl.service.utils.swagger.parser.ResolverCache;

import java.util.List;
import java.util.Map;

import static com.king.yyl.service.utils.swagger.parser.util.RefUtils.isAnExternalRefFormat;


public class ModelProcessor {
    private final PropertyProcessor propertyProcessor;
    private final ExternalRefProcessor externalRefProcessor;

    public ModelProcessor(ResolverCache cache, Swagger swagger) {
        this.propertyProcessor = new PropertyProcessor(cache, swagger);
        this.externalRefProcessor = new ExternalRefProcessor(cache, swagger);
    }

    public void processModel(Model model) {
        if (model == null) {
            return;
        }

        if (model instanceof RefModel) {
            processRefModel((RefModel) model);
        } else if (model instanceof ArrayModel) {
            processArrayModel((ArrayModel) model);
        } else if (model instanceof ComposedModel) {
            processComposedModel((ComposedModel) model);
        } else if (model instanceof ModelImpl) {
            processModelImpl((ModelImpl) model);
        }
    }

    private void processModelImpl(ModelImpl modelImpl) {

        final Map<String, Property> properties = modelImpl.getProperties();

        if (properties == null) {
            return;
        }

        for (Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
            final Property property = propertyEntry.getValue();
            propertyProcessor.processProperty(property);
        }

    }

    private void processComposedModel(ComposedModel composedModel) {

        processModel(composedModel.getParent());
        processModel(composedModel.getChild());

        final List<RefModel> interfaces = composedModel.getInterfaces();
        if (interfaces != null) {
            for (RefModel model : interfaces) {
                processRefModel(model);
            }
        }

    }

    private void processArrayModel(ArrayModel arrayModel) {

        final Property items = arrayModel.getItems();

        // ArrayModel has a properties map, but my reading of the swagger spec makes me think it should be ignored

        if (items != null) {
            propertyProcessor.processProperty(items);
        }
    }


    private void processRefModel(RefModel refModel) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/definitions
        3) update the RefModel to point to its location in #/definitions
     */

        if (isAnExternalRefFormat(refModel.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat());

            if (newRef != null) {
                refModel.set$ref(newRef);
            }
        }
    }


}
