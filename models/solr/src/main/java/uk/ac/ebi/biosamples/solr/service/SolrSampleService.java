package uk.ac.ebi.biosamples.solr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.model.SampleFacetsBuilder;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class SolrSampleService {

	private final SolrSampleRepository solrSampleRepository;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public SolrSampleService(SolrSampleRepository solrSampleRepository) {
		this.solrSampleRepository = solrSampleRepository;
	}		

	public Page<SolrSample> fetchSolrSampleByText(String searchTerm, MultiValueMap<String,String> filters, Pageable pageable) {
		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		//build a query out of the users string and any facets
		Query query = new SimpleQuery(searchTerm);
		query.setPageRequest(pageable);
				
		if (filters != null) {
			query = addFilters(query, filters);
		}		

		//filter out non-public
		FilterQuery filterQuery = new SimpleFilterQuery();
		filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW"));
		query.addFilterQuery(filterQuery);
		
		// return the samples from solr that match the query
		return solrSampleRepository.findByQuery(query);
	}

	public List<SampleFacet> getFacets(String searchTerm, MultiValueMap<String,String> filters, Pageable facetPageable, Pageable facetValuePageable) {
		//default to search all
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			searchTerm = "*:*";
		}
		
		SampleFacetsBuilder builder = new SampleFacetsBuilder();

		//build a query out of the users string and any facets
		FacetQuery query = new SimpleFacetQuery();
		query.addCriteria(new Criteria().expression(searchTerm));
		query = addFilters(query, filters);
		
		//filter out non-public
		FilterQuery filterQuery = new SimpleFilterQuery();
		filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW"));
		query.addFilterQuery(filterQuery);
		
		Page<FacetFieldEntry> facetFields = solrSampleRepository.getFacetFields(query, facetPageable);

		//using the query, get a list of facets and overall counts
		List<String> facetFieldList = new ArrayList<>();
		for (FacetFieldEntry ffe : facetFields) {
			log.info("Putting "+ffe.getValue()+" with count "+ffe.getValueCount());
			facetFieldList.add(ffe.getValue());				
			builder.addFacet(SolrSampleService.fieldToAttributeType(ffe.getValue()), ffe.getValueCount());
		}
		
		//if there are no facets available (e.g. no samples)
		//then cleanly exit here
		if (facetFieldList.isEmpty()) {
			return builder.build();
		}

		FacetPage<?> facetPage = solrSampleRepository.getFacets(query, facetFieldList, facetValuePageable);
		for (Field field : facetPage.getFacetFields()) {

			//for each value, put the number of them into this facets map
			for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
				log.info("Adding "+field.getName()+" : "+ffe.getValue()+" with count "+ffe.getValueCount());					
				builder.addFacetValue(SolrSampleService.fieldToAttributeType(field.getName()), ffe.getValue(), ffe.getValueCount());
			}
		}
		
		return builder.build();
		
	}

	public Autocomplete getAutocomplete(String autocompletePrefix, MultiValueMap<String,String> filters, int maxSuggestions) {
		//default to search all
		String searchTerm = "*:*";
		//build a query out of the users string and any facets
		FacetQuery query = new SimpleFacetQuery();
		query.addCriteria(new Criteria().expression(searchTerm));
		query.setPageRequest(new PageRequest(0, 1));
				
		if (filters != null) {
			query = addFilters(query, filters);
		}		

		//filter out non-public
		FilterQuery filterQuery = new SimpleFilterQuery();
		filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW"));
		query.addFilterQuery(filterQuery);

		FacetOptions facetOptions = new FacetOptions();
		facetOptions.addFacetOnField("autocomplete_ss");
		facetOptions.setPageable(new PageRequest(0, maxSuggestions));
		facetOptions.setFacetPrefix(autocompletePrefix);
		query.setFacetOptions(facetOptions);
		
		FacetPage<?> facetPage = solrSampleRepository.findByFacetQuery(query);
		
		Page<FacetFieldEntry> facetFiledEntryPage = facetPage.getFacetResultPage("autocomplete_ss");
		
		List<String> autocompleted = new ArrayList<>();
		for (FacetFieldEntry facetFieldEntry : facetFiledEntryPage) {
			autocompleted.add(facetFieldEntry.getValue());
		}
		return new Autocomplete(autocompletePrefix, autocompleted);		
	}
	
	private <T extends Query> T addFilters(T query, MultiValueMap<String,String> filters) {
		//if no filters or filters are null, quick exit
		if (filters == null || filters.size() == 0) {
			return query;
		}		

		boolean filter = false;
		FilterQuery filterQuery = new SimpleFilterQuery();
		for (String facetType : filters.keySet()) {
			Criteria facetCriteria = null;
			
			String facetField = attributeTypeToField(facetType);
			for (String facatValue : filters.get(facetType)) {
				if (facatValue == null) {
					//no specific value, check if its not null
					facetCriteria = new Criteria(facetField).isNotNull();					
				} else if (facetCriteria == null) {
					facetCriteria = new Criteria(facetField).is(facatValue);
				} else {
					facetCriteria = facetCriteria.or(new Criteria(facetField).is(facatValue));
				}

				log.info("Filtering on "+facetField+" for value "+facatValue);
			}
			if (facetCriteria != null) {
				filterQuery.addCriteria(facetCriteria);
				filter = true;
			}
		}
		
		if (filter) {
			query.addFilterQuery(filterQuery);
		}
		return query;
	}
	
	
	public static String attributeTypeToField(String type) {
		type = type.replaceAll(" ", "_");
		type = type+"_av_ss";
		return type;
	}
	
	public static String fieldToAttributeType(String field) {
		//strip _ss
		if (field.endsWith("_ss")) {
			field = field.substring(0, field.length()-3);			
		}		
		//strip _av
		if (field.endsWith("_av")) {
			field = field.substring(0, field.length()-3);			
		}				
		//replace _ with space
		field = field.replace("_", " ");
		
		return field;
	}
}
