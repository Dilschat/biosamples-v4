package uk.ac.ebi.biosamples.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.validator.SampleTabValidator;
import uk.ac.ebi.biosamples.model.v1.Outcome;
import uk.ac.ebi.biosamples.model.v1.SampleTabRequest;
import uk.ac.ebi.biosamples.service.ApiKeyService;
import uk.ac.ebi.biosamples.service.SampleTabService;

@RestController
public class LegacySampleTabController {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private SampleTabService sampleTabService;
	
	@Autowired
	private ApiKeyService apiKeyService;
	
    @PostMapping(value = "/api/v1/json/ac")
    public @ResponseBody Outcome doAccession(@RequestBody SampleTabRequest sampletab, String apikey) {
    	return process(sampletab, apikey, false);
    }
	
    @PostMapping(value = "/api/v1/json/sb")
    public @ResponseBody Outcome doSubmission(@RequestBody SampleTabRequest sampletab, String apikey) {
    	return process(sampletab, apikey, true);
    }
    	
	private Outcome process(SampleTabRequest sampletab, String apikey, boolean submission) {
        String keyOwner = null;
        if (apikey != null) {
	        try { 
	            keyOwner = apiKeyService.getApiKeyOwner(apikey);
	        } catch (IllegalArgumentException e) {
	            //invalid API key, return errors
                Outcome o = new Outcome();
                List<Map<String,String>> errorList = new ArrayList<Map<String,String>>();
                Map<String, String> errorMap = new HashMap<String, String>();
                errorMap.put("message", "Invalid API key ("+apikey+")");
                errorMap.put("comment", "Contact biosamples@ebi.ac.uk for assistance");
                errorList.add(errorMap);
                o.setErrors(errorList);
                return o;
	        }
        }
    	
        if (keyOwner == null) {
        	throw new RuntimeException("No API key owner recognized");
        }
    	
        //setup parser to listen for errors
        SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>();
        
        final List<ErrorItem> errorItems;
        errorItems = new ArrayList<ErrorItem>();
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                errorItems.add(item);
            }
        });
         
        
        Outcome outcome = null;
        try {
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
            
            //parse the input into sampletab
            SampleData sampledata = parser.parse(is);
            
            //some corrections for hipsci
            if (sampledata.msi.submissionIdentifier.equals("GCG-HipSci")) {
                sampledata.msi.submissionIdentifier = "GSB-3";
            }

            //assign accessions to sampletab object
            
            
            //TODO this needs to be smarter 
            // - check if existing accession exists first
            // - check if there are samples with same name & owner

            //save the old release date
            Date oldReleaseDate = sampledata.msi.submissionReleaseDate;
            
            if (!submission) {
	            //set release date to 100 years in future
	            Calendar c = Calendar.getInstance();
	            c.setTime(new Date()); // Now use today date.
	            c.add(Calendar.DATE, 100*365); // Adds 100 years
	            sampledata.msi.submissionReleaseDate = c.getTime();
            }
            
            //persist it then put the new accessions back into the sampletab
            sampledata = sampleTabService.saveSampleTab(sampledata);
            
            //reset the date to the old one
            if (!submission) {
            	sampledata.msi.submissionReleaseDate = oldReleaseDate;
            }
            
            //return the accessioned file, and any generated errors            
            outcome = new Outcome(sampledata, errorItems);
            
        } catch (ParseException e) {
            //catch parsing errors for malformed submissions
            log.error(e.getMessage(), e);
            outcome = new Outcome(null, e.getErrorItems());
        } catch (UnsupportedEncodingException e) {
            //catch parsing errors for malformed submissions
        	//these errors should never happen
            log.error(e.getMessage(), e);
            Map<String, String> error;
            List<Map<String,String>> errors = new ArrayList<Map<String,String>>();
            error = new HashMap<String, String>();
            error.put("type", e.getClass().getName());
            error.put("message", e.getLocalizedMessage());
            errors.add(error);
            outcome = new Outcome(null, errors);
        } catch (Exception e) {
            //general catch all for other errors, e.g SQL
            log.error(e.getMessage(), e);
            List<Map<String,String>> errors = new ArrayList<Map<String,String>>();
            Map<String, String> error = new HashMap<String, String>();
            error.put("type", e.getClass().getName());
            error.put("message", e.getLocalizedMessage());
            errors.add(error);
            outcome = new Outcome(null, errors);
        } 
        return outcome;
    }
    
    
    
    
                
    @PostMapping(value = "/api/v1/json/va")
    public @ResponseBody Outcome doValidation(@RequestBody SampleTabRequest sampletab) {
        //setup parser to listen for errors
        SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>(new SampleTabValidator());
        
        final List<ErrorItem> errorItems;
        errorItems = new ArrayList<ErrorItem>();
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                errorItems.add(item);
            }
        });
         
        try {
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
            
            //parse the input into sampletab
            //will also validate
            SampleData sampledata = parser.parse(is);
            
            //return the accessioned file, and any generated errors            
            return new Outcome(sampledata, errorItems);
            
        } catch (ParseException e) {
            //catch parsing errors for malformed submissions
            log.error(e.getMessage());
            return new Outcome(null, e.getErrorItems());
        } catch (Exception e) {
            //general catch all for other errors, e.g SQL
            log.error(e.getMessage());
            return new Outcome();
        } 
    }

    
    /*
     * Echoing function. Used for triggering download of javascript
     * processed sampletab files. No way to download a javascript string
     * directly from memory, so it is bounced back off the server through
     * this method.
     */
    @PostMapping(value = "/api/echo")
    public void echo(String input, HttpServletResponse response) throws IOException {
        //set it to be marked as a download file
        //response.setContentType("application/octet-stream");
        response.setContentType("application/force-download; charset=UTF-8");
        //set the filename to download it as
        response.addHeader("Content-Disposition","attachment; filename=\"sampletab.txt\"");
        response.setHeader("Content-Transfer-Encoding", "binary");

        //writer to the output stream
        //let springs default error handling take over and redirect on error.
        Writer out = null; 
        try {
            out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
            out.write(input);
        } finally {
            if (out != null){
                try {
                    out.close();
                    response.flushBuffer();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
        
    }
}
