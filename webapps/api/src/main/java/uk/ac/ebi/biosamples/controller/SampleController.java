package uk.ac.ebi.biosamples.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.service.InverseRelationshipService;

@Controller
public class SampleController {

	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	
	@Autowired
	public InverseRelationshipService inverseRelationshipService;
	
	@RequestMapping(value = "samples/{accession}", produces = MediaType.TEXT_HTML_VALUE, method = RequestMethod.GET)
    public String greeting(Model model, @PathVariable String accession, HttpServletRequest request) {
        MongoSample sample = mongoSampleRepository.findOneByAccession(accession);

		inverseRelationshipService.addInverseRelationships(sample);
        
		model.addAttribute("sample", sample);		
        return "sample";
    }
}