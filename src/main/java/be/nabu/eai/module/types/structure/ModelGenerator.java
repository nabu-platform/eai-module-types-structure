package be.nabu.eai.module.types.structure;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface ModelGenerator {
	@WebResult(name = "models")
	public List<StructureModel> getModels();
	
	@WebResult(name = "fields")
	public default List<StructureModelField> getFields(@WebParam(name = "modelId") String modelId) {
		List<StructureModel> models = getModels();
		if (models != null) {
			for (StructureModel model : models) {
				if (modelId.equals(model.getId())) {
					return model.getFields();
				}
			}
		}
		return null;
	}
}
