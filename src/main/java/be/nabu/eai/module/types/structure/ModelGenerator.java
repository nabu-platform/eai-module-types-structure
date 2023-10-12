package be.nabu.eai.module.types.structure;

import java.util.List;

import javax.jws.WebResult;

public interface ModelGenerator {
	@WebResult(name = "models")
	public List<StructureModel> getModels();
}
