package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class IntegrationProperties {

	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosampleSubmissionUri;

	public URI getBiosampleSubmissionUri() {
		return biosampleSubmissionUri;
	}
	
	@Value("${biosamples.submissionuri.sampletab:http://localhost:8082}")
	private URI biosampleSubmissionUriSampletab;

	public URI getBiosampleSubmissionUriSampleTab() {
		return biosampleSubmissionUriSampletab;
	}
}
