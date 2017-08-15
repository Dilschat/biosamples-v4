package uk.ac.ebi.biosamples.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
    
	private Logger log = LoggerFactory.getLogger(getClass());
    private static final String STMGETUSR = "SELECT APIKEY, USERNAME, PUBLICEMAIL, PUBLICURL, CONTACTNAME, CONTACTEMAIL FROM USERS WHERE APIKEY LIKE ?";

	@Autowired
	@Qualifier("accessionJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;
    
    public String getApiKeyOwner(String apikey) throws IllegalArgumentException {
        //generate keys with following python:
        //  "".join([random.choice("ABCDEFGHKLMNPRTUWXY0123456789") for x in xrange(16)])
        //NB: avoid similar looking letters/numbers
    	
    	Optional<AccessionUser> user = getUserForApikey(apikey);
    	if (!user.isPresent()) {
            //invalid API key, throw exception
            throw new IllegalArgumentException("Invalid API key ("+apikey+")");
    	} else {
    		return user.get().username;
    	}
    }
    
    public boolean canKeyOwnerEditSource(String keyOwner, String source) {
        if (keyOwner == null || keyOwner.trim().length() == 0) {
            throw new IllegalArgumentException("keyOwner must a sensible string");
        }
        if (source == null || source.trim().length() == 0) {
            throw new IllegalArgumentException("source must be a sensible string");
        }
        
        if ("BioSamples".toLowerCase().equals(keyOwner.toLowerCase())) {
            //BioSamples key can edit anything
            return true;
        } else if (source.toLowerCase().equals(keyOwner.toLowerCase())) {
            //source key can edit their own samples
            return true;
        } else {
            //deny everyone else
        	log.info("Keyowner "+keyOwner+" attempted to access "+source);
            return false;
        }
    }
    
	private Optional<AccessionUser> getUserForApikey(String apiKey) {
		List<AccessionUser> users = jdbcTemplate.query(STMGETUSR, new RowMapper<AccessionUser>(){
			@Override
			public AccessionUser mapRow(ResultSet rs, int rowNum) throws SQLException {
				//APIKEY, USERNAME, PUBLICEMAIL, PUBLICURL, CONTACTNAME, CONTACTEMAIL
				String apiKey = rs.getString("APIKEY");
				String username = rs.getString("USERNAME");
				Optional<String> publicEmail = Optional.ofNullable(rs.getString("PUBLICEMAIL"));
				Optional<URL> publicUrl;
				if (rs.getString("PUBLICURL") == null) {
					publicUrl = Optional.empty();
				} else {
					try {
						publicUrl = Optional.ofNullable(new URL(rs.getString("PUBLICURL")));
					} catch (MalformedURLException e) {
						log.error("Invalid public URL for "+username, e);
						publicUrl = Optional.empty();
					}
				}
				Optional<String> contactName = Optional.ofNullable(rs.getString("CONTACTNAME"));
				Optional<String> contactEmail = Optional.ofNullable(rs.getString("CONTACTEMAIL"));
				return new AccessionUser(apiKey, username, publicEmail, publicUrl, contactName, contactEmail);
			}}, apiKey);
		if (users.size() == 0) {
			//no user found
			return Optional.empty();
		} else if (users.size() > 1) {
			//multiple users found
			throw new IllegalStateException("Multiple users for API key "+apiKey);
		} else {
			return Optional.of(users.get(0));
		}
	}
	
	public class AccessionUser {
		public final String apiKey;
		public final String username;
		public final Optional<String> publicEmail;
		public final Optional<URL> publicUrl; 
		public final Optional<String> contactName;
		public final Optional<String> contactEmail;
		
		public AccessionUser(String apiKey, String username, Optional<String> publicEmail, Optional<URL> publicUrl,
				Optional<String> contactName, Optional<String> contactEmail) {
			super();
			this.apiKey = apiKey;
			this.username = username;
			this.publicEmail = publicEmail;
			this.publicUrl = publicUrl;
			this.contactName = contactName;
			this.contactEmail = contactEmail;
		}
	}
}
