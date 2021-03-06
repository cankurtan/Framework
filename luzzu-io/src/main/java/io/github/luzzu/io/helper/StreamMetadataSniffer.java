package io.github.luzzu.io.helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Quad;

import io.github.luzzu.qualitymetrics.commons.cache.TemporaryGraphMetadataCacheObject;
import io.github.luzzu.semantics.commons.ResourceCommons;
import io.github.luzzu.semantics.vocabularies.DAQ;

/**
 * @author Jeremy Debattista
 *
 * This class acts as a helper class to the StreamProcessor
 * to extract the available quality metadata from the assessed 
 * dataset.
 * The output of this builder will be cached for future
 * use.
 */
public class StreamMetadataSniffer {
	
	private TemporaryGraphMetadataCacheObject _temp = null;
	private ConcurrentMap<String, Model> temporaryResources = new ConcurrentHashMap<String, Model>();
	
	/**
	 * Takes quad and tries to build the quality metadata 
	 * with the quad information available
	 * 
	 * The approach taken to build the temporary graph object
	 * is a poor man's approach, keeping in mind the following:
	 * 1) If the passed quad contains the triple "?x a DAQ.QualityGraph" then
	 * be can create a new TemporaryGraphMetadataCacheObject with the subject
	 * resource as the Graph URI
	 * 2) Since a quad might contain the GRAPH URI in its fourth (context) element, 
	 * then:
	 *	a) first check if the TemporaryGraphMetadataCacheObject is created and
	 *	if so, check if the context element (in the quad) is the same as defined 
	 *	as the Graph URI of the java object. If these are the same, then it is 
	 *  easy, as we just add the triples to the cached metadata.
	 *  b) if the cached object is not created yet, then we create a temporary 
	 *  jena model containing all resources of the same graph (these are known when
	 *  quads are streamed) and then stored in a hashmap. This hashmap will be used
	 *  for building the final cached object, pushing the corresponding model from the
	 *  datastructure to the cached object.
	 * 
	 * @param quad
	 */
	public int counter = 0;
	public void sniff(Quad quad){
		counter++;
		if (quad.getGraph() != null){
			if (this._temp != null && this._temp.getGraphURI() != null){
				if (quad.getGraph().getURI().equals(this._temp.getGraphURI().getURI())){
					this._temp.addTriplesToMetadata(quad);
				}
			} else {
				if (this.temporaryResources.containsKey(quad.getGraph().toString())){
					Model m = this.temporaryResources.get(quad.getGraph().toString());
					m.add(ResourceCommons.asRDFNode(quad.getSubject()).asResource(), 
							m.createProperty(quad.getPredicate().getURI()), 
							ResourceCommons.asRDFNode(quad.getObject()));
					this.temporaryResources.put(quad.getGraph().toString(), m);
				} else {
					Model m = ModelFactory.createDefaultModel();
					m.add(ResourceCommons.asRDFNode(quad.getSubject()).asResource(), 
							m.createProperty(quad.getPredicate().getURI()), 
							ResourceCommons.asRDFNode(quad.getObject()));
					this.temporaryResources.put(quad.getGraph().toString(), m);
				}
			}
		}
		if (quad.getObject().isURI()){
			if (quad.getObject().getURI().equals(DAQ.QualityGraph.getURI())){
				this._temp = new TemporaryGraphMetadataCacheObject(ResourceCommons.asRDFNode(quad.getObject()).asResource());
			}
		}
	}
	
	/**
	 * @return Returns the Object to be Cached
	 */
	public TemporaryGraphMetadataCacheObject getCachingObject(){
		if (_temp == null) return null;
		if (this.temporaryResources.containsKey(_temp.getGraphURI().toString())){
			_temp.addModelToMetadata(this.temporaryResources.get(_temp.getGraphURI().toString()));
		}
		return _temp;
	}
	
}
