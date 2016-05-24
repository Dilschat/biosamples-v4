package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class SampleDeserializer extends JsonDeserializer<Sample> {

	@Override
	public Sample deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        
        String accession = node.get("accession").asText();
        String name = node.get("name").asText(); 

    	Map<String, Set<String>> keyValues = new HashMap<>();
    	Map<String, Map<String, URI>> ontologyTerms = new HashMap<>();
    	Map<String, Map<String, String>> units = new HashMap<>();
    	
    	Iterator<JsonNode> attributes = node.get("attributes").elements();
        while (attributes.hasNext()) {
        	JsonNode attribute = attributes.next();
        	
        	String key = attribute.get("key").asText();
        	String value = attribute.get("value").asText();
			URI ontologyTerm = null;
        	try {
        		if (attribute.has("ontologyTerm") 
        				&& attribute.get("ontologyTerm").asText().length() > 0 ){
        			ontologyTerm = new URI(attribute.get("ontologyTerm").asText());
        		}
			} catch (URISyntaxException e) {
				throw new JsonParseException(jp, "invalid URI", e);
			}
        	String unit = null;
        	if (attribute.has("unit") 
    				&& attribute.get("unit").asText().length() > 0 ){
        		unit = attribute.get("unit").asText();
    		}

        	if (!keyValues.containsKey(key)) {
        		keyValues.put(key, new HashSet<>());
        	}
			keyValues.get(key).add(value);
			
			if (ontologyTerm != null) {
	        	if (!ontologyTerms.containsKey(key)) {
	        		ontologyTerms.put(key, new HashMap<>());
	        	}
	        	ontologyTerms.get(key).put(value, ontologyTerm);
			}
			
			if (unit != null) {
	        	if (!units.containsKey(key)) {
	        		units.put(key, new HashMap<>());
	        	}
	        	units.get(key).put(value, unit);
			}
        }
        
        return SimpleSample.createFrom(name, accession, keyValues, ontologyTerms, units);
	}

}
