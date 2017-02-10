package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class TestRunner implements ApplicationRunner {

	
	@Autowired
	private IntegrationProperties integrationProperties;
	
	@Autowired
	private RestOperations restTemplate;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
				
		Sample sample = getSimpleSample();
		
		//get and check that nothing exists already
		doGetAndFail(sample);
		
		//post a new submission
		//get to check it worked
		//put an update
		//get to check it worked
		//delete it
		//get to check it worked
		
	}
	
	public void doGetAndFail(Sample sample) {
		try {
			doGet(sample);
		} catch (HttpStatusCodeException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				//we expect to get a 404 error
				return;
			} else {
				//we got something else
				throw e;
			}
		}		
		throw new RuntimeException("Expected a 404 response");
	}
	
	public ResponseEntity<Sample> doGet(Sample sample) throws RestClientException {		
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionURI())
				.path("samples/")
				.path(sample.getAccession())
				.build().toUri();
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();		
		ResponseEntity<Sample> response = restTemplate.exchange(request, Sample.class);
		return response;
	}
	

	private Sample getSimpleSample() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));
		
		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("derived from", "TEST2", "TEST1"));

		return Sample.build(name, accession, release, update, attributes, relationships);
	}


}
